/*
 * Copyright (C) 2025  Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ideal.linked.toposoid.common

import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObject
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeBaseEdge
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeBaseNode
import scala.util.{Failure, Success, Try}
import com.ideal.linked.common.DeploymentConverter.conf
import com.typesafe.scalalogging.LazyLogging
import com.ideal.linked.toposoid.protocol.model.base.MatchedKnowledgeNode
import com.ideal.linked.toposoid.protocol.model.base.MatchedFeatureInfo
import com.ideal.linked.toposoid.protocol.model.base.KnowledgeBaseSideInfo
import com.ideal.linked.toposoid.protocol.model.base.CoveredPropositionNode
import com.ideal.linked.toposoid.protocol.model.base.CoveredPropositionEdge
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeBaseSynonymNode
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeFeatureReference
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import play.api.libs.json.{Json, __}


case class DeductionQuery(query:String,relationMatchState:RelationMatchState, sourceAlias:String, destinationAlias:String,isSourceConfirmed:Boolean, isDestinationConfirmed:Boolean, featureSimilarityMap:Map[String, Float] = Map.empty[String, Float])

object DeductionUtils extends LazyLogging {

    def analyzeGraphKnowledge(getQeuries:(KnowledgeBaseEdge, aso:AnalyzedSentenceObject, TransversalState) => List[DeductionQuery], aso:AnalyzedSentenceObject, transversalState:TransversalState):List[CoveredPropositionEdge] = {    
        val edges:List[KnowledgeBaseEdge] = getUnsettledEdges(aso)
        val futures: List[Future[Option[CoveredPropositionEdge]]] = edges.foldLeft(List.empty[Future[Option[CoveredPropositionEdge]]]){
        (acc, edge) => {
            val deductionQueries = getQeuries(edge, aso, transversalState)       
            deductionQueries.size match {
            case 0 => acc :+ Future(Option(aso.deductionResult.coveredPropositionEdges.filter(x => x.sourceNode.terminalId.equals(edge.sourceId) && x.destinationNode.terminalId.equals(edge.destinationId)).head))
            case _ => acc :+ Future(analyzeEdge(0, deductionQueries, edge, aso, Neo4JUtilsImpl(), transversalState))
            }        
        }
        }    
        val combinedFuture: Future[List[Option[CoveredPropositionEdge]]] = Future.sequence(futures)
        val result = Await.result(combinedFuture, Duration.Inf)    
        result.flatten
    }

    private def getUnsettledEdges(aso:AnalyzedSentenceObject): List[KnowledgeBaseEdge] = {
        //TODO:ロジカルエッヂを省けてる？
        val pairSetList = aso.deductionResult.coveredPropositionEdges.foldLeft(List.empty[Set[String]]){
            (acc, x) => {
                if(x.sourceNode.isConfirmed && x.destinationNode.isConfirmed){
                    acc :+ Set(x.sourceNode.terminalId, x.destinationNode.terminalId)
                }else{
                    acc
                }        
            }
        }
        aso.edgeList.filterNot(x => {
            val targetLink = Set(x.sourceId, x.destinationId)
            pairSetList.contains(targetLink)
        })    
    }

    private def analyzeEdge(idx:Int, deductionQueries:List[DeductionQuery],edge:KnowledgeBaseEdge, aso:AnalyzedSentenceObject, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState):Option[CoveredPropositionEdge] = {
        val nodeMap = aso.nodeMap
        val sourceNode = nodeMap.get(edge.sourceId).get.asInstanceOf[KnowledgeBaseNode]
        val destinationNode = nodeMap.get(edge.destinationId).get.asInstanceOf[KnowledgeBaseNode]

        //引数のisSourceConfirmed, isDestinationConfirmedとdeductionQueries(idx)のisSourceConfirmed, isDestinationConfirmedが同じかをチェックする。
        //チェックNGの場合は、analyzeEdge(idx+1, deductionQueries, edge, nodeMap, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)    
        val coveredPropositionEdges = aso.deductionResult.coveredPropositionEdges.filter(x => {
            x.sourceNode.terminalId.equals(edge.sourceId) && x.destinationNode.terminalId.equals(edge.destinationId)
        })
        val (isSourceConfirmed, isDestinationConfirmed, coveredPropositionEdge) = coveredPropositionEdges.size match {
            case 0 => (false, false, None) //BaseMatch用
            case _ => (coveredPropositionEdges.head.sourceNode.isConfirmed, coveredPropositionEdges.head.destinationNode.isConfirmed, Option(coveredPropositionEdges.head))
        }
        //クエリを実行する必要のない場合は、早めに判断し次のクエリを実行を促す。
        if(!deductionQueries(idx).isSourceConfirmed == isSourceConfirmed || !deductionQueries(idx).isDestinationConfirmed == isDestinationConfirmed){
            if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
            else coveredPropositionEdge  
        }else{

            val sourceMorphemes = sourceNode.predicateArgumentStructure.morphemes
            val destinationMorphemes = destinationNode.predicateArgumentStructure.morphemes
            val isVerbOrNounOnSource = sourceNode.localContext.lang match {
            case "ja_JP" =>  sourceMorphemes.filter(x => x.split(",").toList.contains("動詞")).size > 0 || sourceMorphemes.filter(x => x.split(",").toList.contains("名詞")).size > 0
            case "en_US" => sourceMorphemes.filter(x => x.split(",").toList.contains("VERB")).size > 0  || sourceMorphemes.filter(x => x.split(",").toList.contains("NOUN")).size > 0
            }
            val isVerbOrNounOnDestination = destinationNode.localContext.lang match {
            case "ja_JP" =>  destinationMorphemes.filter(x => x.split(",").toList.contains("動詞")).size > 0 || destinationMorphemes.filter(x => x.split(",").toList.contains("名詞")).size > 0
            case "en_US" => destinationMorphemes.filter(x => x.split(",").toList.contains("VERB")).size > 0  || destinationMorphemes.filter(x => x.split(",").toList.contains("NOUN")).size > 0
            }

            deductionQueries(idx).relationMatchState match {
            case RelationMatchState.MATCHED_BOTH => {        
                analyze(idx, deductionQueries, edge, nodeMap, neo4JUtils, transversalState) match {
                case Some(x) => Option(x)
                case _ => {
                    if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                    else coveredPropositionEdge
                }}
            }
            case RelationMatchState.MATCHED_SOURCE_NODE_ONLY => {
                if(isVerbOrNounOnDestination){
                analyze(idx, deductionQueries, edge, nodeMap, neo4JUtils, transversalState) match {
                    case Some(x) => Option(x)
                    case _ => {
                    if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                    else coveredPropositionEdge  
                    }}          
                }else {
                if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                else coveredPropositionEdge        
                }
            }
            case RelationMatchState.MATCHED_TARGET_NODE_ONLY => {
                if(isVerbOrNounOnSource) {
                analyze(idx, deductionQueries, edge, nodeMap, neo4JUtils, transversalState) match {
                    case Some(x) => Option(x)
                    case _ => {
                    if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                    else coveredPropositionEdge  
                    }}          
                }else {
                if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                else coveredPropositionEdge        
                }
            }
            case RelationMatchState.NOT_MATCHED_BOTH => {
                if(isVerbOrNounOnSource && isVerbOrNounOnDestination){
                analyze(idx, deductionQueries, edge, nodeMap, neo4JUtils, transversalState) match {
                    case Some(x) => Option(x)
                    case _ => {
                    if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                    else coveredPropositionEdge  
                    }}          
                }else {
                if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                else coveredPropositionEdge        
                }
            }
            }
        }
    }
  
    private def analyze(idx:Int, deductionQueries:List[DeductionQuery],edge:KnowledgeBaseEdge, nodeMap: Map[String, KnowledgeBaseNode], neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState):Option[CoveredPropositionEdge] = {
        val deductionUnitName = conf.getString("TOPOSOID_DEDUCTION_UNIT_NAME")
        val jsonStr: String = neo4JUtils.getCypherQueryResult(deductionQueries(idx).query, "", transversalState)
        //If there is even one that does not match, it is useless to search further
        if (!jsonStr.equals("""{"records":[]}""")) {
        //ヒットするものがある場合
        val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]      
        Option(DeductionUtils.getCoveredPropositionEdge(edge, deductionQueries(idx).sourceAlias, deductionQueries(idx).destinationAlias, nodeMap,  neo4jRecords, deductionQueries(idx).relationMatchState, deductionUnitName, deductionQueries(idx).featureSimilarityMap))        
        }else{
        None
        }
    }

    private def getCoveredPropositionEdge(edge: KnowledgeBaseEdge, sourceAlias:String, destinationAlias:String, nodeMap:Map[String, KnowledgeBaseNode], neo4jRecords: Neo4jRecords, relationMatchState:RelationMatchState, deductionUnitName:String, featureSimilarityMap:Map[String, Float] = Map.empty[String, Float]):CoveredPropositionEdge = {
        //一旦どちらかのノードが埋まっていれば推論を進めるものとする。        
        val (isConfirmedSource, isConfirmedDestination)= relationMatchState match {
            case RelationMatchState.MATCHED_BOTH => (true, true)
            case RelationMatchState.MATCHED_SOURCE_NODE_ONLY => (true, false)
            case RelationMatchState.MATCHED_TARGET_NODE_ONLY => (false, true)
            case RelationMatchState.NOT_MATCHED_BOTH => (false, false)
        } 

        val sourceNodeSurface = nodeMap.get(edge.sourceId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
        val destinationNodeSurface = nodeMap.get(edge.destinationId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface

        val sourceKnowledgeNodes = neo4jRecords.records.map(x => x.filter(y => y.key == sourceAlias).map(
        z => List(z.value.localNode, z.value.synonymNode, z.value.featureNode).flatten.head)).flatten.distinct

        val destinationKnowledgeNodes = neo4jRecords.records.map(x => x.filter(y => y.key == destinationAlias).map(
        z => List(z.value.localNode, z.value.synonymNode, z.value.featureNode).flatten.head)).flatten.distinct

        val sourceMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = sourceAlias match {
            case "" => List.empty[MatchedKnowledgeNode]
            case _ => getMatchedKnowledgeNodes(edge, sourceKnowledgeNodes, nodeMap.get(edge.sourceId).get, featureSimilarityMap)
        }

        val destinationMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = destinationAlias match {
            case "" =>  List.empty[MatchedKnowledgeNode]
            case _ => getMatchedKnowledgeNodes(edge, destinationKnowledgeNodes, nodeMap.get(edge.destinationId).get, featureSimilarityMap)
        }

        val sourceNode = CoveredPropositionNode(terminalId = edge.sourceId, terminalSurface = sourceNodeSurface, terminalUrl = "", matchedKnowledgeNodes=sourceMatchedKnowledgeNodes, isConfirmedSource, deductionUnitName)
        val destinationNode = CoveredPropositionNode(terminalId = edge.destinationId, terminalSurface = destinationNodeSurface, terminalUrl = "", matchedKnowledgeNodes=destinationMatchedKnowledgeNodes, isConfirmedDestination, deductionUnitName)        
        CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode)
    }

    private def getMatchedKnowledgeNodes(
        edge: KnowledgeBaseEdge, 
        serchedKnowledgeNodes:List[KnowledgeBaseNode | KnowledgeBaseSynonymNode | KnowledgeFeatureReference], 
        proopsitionNode: KnowledgeBaseNode,
        featureSimilarityMap:Map[String, Float]):List[MatchedKnowledgeNode]= {
        
        val emptyMatchedFeatureInfo = MatchedFeatureInfo("", -1.0)

        serchedKnowledgeNodes.map(x => {
            x match {
                case a:KnowledgeBaseNode => {
                    MatchedKnowledgeNode(
                        propositionId = a.propositionId,
                        sentenceId = a.sentenceId,
                        nodeId = a.nodeId,
                        caseNameOnEdge = edge.caseStr,
                        isDenialWord = a.predicateArgumentStructure.isDenialWord,
                        nodeType = a.predicateArgumentStructure.nodeType,
                        featureInfo = emptyMatchedFeatureInfo
                    )
                }
                case b:KnowledgeBaseSynonymNode => {
                    MatchedKnowledgeNode(
                        propositionId = b.propositionId,
                        sentenceId = b.sentenceId,
                        nodeId = b.nodeId,
                        caseNameOnEdge = edge.caseStr,
                        isDenialWord = proopsitionNode.predicateArgumentStructure.isDenialWord,
                        nodeType = proopsitionNode.predicateArgumentStructure.nodeType, 
                        featureInfo = emptyMatchedFeatureInfo
                    )
                }
                case c:KnowledgeFeatureReference => {

                    val matchedFeatureInfo = c.featureType match {
                        case FeatureType.IMAGE.index => {
                            MatchedFeatureInfo(c.featureId, featureSimilarityMap.getOrElse(c.featureId, -1.0F))
                        }
                        case _ => {
                            emptyMatchedFeatureInfo    
                        }
                    }
                    
                    MatchedKnowledgeNode(
                        propositionId = c.propositionId,
                        sentenceId = c.sentenceId,
                        nodeId = c.featureId,                        
                        caseNameOnEdge = edge.caseStr,
                        isDenialWord = proopsitionNode.predicateArgumentStructure.isDenialWord,
                        nodeType = proopsitionNode.predicateArgumentStructure.nodeType, 
                        featureInfo = matchedFeatureInfo 
                    )
                }
            }}
        )    
    }

}

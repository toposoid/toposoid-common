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

object DeductionUtils extends LazyLogging {

    def getUnsettledEdges(aso:AnalyzedSentenceObject): List[KnowledgeBaseEdge] = {
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

    def getCoveredPropositionEdge(edge: KnowledgeBaseEdge, sourceAlias:String, destinationAlias:String, nodeMap:Map[String, KnowledgeBaseNode], neo4jRecords: Neo4jRecords, relationMatchState:RelationMatchState, deductionUnitName:String):CoveredPropositionEdge = {
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
            case _ => getMatchedKnowledgeNodes(edge, sourceKnowledgeNodes, nodeMap.get(edge.sourceId).get, List.empty[MatchedFeatureInfo])
        }

        val destinationMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = destinationAlias match {
            case "" =>  List.empty[MatchedKnowledgeNode]
            case _ => getMatchedKnowledgeNodes(edge, destinationKnowledgeNodes, nodeMap.get(edge.destinationId).get, List.empty[MatchedFeatureInfo])
        }

        val sourceNode = CoveredPropositionNode(terminalId = edge.sourceId, terminalSurface = sourceNodeSurface, terminalUrl = "", matchedKnowledgeNodes=sourceMatchedKnowledgeNodes, isConfirmedSource, deductionUnitName)
        val destinationNode = CoveredPropositionNode(terminalId = edge.destinationId, terminalSurface = destinationNodeSurface, terminalUrl = "", matchedKnowledgeNodes=destinationMatchedKnowledgeNodes, isConfirmedDestination, deductionUnitName)        
        CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode)
    }

    def getMatchedKnowledgeNodes(
        edge: KnowledgeBaseEdge, 
        serchedKnowledgeNodes:List[KnowledgeBaseNode | KnowledgeBaseSynonymNode | KnowledgeFeatureReference], 
        proopsitionNode: KnowledgeBaseNode,
        featureInfoList:List[MatchedFeatureInfo]):List[MatchedKnowledgeNode]= {
        
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
                    featureInfoList = List.empty[MatchedFeatureInfo]
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
                    featureInfoList = List.empty[MatchedFeatureInfo]
                    )
                }
                case c:KnowledgeFeatureReference => {
                MatchedKnowledgeNode(
                    propositionId = c.propositionId,
                    sentenceId = c.sentenceId,
                    nodeId = c.featureId,
                    caseNameOnEdge = edge.caseStr,
                    isDenialWord = proopsitionNode.predicateArgumentStructure.isDenialWord,
                    nodeType = proopsitionNode.predicateArgumentStructure.nodeType, 
                    featureInfoList = List.empty[MatchedFeatureInfo]
                    )
                }
            }}
        )    
    }
}

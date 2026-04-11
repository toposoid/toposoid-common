
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

object DeductionUtils extends LazyLogging {

    def getUnsettledEdges(aso:AnalyzedSentenceObject): List[KnowledgeBaseEdge] = {
        val pairSetList = aso.deductionResult.coveredPropositionEdges.foldLeft(List.empty[Set[String]]){
            (acc, x) => {
            acc :+ Set(x.sourceNode.terminalId, x.destinationNode.terminalId)
            }
        }
        aso.edgeList.filterNot(x => {
        val targetLink = Set(x.sourceId, x.destinationId)
        pairSetList.contains(targetLink)
        })
    }

    def getCoveredPropositionEdge(edge: KnowledgeBaseEdge, sourceAlias:String, destinationAlias:String, nodeMap:Map[String, KnowledgeBaseNode], neo4jRecords: Neo4jRecords, relationMatchState:RelationMatchState):CoveredPropositionEdge = {
        //一旦どちらかのノードが埋まっていれば推論を進めるものとする。
        val sourceNodeSurface = nodeMap.get(edge.sourceId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
        val destinationNodeSurface = nodeMap.get(edge.destinationId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface

        val sourceKnowledgeNodes:List[KnowledgeBaseNode] = neo4jRecords.records.map(x => x.filter(y => y.key == sourceAlias).map(z => z.value.localNode.get)).flatten
        val destinationKnowledgeNodes:List[KnowledgeBaseNode] = neo4jRecords.records.map(x => x.filter(y => y.key == destinationAlias).map(z => z.value.localNode.get)).flatten

        val (isConfirmedSource, isConfirmedDestination)= relationMatchState match {
            case RelationMatchState.MATCHED_BOTH => (true, true)
            case RelationMatchState.MATCHED_SOURCE_NODE_ONLY => (true, false)
            case RelationMatchState.MATCHED_TARGET_NODE_ONLY => (false, true)
            case RelationMatchState.NOT_MATCHED_BOTH => (false, false)
        } 

        val sourceMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = sourceAlias match {
        case "" => {
            List.empty[MatchedKnowledgeNode]
        }
        case _ => {
            sourceKnowledgeNodes.map(x => {
            MatchedKnowledgeNode(
                propositionId = x.propositionId,
                sentenceId = x.sentenceId,
                nodeId = x.nodeId,
                caseNameOnEdge = edge.caseStr,
                isDenialWord = x.predicateArgumentStructure.isDenialWord,
                nodeType = x.predicateArgumentStructure.nodeType,
                featureInfoList = List.empty[MatchedFeatureInfo]
                )
            })
        }
        }
        val destinationMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = destinationAlias match {
        case "" => {
            List.empty[MatchedKnowledgeNode]
        }
        case _ => {
            destinationKnowledgeNodes.map(x => {
            MatchedKnowledgeNode(
                propositionId = x.propositionId,
                sentenceId = x.sentenceId,
                nodeId = x.nodeId,
                caseNameOnEdge = edge.caseStr,
                isDenialWord = x.predicateArgumentStructure.isDenialWord,
                nodeType = x.predicateArgumentStructure.nodeType,
                List.empty[MatchedFeatureInfo]

                )
            })
        }
        }

        //val knowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = List.empty[KnowledgeBaseSideInfo]
        val knowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = (sourceKnowledgeNodes:::destinationKnowledgeNodes).map(x => {   
        //TODO:すでにある deductionUnitsを追加しないといけない。           
        KnowledgeBaseSideInfo(propositionId=x.propositionId, sentenceId=x.sentenceId , featureInfoList = List.empty[MatchedFeatureInfo], deductionUnits = List("exact-match"))
        }).distinct

        //isConfirmed:Boolean, deductionUnit:String
        val sourceNode = CoveredPropositionNode(terminalId = edge.sourceId, terminalSurface = sourceNodeSurface, terminalUrl = "", matchedKnowledgeNodes=sourceMatchedKnowledgeNodes, isConfirmedSource, "exact-match")
        val destinationNode = CoveredPropositionNode(terminalId = edge.destinationId, terminalSurface = destinationNodeSurface, terminalUrl = "", matchedKnowledgeNodes=destinationMatchedKnowledgeNodes, isConfirmedDestination, "exact-match")
        //val knowledgeBaseSideInfo = KnowledgeBaseSideInfo(propositionId = , sentenceId = , featureInfoList = List.empty[MatchedFeatureInfo])
        CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode)
    }


}

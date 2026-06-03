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

import com.typesafe.scalalogging.LazyLogging
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.FeatureVectorSearchResult
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import play.api.libs.json.Json
import com.ideal.linked.toposoid.protocol.model.base.CoveredPropositionEdge
import com.ideal.linked.toposoid.protocol.model.base.MatchedKnowledgeNode
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObject
import com.ideal.linked.toposoid.protocol.model.base.CoveredPropositionNode
import com.ideal.linked.toposoid.protocol.model.base.MatchedFeatureInfo
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeBaseNode
import com.ideal.linked.toposoid.knowledgebase.featurevector.model.FeatureVectorIdentifier
import com.ideal.linked.common.DeploymentConverter.conf

case class FeatureVectorSearchInfo(propositionId:String, sentenceId:String, sentenceType:Int, lang:String, featureId:String, similarity:Float)

object DeductionUtilsForSemiGlobal extends LazyLogging {

    def extractExistInNeo4JResultForSentence(featureVectorSearchResult: FeatureVectorSearchResult, sentenceIdsForFilter:List[String], originalSentenceType: Int, transversalState:TransversalState): List[FeatureVectorSearchInfo] = {
        
        val neo4jUtils = Neo4JUtilsImpl()
        val sentenceIdFilterQuery = sentenceIdsForFilter.size match {
            case 0 => ""
            case _ => "AND n.sentenceId IN [%s]".format(sentenceIdsForFilter.mkString(","))
        }

        (featureVectorSearchResult.ids zip featureVectorSearchResult.similarities).foldLeft(List.empty[FeatureVectorSearchInfo]) {
            (acc, x) => {
                val idInfo = x._1
                val propositionId = idInfo.superiorId
                val lang = idInfo.lang
                val featureId = idInfo.featureId
                val similarity = x._2
                val nodeType: String = ToposoidUtils.getNodeType(idInfo.sentenceType, ScopeType.SEMIGLOBAL.index, FeatureType.SENTENCE.index)
                //Check whether featureVectorSearchResult information exists in Neo4J
                val query = "MATCH (n:%s) WHERE n.propositionId='%s' '%s' RETURN n".format(nodeType, propositionId, sentenceIdFilterQuery)
                val jsonStr: String = neo4jUtils.getCypherQueryResult(query, "", transversalState)
                val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
                neo4jRecords.records.size match {
                    case 0 => acc
                    case _ => {
                        /*
                        val idInfoOnNeo4jSide = neo4jRecords.records.head.head.value.semiGlobalNode.get
                        //sentenceType returns the originalSentenceType of the argument
                        acc :+ FeatureVectorSearchInfo(idInfoOnNeo4jSide.propositionId, idInfoOnNeo4jSide.sentenceId, originalSentenceType, lang, featureId, similarity)
                        */
                        acc ::: neo4jRecords.records.map(y => {
                            y.map(z => {
                                val semiGlobalNode = z.value.semiGlobalNode.get
                                FeatureVectorSearchInfo(semiGlobalNode.propositionId, semiGlobalNode.sentenceId, originalSentenceType, lang, featureId, similarity)
                            })
                        }).flatten 
                    }
                }
            }
        }.distinct
    }

    def getCoveredPropositionEdges(isConfirmed:Boolean, aso:AnalyzedSentenceObject ,featureVectorSearchResult: FeatureVectorSearchResult, transversalState:TransversalState): List[CoveredPropositionEdge] = {

        //既にdeductionResultが設定されている場合は、EmbedingSetenceMatchが候補を列挙したことになる。
        val sentenceIds = aso.deductionResult.coveredPropositionEdges.foldLeft(List.empty[String]){
            (acc, x) =>
                acc ++ x.sourceNode.matchedKnowledgeNodes.map(y => "'" + y.sentenceId + "'")
        }.distinct

        val (ids, similarities) = (featureVectorSearchResult.ids zip featureVectorSearchResult.similarities).foldLeft((List.empty[FeatureVectorIdentifier], List.empty[Float])) {
            (acc, x) => {
                x._1.sentenceType match {
                case SentenceType.CLAIM.index => (acc._1 :+ x._1, acc._2 :+ x._2)
                case _ => acc
                }
            }
        }

        val filteredResult = FeatureVectorSearchResult(ids, similarities, featureVectorSearchResult.statusInfo) 
        val deductionUnitName = conf.getString("TOPOSOID_DEDUCTION_UNIT_NAME")
        filteredResult.ids.size match {
        case 0 => List.empty[CoveredPropositionEdge]
        case _ => {        

            val sentenceIdsForFilter = sentenceIds.size match {
                case 0 => {
                    //この場合、DeductionPhaseTypeがDEDUCTION_SENTENCE_BASEということになる。
                    //この場合に限り、sentenceId = featureIdとなる。
                    filteredResult.ids.map(x => x.featureId)
                }
                case _ => {
                    sentenceIds
                }
            }

            val featureVectorSearchInfoList = DeductionUtilsForSemiGlobal.extractExistInNeo4JResultForSentence(filteredResult, sentenceIdsForFilter, aso.knowledgeBaseSemiGlobalNode.sentenceType, transversalState)        
            val matchedKnowledgeNodes = featureVectorSearchInfoList.map(x => {
                MatchedKnowledgeNode(
                    propositionId = x.propositionId,
                    sentenceId = x.sentenceId,
                    nodeId = "",
                    caseNameOnEdge = "",
                    isDenialWord = false,
                    nodeType = x.sentenceType,
                    featureInfo = MatchedFeatureInfo(featureId = x.featureId, similarity = x.similarity)
                )          
            })
            
            aso.deductionResult.coveredPropositionEdges.size match {
                case 0 => {
                    //coveredPropositionEdgesを新規作成
                    aso.edgeList.map(x => {
                        val sourceNode = aso.nodeMap.get(x.sourceId).get.asInstanceOf[KnowledgeBaseNode]
                        val destinationNode = aso.nodeMap.get(x.destinationId).get.asInstanceOf[KnowledgeBaseNode]
                        val sourceCoveredPropositionNode = CoveredPropositionNode(
                            terminalId = sourceNode.nodeId,
                            terminalSurface = sourceNode.predicateArgumentStructure.surface,
                            terminalUrl = "",
                            matchedKnowledgeNodes = matchedKnowledgeNodes,
                            isConfirmed = isConfirmed,
                            deductionUnit = deductionUnitName
                        )
                        val destinationCoveredPropositionNode = CoveredPropositionNode(
                            terminalId = destinationNode.nodeId,
                            terminalSurface = destinationNode.predicateArgumentStructure.surface,
                            terminalUrl = "",
                            matchedKnowledgeNodes = matchedKnowledgeNodes,
                            isConfirmed = isConfirmed,
                            deductionUnit = deductionUnitName
                        )
                        CoveredPropositionEdge(sourceCoveredPropositionNode, destinationCoveredPropositionNode)
                    }) 
                }
                case _ => {
                    //coveredPropositionEdgesを更新
                    //Embedingsの場合は、coveredPropositionEdges全体の洗い替えとなるので、matchedKnowledgeNodesは追記とする。
                    //過去の貢献のあった、feature情報も残す。
                    aso.deductionResult.coveredPropositionEdges.map(x => {                        
                        val updatedSourceNode = CoveredPropositionNode(
                            terminalId = x.sourceNode.terminalId,
                            terminalSurface = x.sourceNode.terminalSurface,
                            terminalUrl = x.sourceNode.terminalUrl,
                            matchedKnowledgeNodes = x.sourceNode.matchedKnowledgeNodes:::matchedKnowledgeNodes,
                            isConfirmed = isConfirmed,
                            deductionUnit = deductionUnitName
                        )
                        val updatedDestinationNode = CoveredPropositionNode(
                            terminalId = x.destinationNode.terminalId,
                            terminalSurface = x.destinationNode.terminalSurface,
                            terminalUrl = x.destinationNode.terminalUrl,
                            matchedKnowledgeNodes = x.destinationNode.matchedKnowledgeNodes:::matchedKnowledgeNodes,
                            isConfirmed = isConfirmed,
                            deductionUnit = deductionUnitName
                        )
                        CoveredPropositionEdge(updatedSourceNode, updatedDestinationNode)
                    }) 
                }
            }                        
            }
        }              
  }    

}

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

case class FeatureVectorSearchInfo(propositionId:String, sentenceId:String, sentenceType:Int, lang:String, featureId:String, similarity:Float)

object DeductionUtilsForSemiGlobal extends LazyLogging {

    def extractExistInNeo4JResultForSentence(featureVectorSearchResult: FeatureVectorSearchResult, originalSentenceType: Int, transversalState:TransversalState): List[FeatureVectorSearchInfo] = {
        val neo4jUtils = Neo4JUtilsImpl()
        (featureVectorSearchResult.ids zip featureVectorSearchResult.similarities).foldLeft(List.empty[FeatureVectorSearchInfo]) {
            (acc, x) => {
                val idInfo = x._1
                val propositionId = idInfo.superiorId
                val lang = idInfo.lang
                val featureId = idInfo.featureId
                val similarity = x._2
                val nodeType: String = ToposoidUtils.getNodeType(idInfo.sentenceType, ScopeType.SEMIGLOBAL.index, FeatureType.SENTENCE.index)
                //Check whether featureVectorSearchResult information exists in Neo4J
                val query = "MATCH (n:%s) WHERE n.propositionId='%s' AND n.sentenceId='%s' RETURN n".format(nodeType, propositionId, featureId)
                val jsonStr: String = neo4jUtils.getCypherQueryResult(query, "", transversalState)
                val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
                neo4jRecords.records.size match {
                    case 0 => acc
                    case _ => {
                        val idInfoOnNeo4jSide = neo4jRecords.records.head.head.value.semiGlobalNode.get
                        //sentenceType returns the originalSentenceType of the argument
                        acc :+ FeatureVectorSearchInfo(idInfoOnNeo4jSide.propositionId, idInfoOnNeo4jSide.sentenceId, originalSentenceType, lang, featureId, similarity)
                    }
                }
            }
        }
    }

}

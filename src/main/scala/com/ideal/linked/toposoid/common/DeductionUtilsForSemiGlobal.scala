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

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

import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode, KnowledgeBaseSemiGlobalNode, KnowledgeFeatureReference, LocalContext, LocalContextForFeature, PredicateArgumentStructure}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeForImage, KnowledgeForTable, KnowledgeSentenceSet}
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, DeductionResult}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.typesafe.scalalogging.LazyLogging
//import io.jvm.uuid.UUID

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import sttp.client4._
import play.api.libs.json.Json
import sttp.model.HttpVersion

import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.boundary, boundary.break
import com.ideal.linked.toposoid.protocol.model.base.KnowledgeBaseSideInfo
import com.ideal.linked.toposoid.protocol.model.base.CoveredPropositionEdge

/*
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
*/


/**
 * Common Utilities in all toposoid project.
 */
object ToposoidUtils extends LazyLogging{

  val langPatternJP: Regex = "^ja_.*".r
  val langPatternEN: Regex = "^en_.*".r
  val langPatternSpecialSymbol1: Regex = "^@@_#[0-9]+".r

  /**
   * Returns a formatted logger message.
   * @param message
   * @param username
   * @return
   */
  def formatMessageForLogger(message:String, username:String):String = Try{
    isEmpty(message) match {
      case true => "\t" + username
      case _ => message.replace("\t", " ") + "\t" + username
    }
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  def encodeJsonInJson(json:String):String = Try{
    json.replaceAll("\"", "___###DQ###___").replaceAll("\n", "\\\\n")
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  def decodeJsonInJson(json: String): String = Try {
    json.replaceAll("___###DQ###___", "\"").replaceAll("\\\\n", "\n")
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  def isEmpty(x: String) = x == null || x.trim.isEmpty

  /**
   * Returns the Neo4J node type that corresponds to the sentenceType and featureType.
   * @param sentenceType
   * @param featureType
   * @return
   */
  def getNodeType(sentenceType:Int, scopeType: Int, featureType: Int): String = Try{
    (sentenceType, scopeType, featureType) match{
      case (SentenceType.PREMISE.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index) => "PremiseNode"
      case (SentenceType.CLAIM.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index) => "ClaimNode"
      case (SentenceType.PREMISE.index, ScopeType.SEMIGLOBAL.index, FeatureType.SENTENCE.index) => "SemiGlobalPremiseNode"
      case (SentenceType.CLAIM.index, ScopeType.SEMIGLOBAL.index, FeatureType.SENTENCE.index) => "SemiGlobalClaimNode"
      case (SentenceType.PREMISE.index, ScopeType.GLOBAL.index, FeatureType.DOCUMENT.index) => "GlobalPremiseNode"
      case (SentenceType.CLAIM.index, ScopeType.GLOBAL.index, FeatureType.DOCUMENT.index) => "GlobalClaimNode"
      case _ => {
          featureType match {
            case FeatureType.SYNONYM.index => "SynonymNode"
            case FeatureType.IMAGE.index => "ImageNode"
            case FeatureType.TABLE.index => "TableNode"
            case _ => throw new Exception("Unknown NodeType")
          }
      }
    }
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }


  def callComponent(json:String, host:String, port:String, serviceName:String, transversalState:TransversalState): String =Try {
    
    val result:List[String] = Range(0, conf.getInt("retryCallMicroserviceNum")).foldLeft(List("{}")){
      (acc, x) => {
        val latestResult:String = acc.last
        latestResult match  {
          case "{}" => {
            acc :+ this.callComponentImpl(json, host, port, serviceName, transversalState)
          } 
          case _ => {
            acc            
          }
        } 
      }
    }
    if(result.last == "{}") throw new Exception("Results were not returned properly")
    result.last

  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }


  /**
   * This function calls multiple microservices.
   * @param json
   * @param host
   * @param port
   * @param serviceName
   * @return
   */



  private def callComponentImpl(json:String, host:String, port:String, serviceName:String, transversalState:TransversalState): String = {
    val url = s"http://${host}:${port}/${serviceName}"
    val backend = DefaultSyncBackend(
      options = BackendOptions.connectionTimeout(1.minute))

    val request = basicRequest
      .contentType("application/json")
      .header(TRANSVERSAL_STATE.str, Json.toJson(transversalState).toString())
      .readTimeout(5.minutes)
      .httpVersion(HttpVersion.HTTP_1_1)
      .body(json)
      .post(uri"${url}")
    val response = request.send(backend)

    response.body match {
      case Left(error) => {
        logger.error(formatMessageForLogger(json, transversalState.userId))
        logger.error(formatMessageForLogger(s"Failure: $error", transversalState.userId))
        "{}"
      }
      case Right(data) => data
    }
  }
  /*
  private def callComponentImpl(json:String, host:String, port:String, serviceName:String, transversalState:TransversalState): String = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    val entity = HttpEntity(ContentTypes.`application/json`, json)
    val request = HttpRequest(uri = "http://" + host + ":" + port + "/" + serviceName, method = HttpMethods.POST, entity = entity)
                  .withHeaders(RawHeader(TRANSVERSAL_STATE.str, Json.toJson(transversalState).toString()))
    val result = Http().singleRequest(request)
      .flatMap { res =>
        Unmarshal(res).to[String].map { data =>
          Json.parse(data.getBytes("UTF-8"))
        }
      }
    var queryResultJson:String = "{}"
    result.onComplete {
      case Success(js) =>
        //println(s"Success: $js")
        logger.debug(formatMessageForLogger(s"Success: $js", transversalState.userId))
        queryResultJson = s"$js"
      case Failure(e) =>
        //println(s"Failure: $e")
        logger.error(formatMessageForLogger(s"Failure: $e", transversalState.userId))
    }

    while(!result.isCompleted){
      Thread.sleep(20)
    }
    queryResultJson
  }
  */

  private def convertKnowledge(knowledge: Knowledge): Knowledge = {
    val knowledgeForImages: List[KnowledgeForImage] = knowledge.knowledgeForImages.map(y => {
      Option(y.id) match {
        case Some(x) => {
          if (x.trim().equals("")) KnowledgeForImage(java.util.UUID.randomUUID().toString, y.imageReference)
          else y
        }
        case None => {
          KnowledgeForImage(java.util.UUID.randomUUID().toString, y.imageReference)
        }
      }
    })
    val knowledgeForTables: List[KnowledgeForTable] = knowledge.knowledgeForTables.map(y => {
      Option(y.id) match {
        case Some(x) => {
          if (x.trim().equals("")) KnowledgeForTable(java.util.UUID.randomUUID().toString, y.tableReference)
          else y
        }
        case None => {
          KnowledgeForTable(java.util.UUID.randomUUID().toString, y.tableReference)
        }
      }
    })
    Knowledge(knowledge.sentence, knowledge.lang, knowledge.extentInfoJson, knowledge.isNegativeSentence, knowledgeForImages, knowledgeForTables, knowledgeForDocument = knowledge.knowledgeForDocument, documentPageReference = knowledge.documentPageReference)
  }

  def assignId(knowledgeSentenceSet: KnowledgeSentenceSet): (KnowledgeSentenceSetForParser, String) = {
    val propositionId = java.util.UUID.randomUUID().toString
    val knowledgeForParserPremise: List[KnowledgeForParser] = knowledgeSentenceSet.premiseList.map(x => KnowledgeForParser(propositionId, java.util.UUID.randomUUID().toString, convertKnowledge(x)))
    val knowledgeForParserClaim: List[KnowledgeForParser] = knowledgeSentenceSet.claimList.map(x => KnowledgeForParser(propositionId, java.util.UUID.randomUUID().toString, convertKnowledge(x)))

    (KnowledgeSentenceSetForParser(
      premiseList = knowledgeForParserPremise,
      premiseLogicRelation = knowledgeSentenceSet.premiseLogicRelation,
      claimList = knowledgeForParserClaim,
      claimLogicRelation = knowledgeSentenceSet.claimLogicRelation
    ), propositionId)
  }


  def parseSpecialSymbol(knowledgeForParser: KnowledgeForParser): AnalyzedSentenceObject = {

    val propositionId = knowledgeForParser.propositionId
    val sentenceId = knowledgeForParser.sentenceId
    val documentId = knowledgeForParser.knowledge.knowledgeForDocument.id

    val localContext = LocalContext(
      lang = knowledgeForParser.knowledge.lang,
      namedEntities = Map.empty[String, String],
      rangeExpressions = Map.empty[String, Map[String, String]],
      categories = Map.empty[String, String],
      domains = Map.empty[String, String],
      knowledgeFeatureReferences = List.empty[KnowledgeFeatureReference],
      properNouns = Map.empty[String, String]
    )

    val caseType = "-"

    val predicateArgumentStructure = PredicateArgumentStructure(
      currentId = 0,
      parentId = -1,
      isMainSection = true,
      surface = knowledgeForParser.knowledge.sentence,
      normalizedName = knowledgeForParser.knowledge.sentence,
      dependType = "-",
      caseType = caseType,
      isDenialWord = false,
      isConditionalConnection = false,
      surfaceYomi = "",
      normalizedNameYomi = "",
      modalityType = "-",
      parallelType = "-",
      nodeType = 1,
      morphemes = List("-"),
      caseGroupType = CaseGroupType.UNSPECIFIED.index,
      casePhraseId = "",
      casePhrase = ""
    )

    val node = KnowledgeBaseNode(
      nodeId = sentenceId + "-0",
      propositionId = propositionId,
      sentenceId = sentenceId,
      predicateArgumentStructure = predicateArgumentStructure,
      localContext = localContext,
    )
    val nodeMap = Map(sentenceId + "-0" -> node)

    val localContextForFeature = LocalContextForFeature(
      lang = knowledgeForParser.knowledge.lang,
      knowledgeFeatureReferences = List.empty[KnowledgeFeatureReference]
    )

    val knowledgeBaseSemiGlobalNode = KnowledgeBaseSemiGlobalNode(
      sentenceId = sentenceId,
      propositionId = propositionId,
      documentId = documentId,
      sentence = knowledgeForParser.knowledge.sentence,
      sentenceType = 1,
      localContextForFeature = localContextForFeature,
    )

    val defaultDeductionResult = DeductionResult(
      status = false,
      authenticityType = AuthenticityType.UNKNOWN.index,
      coveredPropositionEdges = List.empty[CoveredPropositionEdge],
      evidenceKnowledgeList = List.empty[KnowledgeBaseSideInfo]
    )
    AnalyzedSentenceObject(
      nodeMap = nodeMap,
      edgeList = List.empty[KnowledgeBaseEdge],
      knowledgeBaseSemiGlobalNode = knowledgeBaseSemiGlobalNode,
      deductionResult = defaultDeductionResult
    )
  }

  def escapeSingleQuote(s: String): String = {
    s.replaceAll("'", "＇")
  }

  def escapeDoubleQuote(s: String): String = {
    s.replaceAll("\"", "＂")
  }

  def preprocessForSentence(knowledgeList: List[Knowledge]):List[Knowledge] = {
    knowledgeList.map(x => {
      Knowledge(
        sentence = escapeDoubleQuote(escapeSingleQuote(x.sentence)),
        lang = x.lang,
        extentInfoJson = x.extentInfoJson,
        isNegativeSentence = x.isNegativeSentence,
        knowledgeForImages = x.knowledgeForImages,
        knowledgeForTables = x.knowledgeForTables,
        knowledgeForDocument = x.knowledgeForDocument,
        documentPageReference = x.documentPageReference
      )
    })
  }


}

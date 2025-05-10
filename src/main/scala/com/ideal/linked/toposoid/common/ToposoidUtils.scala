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

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeForImage, KnowledgeForTable, KnowledgeSentenceSet}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import io.jvm.uuid.UUID

import java.net.URI
import scala.util.{Failure, Success, Try}


/**
 * Common Utilities in all toposoid project.
 */
object ToposoidUtils extends LazyLogging{

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
      case (PREMISE.index, LOCAL.index, PREDICATE_ARGUMENT.index) => "PremiseNode"
      case (CLAIM.index, LOCAL.index, PREDICATE_ARGUMENT.index) => "ClaimNode"
      case (PREMISE.index, SEMIGLOBAL.index, SENTENCE.index) => "SemiGlobalPremiseNode"
      case (CLAIM.index, SEMIGLOBAL.index, SENTENCE.index) => "SemiGlobalClaimNode"
      case (PREMISE.index, GLOBAL.index, DOCUMENT.index) => "GlobalPremiseNode"
      case (CLAIM.index, GLOBAL.index, DOCUMENT.index) => "GlobalClaimNode"
      case _ => {
          featureType match {
            case SYNONYM.index => "SynonymNode"
            case IMAGE.index => "ImageNode"
            case TABLE.index => "TableNode"
            case _ => throw new Exception("Unknown NodeType")
          }
      }
    }
  } match {
    case Success(s) => s
    case Failure(e) => throw e
  }


  def callComponent(json:String, host:String, port:String, serviceName:String, transversalState:TransversalState): String =Try {
    val retryNum =  conf.getInt("retryCallMicroserviceNum") -1
    for (i <- 0 to retryNum) {
      val result:String  = this.callComponentImpl(json, host, port, serviceName, transversalState)
      if (result != "{}") {
        return result
      }
      if(i == retryNum) throw new Exception("Results were not returned properly")
    }
    ""
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

  def publishMessage(json: String, mqHost:String, mqPort:String, queueName:String, region:Region = Region.AP_NORTHEAST_1 ): Unit = {

    implicit val actorSystem = ActorSystem("example")

    val testEndPoint = "http://" + mqHost + ":" + mqPort
    val queueUrl = testEndPoint + "/" + queueName

    val sqs = SqsAsyncClient
      .builder()
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create(conf.getString("TOPOSOID_MQ_ACCESS_KEY"), conf.getString("TOPOSOID_MQ_SECRET_KEY")) // (1)
        )
      )
      .endpointOverride(URI.create(testEndPoint)) // (2)
      .region(region)
      .httpClient(AkkaHttpClient.builder()
        .withActorSystem(actorSystem).build())
      .build()

    sqs.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(queueUrl)
        .messageGroupId("x")
        .messageBody(json)
        .build()
    ).join()

  }

  private def convertKnowledge(knowledge: Knowledge): Knowledge = {
    val knowledgeForImages: List[KnowledgeForImage] = knowledge.knowledgeForImages.map(y => {
      Option(y.id) match {
        case Some(x) => {
          if (x.trim().equals("")) KnowledgeForImage(UUID.random.toString, y.imageReference)
          else y
        }
        case None => {
          KnowledgeForImage(UUID.random.toString, y.imageReference)
        }
      }
    })
    val knowledgeForTables: List[KnowledgeForTable] = knowledge.knowledgeForTables.map(y => {
      Option(y.id) match {
        case Some(x) => {
          if (x.trim().equals("")) KnowledgeForTable(UUID.random.toString, y.tableReference)
          else y
        }
        case None => {
          KnowledgeForTable(UUID.random.toString, y.tableReference)
        }
      }
    })
    Knowledge(knowledge.sentence, knowledge.lang, knowledge.extentInfoJson, knowledge.isNegativeSentence, knowledgeForImages, knowledgeForTables, knowledgeForDocument = knowledge.knowledgeForDocument, documentPageReference = knowledge.documentPageReference)
  }

  def assignId(knowledgeSentenceSet: KnowledgeSentenceSet): (KnowledgeSentenceSetForParser, String) = {
    val propositionId = UUID.random.toString
    val knowledgeForParserPremise: List[KnowledgeForParser] = knowledgeSentenceSet.premiseList.map(x => KnowledgeForParser(propositionId, UUID.random.toString, convertKnowledge(x)))
    val knowledgeForParserClaim: List[KnowledgeForParser] = knowledgeSentenceSet.claimList.map(x => KnowledgeForParser(propositionId, UUID.random.toString, convertKnowledge(x)))

    (KnowledgeSentenceSetForParser(
      premiseList = knowledgeForParserPremise,
      premiseLogicRelation = knowledgeSentenceSet.premiseLogicRelation,
      claimList = knowledgeForParserClaim,
      claimLogicRelation = knowledgeSentenceSet.claimLogicRelation
    ), propositionId)
  }



}

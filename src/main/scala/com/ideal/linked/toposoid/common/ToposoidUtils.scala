/*
 * Copyright 2021 Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ideal.linked.toposoid.common

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.ideal.linked.common.DeploymentConverter.conf
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

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


  def callComponent(json:String, host:String, port:String, serviceName:String, userName:String): String =Try {
    val retryNum =  conf.getInt("retryCallMicroserviceNum") -1
    for (i <- 0 to retryNum) {
      val result:String  = this.callComponentImpl(json, host, port, serviceName, userName)
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
  private def callComponentImpl(json:String, host:String, port:String, serviceName:String, userName:String): String = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    val entity = HttpEntity(ContentTypes.`application/json`, json)
    val request = HttpRequest(uri = "http://" + host + ":" + port + "/" + serviceName, method = HttpMethods.POST, entity = entity)
                  .withHeaders(
                    RawHeader(USERNAME.str, userName)
                  )
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
        logger.debug(s"Success: $js")
        queryResultJson = s"$js"
      case Failure(e) =>
        //println(s"Failure: $e")
        logger.error(s"Failure: $e")
    }

    while(!result.isCompleted){
      Thread.sleep(20)
    }
    queryResultJson
  }
}

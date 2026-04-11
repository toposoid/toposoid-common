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
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import play.api.libs.json.Json
import scala.util.{Failure, Success, Try}

trait Neo4JUtils {
  def executeQuery(query: String, transversalState: TransversalState): Unit
  def executeQueryAndReturn(query: String, transversalState: TransversalState): Neo4jRecords
}

class Neo4JUtilsImpl extends Neo4JUtils {
  override def executeQuery(query: String, transversalState: TransversalState): Unit = {
    val convertQuery = ToposoidUtils.encodeJsonInJson(query)
    val json = s"""{ "query":"$convertQuery", "target": "" }"""
    val res = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_GRAPHDB_WEB_HOST"), conf.getString("TOPOSOID_GRAPHDB_WEB_PORT"), "executeQuery", transversalState)
    if (!res.equals("""{"status":"OK","message":""}""")) {
      throw new Exception("Cypher query execution failed. " + res)
    }
  }
  override def executeQueryAndReturn(query: String, transversalState: TransversalState): Neo4jRecords  = {
    val convertQuery = ToposoidUtils.encodeJsonInJson(query)
    val json = s"""{ "query":"$convertQuery", "target": "" }"""
    val jsonResult = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_GRAPHDB_WEB_HOST"), conf.getString("TOPOSOID_GRAPHDB_WEB_PORT"), "getQueryFormattedResult", transversalState)
    Json.parse(jsonResult).as[Neo4jRecords]
  }


  def getCypherQueryResult(query:String, target:String, transversalState:TransversalState): String = Try{
    val retryNum =  conf.getInt("retryCallMicroserviceNum") -1

    val json: Option[String] = (0 to retryNum).toList.foldLeft(Option.empty[String]){
      (opt, i) => {
        opt match {
          case Some(v) => opt
          case None    => {
            val json = """{ "query":"%s", "target":"%s" }""".format(ToposoidUtils.encodeJsonInJson(query), target)      
            val result:String = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_GRAPHDB_WEB_HOST"), conf.getString("TOPOSOID_GRAPHDB_WEB_PORT"), "getQueryFormattedResult",transversalState)
            if(result != "{}") {
              Option(result)
            }else{
              None
            } 
          }
        }
      }
    }
    json match {
      case Some(v) => v
      case None => throw new Exception("Results were not returned properly")
    }
  }match {
      case Success(s) => s
      case Failure(e) => throw e
  }
}

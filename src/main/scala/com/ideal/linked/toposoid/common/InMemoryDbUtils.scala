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
import com.ideal.linked.toposoid.protocol.model.frontend.Endpoint
import com.ideal.linked.toposoid.protocol.model.redis.KeyValueStoreInfo
import play.api.libs.json.Json



object InMemoryDbUtils {


  private def getDefaultEndPoints(deductionPhaseType:DeductionPhaseType, isGroupDeduction:Boolean):Seq[Endpoint] = {

    Option(deductionPhaseType) match {
        case Some(x) => {
            deductionPhaseType match {
                case DeductionPhaseType.DEDUCTION_PHRASE_BASE => {
                  val deductionUnitHosts = Json.parse(conf.getString("TOPOSOID_HYBRID_DEDUCTION_UNITS")).as[List[String]]
                  val deductionUnitPorts = Json.parse(conf.getString("TOPOSOID_HYBRID_DEDUCTION_PORTS")).as[List[String]]
                  val deductionUnitNames = Json.parse(conf.getString("TOPOSOID_HYBRID_DEDUCTION_NAMES")).as[List[String]]
                  deductionUnitHosts.lazyZip(deductionUnitPorts).lazyZip(deductionUnitNames).map { (x, y, z) =>
                    Endpoint(x,y,z)
                  }.toSeq
                }
                case DeductionPhaseType.DEDUCTION_SENTENCE_BASE => {
                  val deductionUnitHosts = Json.parse(conf.getString("TOPOSOID_EMBEDDING_DEDUCTION_UNITS")).as[List[String]]
                  val deductionUnitPorts = Json.parse(conf.getString("TOPOSOID_EMBEDDING_DEDUCTION_PORTS")).as[List[String]]
                  val deductionUnitNames = Json.parse(conf.getString("TOPOSOID_EMBEDDING_DEDUCTION_NAMES")).as[List[String]]
                  deductionUnitHosts.lazyZip(deductionUnitPorts).lazyZip(deductionUnitNames).map { (x, y, z) =>
                    Endpoint(x,y,z)
                  }.toSeq
                }
                case DeductionPhaseType.DEDUCTION_TERM_BASE => {
                  val deductionUnitHosts = Json.parse(conf.getString("TOPOSOID_CLAUSE_DEDUCTION_UNITS")).as[List[String]]
                  val deductionUnitPorts = Json.parse(conf.getString("TOPOSOID_CLAUSE_DEDUCTION_PORTS")).as[List[String]]
                  val deductionUnitNames = Json.parse(conf.getString("TOPOSOID_CLAUSE_DEDUCTION_NAMES")).as[List[String]]
                  deductionUnitHosts.lazyZip(deductionUnitPorts).lazyZip(deductionUnitNames).map { (x, y, z) =>
                    Endpoint(x,y,z)
                  }.toSeq
                }
                case _ => Seq.empty[Endpoint]
            }
        } 
        case None => {
          if(isGroupDeduction) {
            val deductionUnitHosts = Json.parse(conf.getString("TOPOSOID_DEDUCTION_GROUP_UNITS")).as[List[String]]
            val deductionUnitPorts = Json.parse(conf.getString("TOPOSOID_DEDUCTION_GROUP_PORTS")).as[List[String]]
            val deductionUnitNames = Json.parse(conf.getString("TOPOSOID_DEDUCTION_GROUP_NAMES")).as[List[String]]
            deductionUnitHosts.lazyZip(deductionUnitPorts).lazyZip(deductionUnitNames).map { (x, y, z) =>
              Endpoint(x,y,z)
            }.toSeq

          }else{
            Seq.empty[Endpoint]
          }           
        }
    }

    
  }

  def getEmbedingDeducitonUnitEndPoints(transversalState: TransversalState): Seq[Endpoint] = {
    val userInfo = KeyValueStoreInfo(identifier = transversalState.userId, key = "EMBEDDING_DEDUCTION_UNIT_ENDPOINTS", value = "")
    val responseJson = ToposoidUtils.callComponent(
      Json.toJson(userInfo).toString(),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_HOST"),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_PORT"),
      "getData",
      transversalState)
    val responseUserInfo: KeyValueStoreInfo = Json.parse(responseJson).as[KeyValueStoreInfo]
    responseUserInfo.value match {
      case "" => getDefaultEndPoints(DeductionPhaseType.DEDUCTION_SENTENCE_BASE, false)
      case _ => Json.parse(responseUserInfo.value).as[Seq[Endpoint]]
    }
  }

  def setEmbedingDeducitonUnitEndPoints(endPoints: Seq[Endpoint], transversalState: TransversalState): Seq[Endpoint] = {

    val updatedEndPoints: Seq[Endpoint] = Option(endPoints) match {
      case Some(x) => endPoints
      case None => getDefaultEndPoints(DeductionPhaseType.DEDUCTION_SENTENCE_BASE, false)
    }

    val userInfo = KeyValueStoreInfo(identifier = transversalState.userId, key = "EMBEDDING_DEDUCTION_UNIT_ENDPOINTS", value = Json.toJson(updatedEndPoints).toString())
    val responseJson = ToposoidUtils.callComponent(
      Json.toJson(userInfo).toString(),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_HOST"),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_PORT"),
      "setData",
      transversalState)
    updatedEndPoints

  }
  def getClauseDeducitonUnitEndPoints(transversalState: TransversalState): Seq[Endpoint] = {    
    val userInfo = KeyValueStoreInfo(identifier = transversalState.userId, key = "CLAUSE_DEDUCTION_UNIT_ENDPOINTS", value = "")
    val responseJson = ToposoidUtils.callComponent(
      Json.toJson(userInfo).toString(),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_HOST"),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_PORT"),
      "getData",
      transversalState)
    val responseUserInfo: KeyValueStoreInfo = Json.parse(responseJson).as[KeyValueStoreInfo]
    responseUserInfo.value match {
      case "" => getDefaultEndPoints(DeductionPhaseType.DEDUCTION_TERM_BASE, false)
      case _ => Json.parse(responseUserInfo.value).as[Seq[Endpoint]]
    }
  }

  def setClauseDeducitonUnitEndPoints(endPoints: Seq[Endpoint], transversalState: TransversalState): Seq[Endpoint] = {

    val updatedEndPoints: Seq[Endpoint] = Option(endPoints) match {
      case Some(x) => endPoints
      case None => getDefaultEndPoints(DeductionPhaseType.DEDUCTION_TERM_BASE, false)
    }
    val userInfo = KeyValueStoreInfo(identifier = transversalState.userId, key = "CLAUSE_DEDUCTION_UNIT_ENDPOINTS", value = Json.toJson(updatedEndPoints).toString())
    val responseJson = ToposoidUtils.callComponent(
      Json.toJson(userInfo).toString(),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_HOST"),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_PORT"),
      "setData",
      transversalState)
    updatedEndPoints

  }

  def getDeductionGroupEndPoints(transversalState: TransversalState): Seq[Endpoint] = {
    val userInfo = KeyValueStoreInfo(identifier = transversalState.userId, key = "DEDUCTION_GROUP_ENDPOINTS", value = "")
    val responseJson = ToposoidUtils.callComponent(
      Json.toJson(userInfo).toString(),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_HOST"),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_PORT"),
      "getData",
      transversalState)
    val responseUserInfo: KeyValueStoreInfo = Json.parse(responseJson).as[KeyValueStoreInfo]
    responseUserInfo.value match {
      case "" => getDefaultEndPoints(null, true)
      case _ => Json.parse(responseUserInfo.value).as[Seq[Endpoint]]
    }
  }

  def setDeductionGroupEndPoints(endPoints: Seq[Endpoint], transversalState: TransversalState): Seq[Endpoint] = {

    val updatedEndPoints: Seq[Endpoint] = Option(endPoints) match {
      case Some(x) => endPoints
      case None => getDefaultEndPoints(null, true)
    }

    val userInfo = KeyValueStoreInfo(identifier = transversalState.userId, key = "DEDUCTION_GROUP_ENDPOINTS", value = Json.toJson(updatedEndPoints).toString())
    val responseJson = ToposoidUtils.callComponent(
      Json.toJson(userInfo).toString(),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_HOST"),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_PORT"),
      "setData",
      transversalState)
    updatedEndPoints

  }

}

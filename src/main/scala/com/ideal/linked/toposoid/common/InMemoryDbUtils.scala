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
  /*
  private 
  */
  def getEndPoints(transversalState: TransversalState): Seq[Endpoint] = {
    val userInfo = KeyValueStoreInfo(identifier = transversalState.userId, key = "DEDUCTION_UNIT_ENDPOINTS", value = "")
    val responseJson = ToposoidUtils.callComponent(
      Json.toJson(userInfo).toString(),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_HOST"),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_PORT"),
      "getData",
      transversalState)
    val responseUserInfo: KeyValueStoreInfo = Json.parse(responseJson).as[KeyValueStoreInfo]
    responseUserInfo.value match {
      case "" => setEndPoints(null, transversalState)
      case _ => Json.parse(responseUserInfo.value).as[Seq[Endpoint]]
    }
  }

  def setEndPoints(endPoints: Seq[Endpoint], transversalState: TransversalState): Seq[Endpoint] = {

    val defaultEndPoints: Seq[Endpoint] = Seq(
      Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT1_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT1_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT1_NAME")),
      Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT2_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT2_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT2_NAME")),
      Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT3_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT3_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT3_NAME")),
      Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT4_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT4_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT4_NAME")),
      Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT5_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT5_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT5_NAME"))
    )

    val updatedEndPoints: Seq[Endpoint] = Option(endPoints) match {
      case Some(x) => endPoints
      case None => defaultEndPoints
    }

    val userInfo = KeyValueStoreInfo(identifier = transversalState.userId, key = "DEDUCTION_UNIT_ENDPOINTS", value = Json.toJson(updatedEndPoints).toString())
    val responseJson = ToposoidUtils.callComponent(
      Json.toJson(userInfo).toString(),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_HOST"),
      conf.getString("TOPOSOID_IN_MEMORY_DB_WEB_PORT"),
      "setData",
      transversalState)
    updatedEndPoints

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
      case "" => setEndPoints(null, transversalState)
      case _ => Json.parse(responseUserInfo.value).as[Seq[Endpoint]]
    }
  }

  def setEmbedingDeducitonUnitEndPoints(endPoints: Seq[Endpoint], transversalState: TransversalState): Seq[Endpoint] = {


    val deductionUnitHosts = Json.parse(conf.getString("TOPODOID_EMBEDDING_DEDUCTION_UNITS")).as[List[String]]
    val deductionUnitPorts = Json.parse(conf.getString("TOPODOID_EMBEDDING_DEDUCTION_PORTS")).as[List[String]]
    val deductionUnitNames = Json.parse(conf.getString("TOPODOID_EMBEDDING_DEDUCTION_NAMES")).as[List[String]]

    val defaultEndPoints: Seq[Endpoint] = deductionUnitHosts.lazyZip(deductionUnitPorts).lazyZip(deductionUnitNames).map { (x, y, z) =>
      Endpoint(x,y,z)
    }.toSeq

    val updatedEndPoints: Seq[Endpoint] = Option(endPoints) match {
      case Some(x) => endPoints
      case None => defaultEndPoints
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
      case "" => setEndPoints(null, transversalState)
      case _ => Json.parse(responseUserInfo.value).as[Seq[Endpoint]]
    }
  }

  def setClauseDeducitonUnitEndPoints(endPoints: Seq[Endpoint], transversalState: TransversalState): Seq[Endpoint] = {

    val deductionUnitHosts = Json.parse(conf.getString("TOPODOID_CLAUSE_DEDUCTION_UNITS")).as[List[String]]
    val deductionUnitPorts = Json.parse(conf.getString("TOPODOID_CLAUSE_DEDUCTION_PORTS")).as[List[String]]
    val deductionUnitNames = Json.parse(conf.getString("TOPODOID_CLAUSE_DEDUCTION_NAMES")).as[List[String]]

    val defaultEndPoints: Seq[Endpoint] = deductionUnitHosts.lazyZip(deductionUnitPorts).lazyZip(deductionUnitNames).map { (x, y, z) =>
      Endpoint(x,y,z)
    }.toSeq

    val updatedEndPoints: Seq[Endpoint] = Option(endPoints) match {
      case Some(x) => endPoints
      case None => defaultEndPoints
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
      case "" => setEndPoints(null, transversalState)
      case _ => Json.parse(responseUserInfo.value).as[Seq[Endpoint]]
    }
  }

  def setDeductionGroupEndPoints(endPoints: Seq[Endpoint], transversalState: TransversalState): Seq[Endpoint] = {

    val deductionUnitHosts = Json.parse(conf.getString("TOPODOID_DEDUCTION_GROUP_UNITS")).as[List[String]]
    val deductionUnitPorts = Json.parse(conf.getString("TOPODOID_DEDUCTION_GROUP_PORTS")).as[List[String]]
    val deductionUnitNames = Json.parse(conf.getString("TOPODOID_DEDUCTION_GROUP_NAMES")).as[List[String]]

    val defaultEndPoints: Seq[Endpoint] = deductionUnitHosts.lazyZip(deductionUnitPorts).lazyZip(deductionUnitNames).map { (x, y, z) =>
      Endpoint(x,y,z)
    }.toSeq


    val updatedEndPoints: Seq[Endpoint] = Option(endPoints) match {
      case Some(x) => endPoints
      case None => defaultEndPoints
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

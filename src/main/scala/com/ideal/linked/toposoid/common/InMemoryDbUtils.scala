package com.ideal.linked.toposoid.common

import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.protocol.model.frontend.Endpoint
import com.ideal.linked.toposoid.protocol.model.redis.KeyValueStoreInfo
import play.api.libs.json.Json

object InMemoryDbUtils {

  private val defaultEndPoints: Seq[Endpoint] = Seq(
    Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT1_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT1_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT1_NAME")),
    Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT2_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT2_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT2_NAME")),
    Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT3_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT3_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT3_NAME")),
    Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT4_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT4_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT4_NAME")),
    Endpoint(conf.getString("TOPOSOID_DEDUCTION_UNIT5_HOST"), port = conf.getString("TOPOSOID_DEDUCTION_UNIT5_PORT"), name = conf.getString("TOPOSOID_DEDUCTION_UNIT5_NAME")))

  def getEndPointsFromInMemoryDB(transversalState: TransversalState): Seq[Endpoint] = {
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
}

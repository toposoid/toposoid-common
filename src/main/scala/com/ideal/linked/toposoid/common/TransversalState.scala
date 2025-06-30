package com.ideal.linked.toposoid.common

import play.api.libs.json.{Json, OWrites, Reads}

case class TransversalState(userId: String, roleId: Int, username: String, csrfToken: String)
object TransversalState {
  implicit val jsonWrites: OWrites[TransversalState] = Json.writes[TransversalState]
  implicit val jsonReads: Reads[TransversalState] = Json.reads[TransversalState]
}




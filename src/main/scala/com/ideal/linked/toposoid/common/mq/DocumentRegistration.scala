package com.ideal.linked.toposoid.common.mq

import com.ideal.linked.toposoid.common.TransversalState
import com.ideal.linked.toposoid.knowledgebase.document.model.Document
import play.api.libs.json.{Json, OWrites, Reads}

case class DocumentRegistration(document:Document, transversalState:TransversalState)
object DocumentRegistration {
  implicit val jsonWrites: OWrites[DocumentRegistration] = Json.writes[DocumentRegistration]
  implicit val jsonReads: Reads[DocumentRegistration] = Json.reads[DocumentRegistration]
}


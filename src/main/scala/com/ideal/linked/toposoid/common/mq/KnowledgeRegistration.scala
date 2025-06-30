package com.ideal.linked.toposoid.common.mq

import com.ideal.linked.toposoid.common.TransversalState
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeSentenceSet
import play.api.libs.json.{Json, OWrites, Reads}

case class KnowledgeRegistration(documentId:String, propositionId:String, sequentialNumber:Int, transversalState:TransversalState)
object KnowledgeRegistration {
  implicit val jsonWrites: OWrites[KnowledgeRegistration] = Json.writes[KnowledgeRegistration]
  implicit val jsonReads: Reads[KnowledgeRegistration] = Json.reads[KnowledgeRegistration]
}

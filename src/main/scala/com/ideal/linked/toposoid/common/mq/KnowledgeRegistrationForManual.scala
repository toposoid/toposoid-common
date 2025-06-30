package com.ideal.linked.toposoid.common.mq

import com.ideal.linked.toposoid.common.TransversalState
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeSentenceSet
import play.api.libs.json.{Json, OWrites, Reads}

case class KnowledgeRegistrationForManual(knowledgeSentenceSet:KnowledgeSentenceSet, transversalState:TransversalState)
object KnowledgeRegistrationForManual {
  implicit val jsonWrites: OWrites[KnowledgeRegistrationForManual] = Json.writes[KnowledgeRegistrationForManual]
  implicit val jsonReads: Reads[KnowledgeRegistrationForManual] = Json.reads[KnowledgeRegistrationForManual]
}

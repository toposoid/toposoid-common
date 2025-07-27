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

package com.ideal.linked.toposoid.common.mq

import com.ideal.linked.toposoid.common.TransversalState
import com.ideal.linked.toposoid.knowledgebase.regist.model.KnowledgeSentenceSet
import play.api.libs.json.{Json, OWrites, Reads}

case class KnowledgeRegistrationForManual(knowledgeSentenceSet:KnowledgeSentenceSet, transversalState:TransversalState)
object KnowledgeRegistrationForManual {
  implicit val jsonWrites: OWrites[KnowledgeRegistrationForManual] = Json.writes[KnowledgeRegistrationForManual]
  implicit val jsonReads: Reads[KnowledgeRegistrationForManual] = Json.reads[KnowledgeRegistrationForManual]
}

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

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec

class ToposoidUtilsTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll{

  val transversalState = TransversalState(userId="test-user", roleId=0, username="guest", csrfToken="")

  "a json query" should "be handled properly" in {
    val result:String = ToposoidUtils.callComponent("{\"data\":\"テスト\"}", "jsonplaceholder.typicode.com", "80", "posts", transversalState)
    assert(result.contains("\"data\": \"テスト\""))
  }

  /*
  "request to sentence-parser-japanese" should "be handled properly" in {

    val jsonStr:String = """
                           |  {
                           |    "premise": [],
                           |    "claim": [
                           |      {
                           |        "propositionId": "612bf3d6-bdb5-47b9-a3a6-185015c8c414",
                           |        "sentenceId": "4a2994a1-ec7a-438b-a290-0cfb563a5170",
                           |        "knowledge": {
                           |          "sentence": "案ずるより産むが易し。",
                           |          "lang": "ja_JP",
                           |          "extentInfoJson": "{}",
                           |          "isNegativeSentence": false,
                           |          "knowledgeForImages": []
                           |        }
                           |      }
                           |    ]
                           |  }
                           |""".stripMargin


    val result: String = ToposoidUtils.callComponent(jsonStr, "192.168.33.10", "9001", "analyze", transversalState)
    print(result)
  }
  */
}

/*
 * Copyright 2021 Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ideal.linked.toposoid.common

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec

class ToposoidUtilsTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll{

  val transversalState = TransversalState(userId="test-user", roleId=0, username="guest", csrfToken="")

  "a json query" should "be handled properly" in {
    val result:String = ToposoidUtils.callComponent("{\"data\":\"テスト\"}", "jsonplaceholder.typicode.com", "80", "posts", transversalState)
    assert(result.contains("\"data\":\"テスト\""))
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

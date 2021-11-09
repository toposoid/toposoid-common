package com.ideal.linked.toposoid.common

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, DiagrammedAssertions, FlatSpec}

class ToposoidUtilsTest extends FlatSpec with DiagrammedAssertions with BeforeAndAfter with BeforeAndAfterAll{

  "a json query" should "be handled properly" in {
    val result:String = ToposoidUtils.callComponent("{\"data\":\"テスト\"}", "jsonplaceholder.typicode.com", "80", "users")
    assert(result.contains("\"data\":\"テスト\""))
  }

}

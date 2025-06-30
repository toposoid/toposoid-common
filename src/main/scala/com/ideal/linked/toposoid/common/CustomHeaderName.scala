package com.ideal.linked.toposoid.common


sealed abstract class CustomHeaderName(val str: String) {
  override def toString: String = str
}

case object TRANSVERSAL_STATE extends CustomHeaderName("X_TOPOSOID_TRANSVERSAL_STATE")


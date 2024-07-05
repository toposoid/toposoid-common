package com.ideal.linked.toposoid.common

sealed abstract class CustomHeaderName(val name: String) {
  final case object USERNAME extends CustomHeaderName("X_TOPOSOID_USERNAME")
}

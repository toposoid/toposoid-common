package com.ideal.linked.toposoid.common


sealed abstract class SuperiorType(val index: Int)
final case object PROPOSITION_ID extends SuperiorType(0)
final case object DOCUMENT_ID extends SuperiorType(1)
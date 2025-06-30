package com.ideal.linked.toposoid.common


sealed abstract class SentenceType(val index: Int)
final case object PREMISE extends SentenceType(0)
final case object CLAIM extends SentenceType(1)

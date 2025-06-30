package com.ideal.linked.toposoid.common


sealed abstract class FeatureType(val index: Int)
final case object SENTENCE extends FeatureType(0)
final case object IMAGE extends FeatureType(1)
final case object TABLE extends FeatureType(2)
final case object SYNONYM extends FeatureType(3)
final case object PREDICATE_ARGUMENT extends FeatureType(4)
final case object DOCUMENT extends FeatureType(5)
final case object NON_SENTENCE extends FeatureType(6)
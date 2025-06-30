package com.ideal.linked.toposoid.common

sealed abstract class ScopeType(val index: Int)
final case object LOCAL extends ScopeType(0)
final case object SEMIGLOBAL extends ScopeType(1)
final case object GLOBAL extends ScopeType(2)
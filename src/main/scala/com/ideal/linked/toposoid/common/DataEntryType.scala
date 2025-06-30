package com.ideal.linked.toposoid.common

sealed abstract class DataEntryType(val index: Int)
final case object MANUAL extends DataEntryType(0)
final case object BATCH extends DataEntryType(1)


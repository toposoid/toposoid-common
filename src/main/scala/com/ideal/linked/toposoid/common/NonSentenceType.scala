package com.ideal.linked.toposoid.common

sealed abstract class NonSentenceType(val index: Int)
final case object UNSPECIFIED extends NonSentenceType(0)
final case object REFERENCES extends NonSentenceType(1)
final case object TABLE_OF_CONTENTS extends NonSentenceType(2)
final case object HEADLINES extends NonSentenceType(3)
final case object TITLE_OF_TOP_PAGE extends NonSentenceType(4)

/*
 * Copyright (C) 2025  Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ideal.linked.toposoid.common

sealed abstract class NonSentenceType(val index: Int)
final case object UNSPECIFIED extends NonSentenceType(0)
final case object REFERENCES extends NonSentenceType(1)
final case object TABLE_OF_CONTENTS extends NonSentenceType(2)
final case object HEADLINES extends NonSentenceType(3)
final case object TITLE_OF_TOP_PAGE extends NonSentenceType(4)

/*
 * Copyright 2021 Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ideal.linked.toposoid.common

sealed abstract class NonSentenceType(val index: Int)
final case object UNSPECIFIED extends NonSentenceType(0)
final case object REFERENCES extends NonSentenceType(1)
final case object TABLE_OF_CONTENTS extends NonSentenceType(2)
final case object HEADLINES extends NonSentenceType(3)
final case object TITLE_OF_TOP_PAGE extends NonSentenceType(4)

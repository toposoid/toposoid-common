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


sealed abstract class FeatureType(val index: Int)
final case object SENTENCE extends FeatureType(0)
final case object IMAGE extends FeatureType(1)
final case object TABLE extends FeatureType(2)
final case object SYNONYM extends FeatureType(3)
final case object PREDICATE_ARGUMENT extends FeatureType(4)
final case object DOCUMENT extends FeatureType(5)
final case object NON_SENTENCE extends FeatureType(6)
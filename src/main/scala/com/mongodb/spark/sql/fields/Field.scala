/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.spark.sql.fields

import scala.util.Try

protected trait Field[T] {
  require(Try(underlying).isSuccess)
  def underlying: T
  override def equals(that: Any): Boolean = that match {
    case field: Field[_] => underlying.equals(field.underlying)
    case _               => false
  }
  override def hashCode(): Int = underlying.hashCode()
}

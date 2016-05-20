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

package com.mongodb.spark.rdd.partitioner

import org.apache.spark.Partition
import org.bson.BsonDocument

/**
 * An identifier for a partition in a MongoRDD.
 */
private[rdd] case class MongoPartition(index: Int, queryBounds: BsonDocument, locations: Seq[String] = Nil) extends Partition {
  override def hashCode(): Int = super.hashCode()

  override def equals(other: Any): Boolean = other match {
    case p: MongoPartition if index.equals(p.index) && queryBounds.equals(p.queryBounds) && locations.equals(p.locations) => true
    case _ => false
  }
}

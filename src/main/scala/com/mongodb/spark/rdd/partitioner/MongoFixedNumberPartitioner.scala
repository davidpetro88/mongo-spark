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

import scala.util.{Failure, Success, Try}

import org.bson.{BsonDocument, BsonValue}
import com.mongodb.MongoCommandException
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.{Projections, Sorts}
import com.mongodb.spark.MongoConnector
import com.mongodb.spark.config.ReadConfig

/**
 * The Fixed Number Partitioner.
 *
 * Partitions the collection into a fixed number of partitions.
 *
 * $configurationProperties
 *
 *  - [[partitionKeyProperty partitionkey]], the field to partition the collection by. The field should be indexed and contain unique values.
 *     Defaults to `_id`.
 *  - [[numberOfPartitionsProperty numberofpartitions]], the maximum number of partitions to create. Defaults to `64`.
 *
 *
 * '''Note:''' This can be a expensive operation as it creates 1 cursor for every partition.
 *
 * @since 1.0
 */
class MongoFixedNumberPartitioner extends MongoPartitioner {

  private implicit object BsonValueOrdering extends BsonValueOrdering
  private val DefaultPartitionKey = "_id"
  private val DefaultNumberOfPartitions = "64"

  /**
   * The partition key property
   */
  val partitionKeyProperty = "partitionKey".toLowerCase()

  /**
   * The number of partitions property
   */
  val numberOfPartitionsProperty = "numberOfPartitions".toLowerCase()

  /**
   * Calculate the Partitions
   *
   * @param connector  the MongoConnector
   * @param readConfig the [[com.mongodb.spark.config.ReadConfig]]
   * @return the partitions
   */
  override def partitions(connector: MongoConnector, readConfig: ReadConfig, pipeline: Array[BsonDocument]): Array[MongoPartition] = {

    Try(PartitionerHelper.collStats(connector, readConfig)) match {
      case Success(results) =>
        val partitionKey = readConfig.partitionerOptions.getOrElse(partitionKeyProperty, DefaultPartitionKey)
        val maxNumberOfPartitions = readConfig.partitionerOptions.getOrElse(numberOfPartitionsProperty, DefaultNumberOfPartitions).toInt
        val count = results.getNumber("count").longValue()
        val numberOfPartitions = if (count < maxNumberOfPartitions) count else maxNumberOfPartitions
        val numDocumentsPerPartition = math.floor(count / numberOfPartitions).toInt

        if (count == numberOfPartitions) {
          logWarning("Inefficient partitioning, creating a partition per document. Decrease the `numberOfPartitions` property.")
        }

        val skipValues = (0 to numberOfPartitions.toInt).map(i => i * numDocumentsPerPartition)
        val rightHandBoundaries = skipValues.map(i => connector.withCollectionDo(readConfig, { coll: MongoCollection[BsonDocument] =>
          i >= count match {
            case true => None
            case false =>
              Option(coll.find()
                .skip(i)
                .limit(-1)
                .projection(Projections.include(partitionKey))
                .sort(Sorts.ascending(partitionKey))
                .first())
                .map(_.get(partitionKey))
          }
        })).collect({ case Some(boundary) => boundary })

        PartitionerHelper.createPartitions(partitionKey, rightHandBoundaries.sorted, PartitionerHelper.locations(connector))
      case Failure(ex: MongoCommandException) if ex.getErrorMessage.endsWith("not found.") =>
        logInfo(s"Could not find collection (${readConfig.collectionName}), using a single partition")
        MongoSinglePartitioner.partitions(connector, readConfig, pipeline)
      case Failure(e) =>
        logWarning(s"Could not get collection statistics. Server errmsg: ${e.getMessage}")
        throw e
    }
  }

}

case object MongoFixedNumberPartitioner extends MongoFixedNumberPartitioner

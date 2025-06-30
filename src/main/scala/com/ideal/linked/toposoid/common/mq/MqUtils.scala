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

package com.ideal.linked.toposoid.common.mq

import akka.actor.ActorSystem
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import com.ideal.linked.common.DeploymentConverter.conf
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.net.URI

object MqUtils {
  def publishMessage(json: String, mqHost: String, mqPort: String, queueName: String, region: Region = Region.AP_NORTHEAST_1): Unit = {

    implicit val actorSystem = ActorSystem("example")

    val testEndPoint = "http://" + mqHost + ":" + mqPort
    val queueUrl = testEndPoint + "/" + queueName

    val sqs = SqsAsyncClient
      .builder()
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create(conf.getString("TOPOSOID_MQ_ACCESS_KEY"), conf.getString("TOPOSOID_MQ_SECRET_KEY")) // (1)
        )
      )
      .endpointOverride(URI.create(testEndPoint)) // (2)
      .region(region)
      .httpClient(AkkaHttpClient.builder()
        .withActorSystem(actorSystem).build())
      .build()

    sqs.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(queueUrl)
        .messageGroupId("x")
        .messageBody(json)
        .build()
    ).join()

  }

}

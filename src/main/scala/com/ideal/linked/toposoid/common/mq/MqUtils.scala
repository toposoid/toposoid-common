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

package com.example.auction.item.impl

import com.example.auction.bidding.api.BiddingService
import com.example.auction.item.api.ItemService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.internal.client.CircuitBreakerMetricsProviderImpl
import com.softwaremill.macwire._
import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents
import play.api.Environment
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

trait ItemComponents extends LagomServerComponents
  with CassandraPersistenceComponents {

  implicit def executionContext: ExecutionContext
  def environment: Environment

  override lazy val lagomServer = serverFor[ItemService](wire[ItemServiceImpl])
  lazy val itemRepository = wire[ItemRepository]
  lazy val jsonSerializerRegistry = ItemSerializerRegistry

  persistentEntityRegistry.register(wire[ItemEntity])
  readSide.register(wire[ItemEventProcessor])
}

abstract class ItemApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with ItemComponents
  with AhcWSComponents
  with LagomKafkaComponents {

  lazy val biddingService = serviceClient.implement[BiddingService]

  wire[BiddingServiceSubscriber]
}

class ItemApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new ItemApplication(context) with LagomDevModeComponents

  override def load(context: LagomApplicationContext): LagomApplication =
    new ItemApplication(context) with ConductRApplicationComponents {

    override lazy val circuitBreakerMetricsProvider = new CircuitBreakerMetricsProviderImpl(actorSystem)
  }
  
  override def describeServices = List(
    readDescriptor[ItemService]
  )
}

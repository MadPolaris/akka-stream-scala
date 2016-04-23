package com.eventbank.demo

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Future

/**
  * Created by Barry on 4/22/16.
  */
object ImportMembershipDemo extends App {

  implicit val sys = ActorSystem("ImportMembershipDemo")
  implicit val mat = ActorMaterializer()
  import sys.dispatcher

  type Row = List[String]
  type MembershipId = Int
  type ContactId = Int
  type MemberId = Int

  val f1 = Flow[Row].mapAsync(1)(createMembership)
  val f2 = Flow[Row].mapAsync(1)(createContact)
  val f3 = Flow[Row].map(row => row)
  val f4 = Flow[(MembershipId, ContactId, Row)].map(theTuple => theTuple)
  val f5 = Flow[(MembershipId, ContactId, Row)].mapAsync(1)(bindMembershipMember)
  val f6 = Flow[(MembershipId, ContactId, Row)].mapAsync(1)(createMemberCustomFields)
  val f7 = Flow[(MembershipId, ContactId, Row)].mapAsync(1)(createMemberApplicationForm)
  val f8 = Flow[(MembershipId, MemberId, MemberId)].map(_._1)

  private val f = 1 to 10
  private val rows = f.map { i => List[String](s"${i}") }
  val in = Source(rows)
  val out = Sink.foreach(println)

  in.log("before-map")
    .withAttributes(Attributes.logLevels(onElement = Logging.WarningLevel))
    .via(broadcastAndThenZip(f1, f2, f3))
    .via(f4)
    .via(broadcastAndThenZip(f5, f6, f7))
    .via(f8)
    .fold(List[MembershipId]())(concat)
    .to(out).run()

  private def broadcastAndThenZip[Input, Output1, Output2, Output3](g1: Graph[FlowShape[Input, Output1], Any],
                                                                    g2: Graph[FlowShape[Input, Output2], Any],
                                                                    g3: Graph[FlowShape[Input, Output3], Any]) =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val bcast = builder.add(Broadcast[Input](3))
      val zip3 = builder.add(ZipWith[Output1, Output2, Output3, (Output1, Output2, Output3)]((a, b, c) => (a, b, c)))
      bcast ~> g1 ~> zip3.in0
      bcast ~> g2 ~> zip3.in1
      bcast ~> g3 ~> zip3.in2
      FlowShape(bcast.in, zip3.out)
    }

  private def concat(membershipIds: List[MembershipId], id: MembershipId) = id :: membershipIds

  def createMembership: (Row) => Future[MembershipId] = impl("createMembership")

  def createContact: (Row) => Future[ContactId] = impl("createContact")

  def bindMembershipMember: ((MembershipId, ContactId, Row)) => Future[MembershipId] = impl2("bindMembershipMember")

  def createMemberCustomFields: ((MembershipId, ContactId, Row)) => Future[MemberId] = impl2("createMemberCustomFields")

  def createMemberApplicationForm: ((MembershipId, ContactId, Row)) => Future[MemberId] = impl2("createMemberApplicationForm")

  private def impl(log: String): (Row) => Future[MemberId] = (row) => {
    println(log)
    Future(row.head.toInt)
  }

  private def impl2(log: String): ((MembershipId, ContactId, Row)) => Future[MembershipId] = (tuple3) => {
    println(log)
    Future(tuple3._1)
  }


}

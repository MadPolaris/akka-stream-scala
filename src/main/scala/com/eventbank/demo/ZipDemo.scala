package com.eventbank.demo

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout

import scala.concurrent.duration._

/**
 * Created by Barry on 4/20/16.
 */
object ZipDemo extends App {

  implicit val sys = ActorSystem("TheDemo")
  implicit val mat = ActorMaterializer()
  implicit val timeout = Timeout(3.seconds)

  val numbers = Source(List(1, 2, 3))
  val strings = Source(List("a", "b", "c", "a", "b", "c", "a", "b", "c"))
  //

  //  numbers.zip(strings).runForeach(println)

  val sourceGraph = GraphDSL.create(numbers, strings)((n, s) => (n, s)) { implicit builder =>
    (n, s) =>
      import GraphDSL.Implicits._
      val zip = builder.add(Zip[Int, String]())

      n ~> zip.in0
      s ~> zip.in1

      SourceShape(zip.out)
  }

  val graph = GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._
    val zip = builder.add(Zip[Int, String]())

    numbers ~> zip.in0
    strings ~> zip.in1

    SourceShape(zip.out)
  }

  val flowGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val zip = builder.add(Zip[Int, String]())
    strings ~> zip.in1
    FlowShape(zip.in0, zip.out)
  }

  val flowGraph2 = GraphDSL.create(strings) { implicit builder =>
    r =>
      import GraphDSL.Implicits._
      val zip = builder.add(Zip[Int, String]())
      r ~> zip.in1
      FlowShape(zip.in0, zip.out)
  }

  numbers
    .log("before-map")
    .withAttributes(Attributes.logLevels(onElement = Logging.WarningLevel))
    .via(flowGraph).to(Sink.foreach(println)).run()

}

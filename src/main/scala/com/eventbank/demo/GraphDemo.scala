package com.eventbank.demo

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._

/**
  * Created by Barry on 4/22/16.
  */
object GraphDemo {

  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

    import GraphDSL.Implicits._
    val in = Source(1 to 10)
    val out = Sink.ignore
    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))
    val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
                bcast ~> f4 ~> merge
    ClosedShape

  })
}
package com.eventbank.demo

import akka.actor.Actor.Receive
import akka.actor.{ ActorSystem, Props }
import akka.stream.ActorMaterializer
import akka.stream.actor.{ ActorSubscriber, OneByOneRequestStrategy, RequestStrategy }
import akka.stream.scaladsl.{ Sink, Source }

/**
 * Created by Barry on 4/23/16.
 */
object ActorSubscriberDemo extends App {

  implicit val sys = ActorSystem("ActorSubscriberDemo")
  implicit val mat = ActorMaterializer()

  class ActorDemo extends ActorSubscriber {

    override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

    override def receive: Receive = {
      case e => println(e)
    }
  }

  //val subscriber = ActorSubscriber(sys.actorOf(Props(new ActorDemo)))

  Source(1 to 100)
    .map(_.toString)
    .filter(_.length == 2)
    .drop(2)
    .groupBy(10, _.last)
  //.to(Sink.actorSubscriber(Props(new ActorDemo)))
  //    .run()

}

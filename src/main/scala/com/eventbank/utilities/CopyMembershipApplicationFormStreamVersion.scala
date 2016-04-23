package com.eventbank.utilities

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.eventbank.utilities.CopyMembershipApplicationForm._
import reactivemongo.bson.BSONDocument

// Reactive streams imports
import akka.stream.scaladsl.Source

// ReactiveMongo extensions
import reactivemongo.akkastreams.cursorProducer

import scala.concurrent.Future

/**
  * Created by Barry on 4/19/16.
  */
object CopyMembershipApplicationFormStreamVersion extends App {

  implicit val sys = ActorSystem("StreamVersion")

  import sys.dispatcher

  implicit val materializer = ActorMaterializer()

  //  val query = BSONDocument("organizationId" -> 1046, "type" -> "MembershipType:Company")
  val query = BSONDocument()

  srcFormColl.find(query).cursor[BSONDocument]().source()
    .mapAsync(4)(processForm)
    .mapAsync(4)(extractItemsFromForm)
    .runWith(Sink.foreach(processItems))
    .onComplete {
      case _ => System.exit(0)
    }


  /*
    Source.fromFuture(srcFormColl.find(query).cursor[BSONDocument]().collect[List]())
      .mapConcat(identity)
      .mapAsync(4)(processForm)
      .mapAsync(4)(extractItems)
      .runWith(Sink.foreach(processItems))
      .onComplete {
        case _ => System.exit(0)
      }
  */
  def processItems: (List[BSONDocument]) => Unit = {
    documents => documents.foreach(targetItemColl.insert(_))
  }

  def processForm: (BSONDocument) => Future[BSONDocument] = { document =>
    targetFormColl.insert(document).map {
      case _ => document
    }
  }
}

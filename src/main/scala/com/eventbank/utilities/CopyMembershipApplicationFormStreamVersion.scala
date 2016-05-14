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

  val query = BSONDocument("organizationId" -> 517, "type" -> "MembershipType:Individual")

  srcFormColl.find(query).cursor[BSONDocument]().source()
    .mapAsync(4)(processForm)
    .mapAsync(4)(extractItemsFromForm)
    .runWith(Sink.foreach(processItems))
    .onComplete {
      case t =>
        println("completed" + t)
        System.exit(0)
    }

  private def processForm: (BSONDocument) => Future[BSONDocument] = { document =>
    targetFormColl.insert(document).map {
      case _ => document
    }
  }

  private def extractItemsFromForm(document: BSONDocument) = {
    val items = document.getAs[List[BSONDocument]]("items")
    val itemIds = items.get.map { item => item.getAs[String]("$id").get }
    val itemQuery = BSONDocument("_id" -> BSONDocument("$in" -> itemIds))
    srcItemColl.find(itemQuery)
      .cursor[BSONDocument]
      .collect[List]()
  }

  private def processItems: (List[BSONDocument]) => Unit = documents => documents.foreach {
    targetItemColl.insert(_)
  }


}

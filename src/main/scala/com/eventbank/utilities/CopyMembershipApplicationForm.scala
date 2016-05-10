package com.eventbank.utilities

import com.typesafe.config.ConfigFactory
import reactivemongo.api._
import reactivemongo.bson.BSONDocument
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.core.nodeset.Authenticate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by Barry on 4/19/16.
  */
object CopyMembershipApplicationForm {
  val driver = new MongoDriver

  val srcConfig = ConfigFactory.load().getConfig("mongodbSrc")
  val targetConfig = ConfigFactory.load().getConfig("mongodbTarget")

  val srcServer = srcConfig.getStringList("servers").toArray(new Array[String](0)).toSeq
  val srcUsername = srcConfig.getConfig("credentials").getString("username")
  val srcPassword = srcConfig.getConfig("credentials").getString("password")
  val srcDb = srcConfig.getString("db")
  val srcCredentials = List(Authenticate(srcDb, srcUsername, srcPassword)).toSeq

  //  val srcConnection = driver.connection(srcServer)
  def srcConnection = driver.connection(srcServer, authentications = srcCredentials)

  val targetServer = targetConfig.getStringList("servers").toArray(new Array[String](0)).toSeq
  val targetUsername = targetConfig.getConfig("credentials").getString("username")
  val targetPassword = targetConfig.getConfig("credentials").getString("password")
  val targetDb = targetConfig.getString("db")
  val targetCredentials = List(Authenticate(targetDb, targetUsername, targetPassword)).toSeq

  def targetConnection = driver.connection(targetServer)

  //  val targetConnection = driver.connection(targetServer, authentications = targetCredentials)

  private val eventRegistrationForm = "event_registration_form"
  private val eventRegistrationFormItem = "event_registration_form_item"
  val srcFormColl = connect(srcConnection, srcDb, eventRegistrationForm)
  val srcItemColl = connect(srcConnection, srcDb, eventRegistrationFormItem)
  val targetFormColl = connect(targetConnection, targetDb, eventRegistrationForm)
  val targetItemColl = connect(targetConnection, targetDb, eventRegistrationFormItem)

  def connect(connection: MongoConnection = srcConnection, dbName: String = "eb_mongo", collection: String = eventRegistrationForm) = {
    connection(dbName).apply[BSONCollection](collection)
  }

  def copyApplicationForm2(srcOrganizationId: Int, targetOrganizationId: Int) = {
    val query = BSONDocument("organizationId" -> srcOrganizationId, "type" -> "MembershipType:Company")
    srcFormColl.find(query).one.map {
      for (document <- _) {

        val itemEntities = extractItemsFromForm(document)

        targetFormColl.insert(document).andThen {
          case Success(writeResult) => itemEntities.map { itemDocuments =>
            itemDocuments.foreach(itemDocument => {
              println(itemDocument)
              targetItemColl.insert(itemDocument)
            })
          }
          case Failure(ex) => println(ex)
        }
      }
    }
  }

  def extractItemsFromForm(document: BSONDocument) = {
    val items = document.getAs[List[BSONDocument]]("items")
    val itemIds = items.get.map { item => item.getAs[String]("$id").get }
    val itemQuery = BSONDocument("_id" -> BSONDocument("$in" -> itemIds))
    srcItemColl.find(itemQuery)
      .cursor[BSONDocument]
      .collect[List]()
  }

  copyApplicationForm2(1046, 1046)

}

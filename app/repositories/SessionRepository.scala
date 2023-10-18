/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Indexes.ascending
import models.PbikSession
import org.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{FindOneAndReplaceOptions, IndexModel, IndexOptions}
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionRepository @Inject() (config: Configuration, mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[PbikSession](
      collectionName = config.get[String]("appName"),
      mongoComponent = mongo,
      domainFormat   = PbikSession.pbikSessionFormats,
      indexes = Seq(
        IndexModel(
          ascending("lastUpdated"),
          IndexOptions()
            .name("userAnswersExpiry")
            .expireAfter(config.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS)
        )
      )
    ) {

  def upsert(session: PbikSession): Future[PbikSession] =
    collection
      .findOneAndReplace(byId(session.sessionId), session, FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER))
      .toFuture()

  def get(id: String): Future[Option[PbikSession]] =
    collection
      .find(byId(id))
      .headOption()

  def remove(id: String): Future[Boolean] =
    collection.deleteOne(byId(id)).toFuture().map(_.wasAcknowledged())

  private def byId(value: String): Bson =
    equal("id", value)
}

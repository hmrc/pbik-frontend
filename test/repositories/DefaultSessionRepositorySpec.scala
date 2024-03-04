/*
 * Copyright 2024 HM Revenue & Customs
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

import config.PbikAppConfig
import controllers.FakePBIKApplication
import models._
import org.mongodb.scala.model.Filters
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global

class DefaultSessionRepositorySpec
    extends AnyWordSpecLike
    with Matchers
    with FakePBIKApplication
    with OptionValues
    with BeforeAndAfterEach {

  private val appConfig: PbikAppConfig       = injected[PbikAppConfig]
  private val mongoComponent: MongoComponent = injected[MongoComponent]
  private val sessionRepository              = new DefaultSessionRepository(appConfig, mongoComponent)
  private val pbikSession: PbikSession       = PbikSession(sessionId)

  private def assertSession(session: PbikSession, result: PbikSession): Unit = {
    result.lastUpdated.isAfter(session.lastUpdated) shouldBe true

    session.copy(lastUpdated = result.lastUpdated) shouldBe result
  }

  override def beforeEach(): Unit =
    await(sessionRepository.collection.deleteMany(Filters.empty()).toFuture())

  "DefaultSessionRepositorySpec" when {
    "upsert" should {
      "insert data when there is no data" in {
        val result = await(sessionRepository.upsert(pbikSession))

        assertSession(pbikSession, result)
      }

      "update data when there is already data" in {
        val updatedSession = pbikSession.copy(registrations =
          Some(RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true)), None))
        )

        val result = await(sessionRepository.upsert(pbikSession))
        assertSession(pbikSession, result)

        val result2 = await(sessionRepository.upsert(updatedSession))
        assertSession(updatedSession, result2)
      }
    }

    "get" should {
      "return the session when there is data" in {
        val result = await(sessionRepository.upsert(pbikSession))
        assertSession(pbikSession, result)

        val result2 = await(sessionRepository.get(sessionId)).value
        assertSession(pbikSession, result2)
      }

      "return None when there is no data" in {
        val result = await(sessionRepository.get(sessionId))
        result shouldBe None
      }
    }

    "remove" should {
      "return true when there is data" in {
        val result = await(sessionRepository.upsert(pbikSession))
        assertSession(pbikSession, result)

        val result2 = await(sessionRepository.remove(sessionId))
        result2 shouldBe true
      }

      "return false when there is no data" in {
        val id     = "non existing session id"
        val result = await(sessionRepository.get(id))
        result shouldBe None

        val result2 = await(sessionRepository.remove(id))
        result2 shouldBe false
      }
    }
  }
}

/*
 * Copyright 2026 HM Revenue & Customs
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

import base.FakePBIKApplication
import config.PbikAppConfig
import models._
import models.v1.IabdType
import org.mongodb.scala.model.Filters
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.SingleObservableFuture

import scala.concurrent.ExecutionContext.Implicits.global

class DefaultSessionRepositorySpec extends FakePBIKApplication {

  private val appConfig: PbikAppConfig       = injected[PbikAppConfig]
  private val mongoComponent: MongoComponent = injected[MongoComponent]
  private val sessionRepository              = new DefaultSessionRepository(appConfig, mongoComponent)
  private val pbikSession: PbikSession       = PbikSession(sessionId)

  private def assertSession(session: PbikSession, result: PbikSession): Unit = {
    result.lastUpdated.isAfter(session.lastUpdated) mustBe true

    session.copy(lastUpdated = result.lastUpdated) mustBe result
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
          Some(
            RegistrationList(
              None,
              List(RegistrationItem(IabdType.CarBenefit, active = true, enabled = true)),
              None
            )
          )
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
        result mustBe None
      }
    }

    "remove" should {
      "return true when there is data" in {
        val result = await(sessionRepository.upsert(pbikSession))
        assertSession(pbikSession, result)

        val result2 = await(sessionRepository.remove(sessionId))
        result2 mustBe true
      }

      "return false when there is no data" in {
        val id     = "non existing session id"
        val result = await(sessionRepository.get(id))
        result mustBe None

        val result2 = await(sessionRepository.remove(id))
        result2 mustBe false
      }
    }
  }
}

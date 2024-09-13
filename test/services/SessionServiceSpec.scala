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

package services

import controllers.FakePBIKApplication
import models._
import models.cache.MissingSessionIdException
import models.v1.exclusion.{PbikExclusionPerson, PbikExclusions}
import models.v1.trace.TracePerson
import models.v1.{IabdType, PbikAction}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.Helpers.await
import repositories.SessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class SessionServiceSpec extends AnyWordSpecLike with Matchers with FakePBIKApplication {

  private val timeout: FiniteDuration                  = 5.seconds
  implicit val hc: HeaderCarrier                       = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  private val mockSessionRepository: SessionRepository = mock(classOf[SessionRepository])
  private val sessionService: SessionService           = new SessionService(mockSessionRepository)
  private val pbikSession: PbikSession                 = PbikSession(sessionId)

  "The SessionService" should {

    "cache a list of registrations" in {
      val regList = RegistrationList(
        None,
        List(RegistrationItem(IabdType.CarBenefit.id.toString, active = true, enabled = true)),
        None
      )
      val session = pbikSession.copy(registrations = Some(regList))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result  = await(sessionService.storeRegistrationList(regList))(timeout)

      result shouldBe session
    }

    "cache a bik to remove" in {
      val bikRemoved = RegistrationItem(IabdType.CarBenefit.id.toString, active = true, enabled = true)
      val session    = pbikSession.copy(bikRemoved = Some(bikRemoved))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result     = await(sessionService.storeBikRemoved(bikRemoved))(timeout)

      result shouldBe session
    }

    "cache a list of matches" in {
      val listOfMatches =
        List(
          TracePerson("AB123456C", "John", Some("A"), "Doe", None, 22)
        )
      val session       = pbikSession.copy(listOfMatches = Some(listOfMatches))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result        = await(sessionService.storeListOfMatches(listOfMatches))(timeout)

      result shouldBe session
    }

    "cache an EiLPerson" in {
      val eiLPerson =
        PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", "12345", 22)
      val session   = pbikSession.copy(eiLPerson = Some(eiLPerson))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result    = await(sessionService.storeEiLPerson(eiLPerson))(timeout)

      result shouldBe session
    }

    "cache the current exclusions" in {
      val currentExclusions = PbikExclusions(
        List(
          PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", "12345", 22)
        )
      )
      val session           = pbikSession.copy(currentExclusions = Some(currentExclusions))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result            = await(sessionService.storeCurrentExclusions(currentExclusions))(timeout)

      result shouldBe session
    }

    "return MissingSessionIdException when caching the current exclusions with session ID absent in header carrier" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val currentExclusions = PbikExclusions(
        List(
          PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", "12345", 22)
        )
      )

      intercept[MissingSessionIdException] {
        await(sessionService.storeCurrentExclusions(currentExclusions))(timeout)
      }.getMessage shouldBe "Unable to retrieve session ID"
    }

    "cache the current year registered biks" in {
      val cyRegisteredBiks = List(Bik(IabdType.CarBenefit.id.toString, PbikAction.ReinstatePayrolledBenefitInKind.id))
      val session          = pbikSession.copy(cyRegisteredBiks = Some(cyRegisteredBiks))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result           = await(sessionService.storeCYRegisteredBiks(cyRegisteredBiks))(timeout)

      result shouldBe session
    }

    "cache the next year registered biks" in {
      val nyRegisteredBiks = List(Bik(IabdType.CarBenefit.id.toString, PbikAction.ReinstatePayrolledBenefitInKind.id))
      val session          = pbikSession.copy(nyRegisteredBiks = Some(nyRegisteredBiks))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result           = await(sessionService.storeNYRegisteredBiks(nyRegisteredBiks))(timeout)

      result shouldBe session
    }

    "be able to fetch the pbik session" in {
      val pbikSession = PbikSession(
        sessionId,
        None,
        Some(RegistrationItem(IabdType.CarBenefit.id.toString, active = true, enabled = true)),
        None,
        None,
        None,
        None,
        None
      )
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(pbikSession)))
      val result      = await(sessionService.fetchPbikSession())(timeout)

      result shouldBe Some(pbikSession)
    }

    "not be able to fetch the pbik session and return None" when {
      "session ID is present in header carrier and no session is found" in {
        when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))

        val result = await(sessionService.fetchPbikSession())(timeout)

        result shouldBe None
      }

      "session ID is absent in header carrier" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val result = await(sessionService.fetchPbikSession())(timeout)

        result shouldBe None
      }
    }

    "be able to reset all and return true" when {
      "session ID is present in header carrier and data exists" in {
        when(mockSessionRepository.remove(any())).thenReturn(Future.successful(true))

        val result = await(sessionService.resetAll())(timeout)

        result shouldBe true
      }
    }

    "not be able to reset all and return false" when {
      "session ID is present in header carrier and data does not exist" in {
        when(mockSessionRepository.remove(any())).thenReturn(Future.successful(false))

        val result = await(sessionService.resetAll())(timeout)

        result shouldBe false
      }

      "session ID is absent in header carrier" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val result = await(sessionService.resetAll())(timeout)

        result shouldBe false
      }
    }
  }
}

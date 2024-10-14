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

import base.FakePBIKApplication
import models._
import models.cache.MissingSessionIdException
import models.v1.exclusion.{PbikExclusionPerson, PbikExclusions, SelectedExclusionToRemove}
import models.v1.trace.{TracePersonListResponse, TracePersonResponse}
import models.v1.{BenefitInKindWithCount, BenefitListResponse, IabdType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.Helpers.await
import repositories.SessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class SessionServiceSpec extends FakePBIKApplication {

  private val timeout: FiniteDuration                  = 5.seconds
  implicit val hc: HeaderCarrier                       = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  private val mockSessionRepository: SessionRepository = mock(classOf[SessionRepository])
  private val sessionService: SessionService           = new SessionService(mockSessionRepository)
  private val pbikSession: PbikSession                 = PbikSession(sessionId)

  "The SessionService" should {

    "cache a list of registrations" in {
      val regList = RegistrationList(
        None,
        List(RegistrationItem(IabdType.CarBenefit, active = true, enabled = true)),
        None
      )
      val session = pbikSession.copy(registrations = Some(regList))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result  = await(sessionService.storeRegistrationList(regList))(timeout)

      result mustBe session
    }

    "cache a bik to remove" in {
      val bikRemoved = RegistrationItem(IabdType.CarBenefit, active = true, enabled = true)
      val session    = pbikSession.copy(bikRemoved = Some(bikRemoved))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result     = await(sessionService.storeBikRemoved(bikRemoved))(timeout)

      result mustBe session
    }

    "cache a list of matches" in {
      val listOfMatches = TracePersonListResponse(
        0,
        List(
          TracePersonResponse("AB123456C", "John", Some("A"), "Doe", None, 22)
        )
      )
      val session       = pbikSession.copy(listOfMatches = Some(listOfMatches))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result        = await(sessionService.storeListOfMatches(listOfMatches))(timeout)

      result mustBe session
    }

    "cache an EiLPerson" in {
      val eiLPerson =
        SelectedExclusionToRemove(1, PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", Some("12345"), 22))
      val session   = pbikSession.copy(eiLPerson = Some(eiLPerson))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result    = await(sessionService.storeEiLPerson(eiLPerson))(timeout)

      result mustBe session
    }

    "cache the current exclusions" in {
      val currentExclusions = PbikExclusions(
        0,
        Some(
          List(
            PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", Some("12345"), 22)
          )
        )
      )
      val session           = pbikSession.copy(currentExclusions = Some(currentExclusions))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result            = await(sessionService.storeCurrentExclusions(currentExclusions))(timeout)

      result mustBe session
    }

    "return MissingSessionIdException when caching the current exclusions with session ID absent in header carrier" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val currentExclusions = PbikExclusions(
        0,
        Some(
          List(
            PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", Some("12345"), 22)
          )
        )
      )

      intercept[MissingSessionIdException] {
        await(sessionService.storeCurrentExclusions(currentExclusions))(timeout)
      }.getMessage mustBe "Unable to retrieve session ID"
    }

    "cache the current year registered biks" in {
      val cyRegisteredBiks =
        List(BenefitInKindWithCount(IabdType.CarBenefit, 3))
      val session          = pbikSession.copy(cyRegisteredBiks = Some(BenefitListResponse(Some(cyRegisteredBiks), 99)))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result           = await(sessionService.storeCYRegisteredBiks(BenefitListResponse(Some(cyRegisteredBiks), 99)))(timeout)

      result mustBe session
    }

    "cache the next year registered biks" in {
      val nyRegisteredBiks =
        List(BenefitInKindWithCount(IabdType.CarBenefit, 5))
      val session          = pbikSession.copy(nyRegisteredBiks = Some(BenefitListResponse(Some(nyRegisteredBiks), 99)))
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSessionRepository.upsert(any())).thenReturn(Future.successful(session))
      val result           = await(sessionService.storeNYRegisteredBiks(BenefitListResponse(Some(nyRegisteredBiks), 99)))(timeout)

      result mustBe session
    }

    "be able to fetch the pbik session" in {
      val pbikSession = PbikSession(
        sessionId,
        None,
        Some(RegistrationItem(IabdType.CarBenefit, active = true, enabled = true)),
        None,
        None,
        None,
        None,
        None
      )
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(pbikSession)))
      val result      = await(sessionService.fetchPbikSession())(timeout)

      result mustBe Some(pbikSession)
    }

    "not be able to fetch the pbik session and return None" when {
      "session ID is present in header carrier and no session is found" in {
        when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))

        val result = await(sessionService.fetchPbikSession())(timeout)

        result mustBe None
      }

      "session ID is absent in header carrier" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val result = await(sessionService.fetchPbikSession())(timeout)

        result mustBe None
      }
    }

    "be able to reset all and return true" when {
      "session ID is present in header carrier and data exists" in {
        when(mockSessionRepository.remove(any())).thenReturn(Future.successful(true))

        val result = await(sessionService.resetAll())(timeout)

        result mustBe true
      }
    }

    "not be able to reset all and return false" when {
      "session ID is present in header carrier and data does not exist" in {
        when(mockSessionRepository.remove(any())).thenReturn(Future.successful(false))

        val result = await(sessionService.resetAll())(timeout)

        result mustBe false
      }

      "session ID is absent in header carrier" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val result = await(sessionService.resetAll())(timeout)

        result mustBe false
      }
    }
  }
}

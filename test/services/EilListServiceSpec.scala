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

import connectors.PbikConnector
import controllers.FakePBIKApplication
import models.{AuthenticatedRequest, EiLPerson, EmpRef, UserName}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.collection.immutable
import scala.concurrent.Future

class EilListServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with FakePBIKApplication {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[PbikConnector].toInstance(mock(classOf[PbikConnector])))
    .build()

  val mockEiLListService: EiLListService = {

    val els = app.injector.instanceOf[EiLListService]

    when(
      els.tierConnector.getAllExcludedEiLPersonForBik(any[String], any[EmpRef], any[Int])(
        any[HeaderCarrier]
      )
    ).thenReturn(Future.successful(List.empty[EiLPerson]))

    els
  }

  "When calling the EILService it" should {
    "return an empty list" in {
      val year                                                                        = 2015
      val eilService: EiLListService                                                  = mockEiLListService
      implicit val hc: HeaderCarrier                                                  = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
      implicit val request: FakeRequest[AnyContentAsEmpty.type]                       = mockRequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
        AuthenticatedRequest(
          EmpRef("taxOfficeNumber", "taxOfficeReference"),
          UserName(Name(None, None)),
          request,
          None
        )
      val result                                                                      = await(eilService.currentYearEiL("services", year))
      result.size shouldBe 0
    }

    "return a subset of List(EiL) search results - already excluded" in {
      val eilService                          = mockEiLListService
      val eiL1                                = new EiLPerson("QQ123456", "Humpty", None, "Dumpty", Some("123"), Some("01/01/1980"), None, None)
      val eiL2                                = new EiLPerson("QQ123457", "Humpty", None, "Dumpty", Some("789"), Some("01/01/1980"), None, None)
      val searchResultsEiL: List[EiLPerson]   = List(eiL1, eiL2)
      val alreadyExcludedEiL: List[EiLPerson] = List(eiL1)

      val result: immutable.Seq[EiLPerson] =
        eilService.searchResultsRemoveAlreadyExcluded(alreadyExcludedEiL, searchResultsEiL)
      result.size shouldBe 1
      result.head shouldBe eiL2
    }
  }

}

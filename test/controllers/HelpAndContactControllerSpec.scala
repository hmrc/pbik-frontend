/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import config.{AppConfig, PbikAppConfig}
import controllers.actions.{AuthAction, NoSessionCheckAction}
import models.TaxYearRange
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.HttpEntity.Strict
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.HelpAndContactSubmissionService
import support.{StubHelpAndContactSubmissionService, TestAuthUser}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.{FormMappings, _}

import scala.concurrent.Future

class HelpAndContactControllerSpec extends PlaySpec with FakePBIKApplication with TestAuthUser {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind[AppConfig].toInstance(mock(classOf[PbikAppConfig])))
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[HelpAndContactSubmissionService].to(classOf[StubHelpAndContactSubmissionService]))
    .build()

  val formMappings: FormMappings = app.injector.instanceOf[FormMappings]

  val controller = app.injector.instanceOf[HelpAndContactController]
  implicit val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]

  def YEAR_RANGE: TaxYearRange = taxDateUtils.getTaxYearRange()

  val helpForm = Form(
    tuple(
      a1 = "contact-name"     -> text,
      a2 = "contact-email"    -> text,
      a3 = "contact-comments" -> text
    )
  )

  "When using help / contact hmrc, the HelpAndContactController" should {
    "get 500 response when there is an empty form" in {
      val mockHelpController = controller
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val result = await(mockHelpController.submitContactHmrcForm(request))
      result.header.status must be(INTERNAL_SERVER_ERROR) // 500
      result.body.asInstanceOf[Strict].data.utf8String must include("")
    }

    "be able to submit the contact form successfully" in {
      val mockHelpController = controller
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(helpForm.data.toSeq: _*)
      val result = await(mockHelpController.submitContactHmrcForm(mockRequestForm))
      result.header.status must be(SEE_OTHER) // 303

      val nextUrl = redirectLocation(Future.successful(result)).getOrElse("")

      nextUrl mustBe "/payrollbik/help-and-contact-confirmed"
    }
  }
}

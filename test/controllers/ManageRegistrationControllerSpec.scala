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

import config.AppConfig
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import controllers.registration.ManageRegistrationController
import models._
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.http.HttpEntity.Strict
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionService
import support.{TestAuthUser, TestCYEnabledConfig}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils._

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps

class ManageRegistrationControllerSpec extends PlaySpec with TestAuthUser with FakePBIKApplication {

  val messagesApi = app.injector.instanceOf[MessagesApi]

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[AppConfig].toInstance(TestCYEnabledConfig))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .overrides(bind[SessionService].toInstance(mock(classOf[SessionService])))
    .build()

  implicit val lang: Lang = Lang("en-GB")
  val formMappings: FormMappings = app.injector.instanceOf[FormMappings]
  implicit val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))
  lazy val CYRegistrationItems: List[RegistrationItem] =
    List.tabulate(21)(n => RegistrationItem("" + (n + 1), active = true, enabled = true))
  val timeoutValue: FiniteDuration = 15 seconds

  def YEAR_RANGE: TaxYearRange = taxDateUtils.getTaxYearRange()

  class FakeResponse extends HttpResponse {
    override def status = 200

    override def allHeaders: Map[String, Seq[String]] = Map()

    override def body: String = "empty"
  }

  val registrationController: ManageRegistrationController = {

    val r = app.injector.instanceOf[ManageRegistrationController]

    when(
      app.injector
        .instanceOf[HmrcTierConnector]
        .genericGetCall[List[Bik]](any[String], argEq(""), any[EmpRef], argEq(YEAR_RANGE.cy))(
          any[HeaderCarrier],
          any[json.Format[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      app.injector
        .instanceOf[HmrcTierConnector]
        .genericGetCall[List[Bik]](
          any[String],
          argEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
          argEq(EmpRef.empty),
          argEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[json.Format[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 10
      }))

    when(
      app.injector
        .instanceOf[HmrcTierConnector]
        .genericGetCall[List[Bik]](
          any[String],
          argEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
          argEq(EmpRef.empty),
          argEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[json.Format[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 10
      }))

    when(
      app.injector
        .instanceOf[HmrcTierConnector]
        .genericGetCall[List[Bik]](
          any[String],
          argEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
          argEq(EmpRef.empty),
          argEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[json.Format[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 10
      }))

    when(
      app.injector
        .instanceOf[HmrcTierConnector]
        .genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], argEq(2020))(
          any[HeaderCarrier],
          any[json.Format[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 5
    }))

    when(
      app.injector
        .instanceOf[HmrcTierConnector]
        .genericPostCall(
          any[String],
          argEq(app.injector.instanceOf[URIInformation].updateBenefitTypesPath),
          any[EmpRef],
          any[Int],
          any)(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]]))
      .thenReturn(Future.successful(new FakeResponse()))

    when(
      app.injector
        .instanceOf[HmrcTierConnector]
        .genericGetCall[List[Bik]](
          any[String],
          argEq(app.injector.instanceOf[URIInformation].getRegisteredPath),
          any[EmpRef],
          any[Int])(any[HeaderCarrier], any[json.Format[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) >= 15
      }))

    when(
      app.injector
        .instanceOf[SessionService]
        .cacheRegistrationList(any[RegistrationList])(any[HeaderCarrier]))
      .thenReturn(Future.successful(None))

    r
  }

  "When loading the currentTaxYearOnPageLoad, an authorised user" should {
    "be directed to cy page with list of biks" in {
      val title = messagesApi("AddBenefits.Heading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.currentTaxYearOnPageLoad(mockrequest))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(messagesApi("BenefitInKind.label.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(messagesApi("BenefitInKind.label.3"))
    }
  }

  "When loading the nextTaxYearAddOnPageLoad, an authorised user" should {
    "be directed to cy + 1 page with list of biks" in {
      val title = messagesApi("AddBenefits.Heading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.nextTaxYearAddOnPageLoad(mockrequest))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(messagesApi("BenefitInKind.label.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(messagesApi("BenefitInKind.label.3"))
    }
  }

  "When loading checkYourAnswersAddCurrentTaxYear, an authorised user" should {
    "be taken to the check your answers page when the form is correctly filled" in {
      val mockRegistrationList = RegistrationList(
        None,
        List(RegistrationItem("31", active = true, enabled = true)),
        Some(BinaryRadioButtonWithDesc("software", None)))
      val form = formMappings.objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest
        .withFormUrlEncodedBody(form.data.toSeq: _*)

      val result = registrationController.checkYourAnswersAddCurrentTaxYear().apply(mockRequestForm)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be("/payrollbik/cy/add-benefit-expense")

    }

    "be shown the form with errors if not filled in correctly" in {
      val mockRegistrationList = RegistrationList(None, List.empty[RegistrationItem], None)
      val form = formMappings.objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest
        .withFormUrlEncodedBody(form.data.toSeq: _*)

      val result = registrationController.checkYourAnswersAddCurrentTaxYear().apply(mockRequestForm)

      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(messagesApi("AddBenefits.noselection.error"))
    }
  }

  "When loading showCheckYourAnswersAddCurrentTaxYear, an authorised user" should {
    "be shown the check your answers screen if correct data is present in the cache" in {
      when(registrationController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                Some(RegistrationList(active = List(RegistrationItem("30", true, true)))),
                None,
                None,
                None,
                None,
                None,
                None
              ))))
      val result = registrationController.showCheckYourAnswersAddCurrentTaxYear().apply(mockrequest)

      status(result) must be(OK)
      contentAsString(result) must include(messagesApi("AddBenefits.Confirm.Multiple.Title"))
      contentAsString(result) must include(messagesApi("BenefitInKind.label.30"))
    }
  }

  "When loading checkYourAnswersAddNextTaxYear, and authenticated user" should {
    "be taken to the check your answers screen if the form is filled in correctly" in {
      val mockRegistrationList = RegistrationList(
        None,
        List(RegistrationItem("31", active = true, enabled = true)),
        Some(BinaryRadioButtonWithDesc("software", None)))
      val form = formMappings.objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest
        .withFormUrlEncodedBody(form.data.toSeq: _*)

      val result = registrationController.checkYourAnswersAddNextTaxYear().apply(mockRequestForm)

      status(result) must be(SEE_OTHER)
      redirectLocation(result).get must be("/payrollbik/cy1/add-benefit-expense")
    }

    "be shown the form with errors if not filled in correctly" in {
      val mockRegistrationList = RegistrationList(None, List.empty[RegistrationItem], None)
      val form = formMappings.objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest
        .withFormUrlEncodedBody(form.data.toSeq: _*)

      val result = registrationController.checkYourAnswersAddNextTaxYear().apply(mockRequestForm)

      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(messagesApi("AddBenefits.noselection.error"))
    }
  }

  "When loading showCheckYourAnswersNextCurrentTaxYear, an authorised user" should {
    "be shown the check your answers screen if correct data is present in the cache" in {
      when(registrationController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                Some(RegistrationList(active = List(RegistrationItem("30", true, true)))),
                None,
                None,
                None,
                None,
                None,
                None
              ))))
      val result = registrationController.showCheckYourAnswersAddNextTaxYear().apply(mockrequest)

      status(result) must be(OK)
      contentAsString(result) must include(messagesApi("BenefitInKind.label.30"))
    }
  }

  "When loading checkYourAnswersRemoveNextTaxYear, an authorised user" should {
    "be directed cy + 1 confirmation page to remove bik" in {
      when(registrationController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                Some(RegistrationList(active = List(RegistrationItem("30", true, true)))),
                None,
                None,
                None,
                None,
                None,
                None
              ))))
      val title = messagesApi("RemoveBenefits.Confirm.Title").substring(0, 10)
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.checkYourAnswersRemoveNextTaxYear("car").apply(mockrequest))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When loading the updateCurrentYearRegisteredBenefitTypes, an authorised user" should {
    "persist their changes and be redirected to the what next page" in {
      val mockRegistrationList = RegistrationList(
        None,
        List(RegistrationItem("31", active = true, enabled = true)),
        Some(BinaryRadioButtonWithDesc("software", None)))
      when(registrationController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                Some(mockRegistrationList),
                Some(RegistrationItem("31", true, true)),
                None,
                None,
                None,
                None,
                None
              ))))
      val form = formMappings.objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest
        .withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = registrationController.updateCurrentYearRegisteredBenefitTypes.apply(mockRequestForm)
      (scala.concurrent.ExecutionContext.Implicits.global)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some("/payrollbik/cy/add-benefit-expense-confirmed"))
    }
  }

  "When loading the addNextYearRegisteredBenefitTypes, an unauthorised user" should {
    "be directed to the login page" in {
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.addNextYearRegisteredBenefitTypes.apply(noSessionIdRequest))(timeout)
      result.header.status must be(UNAUTHORIZED)
      result.body.asInstanceOf[Strict].data.utf8String must include(
        "Request was not authenticated user should be redirected")
    }
  }

  "When loading the removeNextYearRegisteredBenefitTypes, an unauthorised user" should {
    "be directed to the login page" in {
      implicit val timeout: FiniteDuration = timeoutValue
      val result =
        await(registrationController.removeNextYearRegisteredBenefitTypes("").apply(noSessionIdRequest))(timeout)
      result.header.status must be(UNAUTHORIZED)
      result.body.asInstanceOf[Strict].data.utf8String must include(
        "Request was not authenticated user should be redirected")
    }
  }

  "When a user removes a benefit" should {
    "selecting 'software' should redirect to what next page" in {
      val mockRegistrationList = RegistrationList(
        None,
        List(RegistrationItem("31", active = true, enabled = true)),
        Some(BinaryRadioButtonWithDesc("software", None)))
      when(registrationController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Some(
              PbikSession(
                Some(mockRegistrationList),
                Some(RegistrationItem("31", true, true)),
                None,
                None,
                None,
                None,
                None
              ))))
      val form = formMappings.removalReasonForm.fill(BinaryRadioButtonWithDesc("software", None))
      val mockRequestForm = mockrequest
        .withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = registrationController.removeNextYearRegisteredBenefitTypes("").apply(mockRequestForm)
      (scala.concurrent.ExecutionContext.Implicits.global)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some("/payrollbik/cy1/remove-benefit-expense-confirmed"))
    }
  }

  "selecting 'guidance' should redirect to what next page" in {
    val mockRegistrationList = RegistrationList(
      None,
      List(RegistrationItem("31", active = true, enabled = true)),
      Some(BinaryRadioButtonWithDesc("guidance", None)))
    when(registrationController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
      .thenReturn(
        Future.successful(
          Some(
            PbikSession(
              Some(mockRegistrationList),
              Some(RegistrationItem("31", true, true)),
              None,
              None,
              None,
              None,
              None
            ))))
    val form = formMappings.removalReasonForm.fill(BinaryRadioButtonWithDesc("guidance", None))
    val mockRequestForm = mockrequest
      .withFormUrlEncodedBody(form.data.toSeq: _*)
    val result = registrationController.removeNextYearRegisteredBenefitTypes("").apply(mockRequestForm)
    (scala.concurrent.ExecutionContext.Implicits.global)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some("/payrollbik/cy1/remove-benefit-expense-confirmed"))
  }

  "selecting 'not-clear' should redirect to what next page" in {
    val mockRegistrationList = RegistrationList(
      None,
      List(RegistrationItem("31", active = true, enabled = true)),
      Some(BinaryRadioButtonWithDesc("not-clear", None)))
    when(registrationController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
      .thenReturn(
        Future.successful(
          Some(
            PbikSession(
              Some(mockRegistrationList),
              Some(RegistrationItem("31", true, true)),
              None,
              None,
              None,
              None,
              None
            ))))
    val form = formMappings.removalReasonForm.fill(BinaryRadioButtonWithDesc("not-clear", None))
    val mockRequestForm = mockrequest
      .withFormUrlEncodedBody(form.data.toSeq: _*)
    val result = registrationController.removeNextYearRegisteredBenefitTypes("").apply(mockRequestForm)
    (scala.concurrent.ExecutionContext.Implicits.global)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some("/payrollbik/cy1/remove-benefit-expense-confirmed"))
  }

  "selecting 'not-offering' should redirect to what next page" in {
    val mockRegistrationList = RegistrationList(
      None,
      List(RegistrationItem("31", active = true, enabled = true)),
      Some(BinaryRadioButtonWithDesc("not-offering", None)))
    when(registrationController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
      .thenReturn(
        Future.successful(
          Some(
            PbikSession(
              Some(mockRegistrationList),
              Some(RegistrationItem("31", true, true)),
              None,
              None,
              None,
              None,
              None
            ))))
    val form = formMappings.removalReasonForm.fill(BinaryRadioButtonWithDesc("not-offering", None))
    val mockRequestForm = mockrequest
      .withFormUrlEncodedBody(form.data.toSeq: _*)
    val result = registrationController.removeNextYearRegisteredBenefitTypes("").apply(mockRequestForm)
    (scala.concurrent.ExecutionContext.Implicits.global)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some("/payrollbik/cy1/remove-benefit-expense-confirmed"))
  }

  "selecting 'other' & providing 'info' should redirect to what next page" in {
    val mockRegistrationList = RegistrationList(
      None,
      List(RegistrationItem("31", active = true, enabled = true)),
      Some(BinaryRadioButtonWithDesc("other", Some("Here's our other info")))
    )
    when(registrationController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
      .thenReturn(
        Future.successful(
          Some(
            PbikSession(
              Some(mockRegistrationList),
              Some(RegistrationItem("31", true, true)),
              None,
              None,
              None,
              None,
              None
            ))))
    val form = formMappings.removalReasonForm.fill(BinaryRadioButtonWithDesc("other", Some("Here's our other info")))
    val mockRequestForm = mockrequest
      .withFormUrlEncodedBody(form.data.toSeq: _*)
    val result = registrationController.removeNextYearRegisteredBenefitTypes("").apply(mockRequestForm)
    (scala.concurrent.ExecutionContext.Implicits.global)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some("/payrollbik/cy1/remove-benefit-expense-confirmed"))
  }

  "selecting 'other' & not providing 'info' should return to the form page with an error" in {
    val form = formMappings.removalReasonForm.fill(BinaryRadioButtonWithDesc("other", None))
    val mockRequestForm = mockrequest
      .withFormUrlEncodedBody(form.data.toSeq: _*)
    val errorMsg = messagesApi("RemoveBenefits.reason.other.required")
    val result = registrationController.removeNextYearRegisteredBenefitTypes("").apply(mockRequestForm)
    (scala.concurrent.ExecutionContext.Implicits.global)
    status(result) must be(OK) // 200
    contentAsString(result) must include(errorMsg)
  }

  "selecting no reason should return to the same page with an error" in {
    val mockRegistrationList = RegistrationList(
      None,
      List(RegistrationItem("31", active = true, enabled = true), RegistrationItem("8", active = true, enabled = true)),
      None)
    val bikList = List(Bik("8", 10))
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
    implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
      AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
    val errorMsg = messagesApi("RemoveBenefits.reason.no.selection")
    val result = registrationController.removeBenefitReasonValidation(mockRegistrationList, 2017, bikList, bikList)
    status(result) must be(OK)
    contentAsString(result) must include(errorMsg)
  }
}

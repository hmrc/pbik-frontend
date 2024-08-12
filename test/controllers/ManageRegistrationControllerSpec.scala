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

package controllers

import connectors.PbikConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import controllers.registration.ManageRegistrationController
import models._
import models.v1.{BenefitTypes, IabdType, PbikStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionService
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class ManageRegistrationControllerSpec extends PlaySpec with FakePBIKApplication {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder()
    .configure(configMap)
    .overrides(bind[AuthAction].to(classOf[TestAuthActionOrganisation]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[PbikConnector].toInstance(mock(classOf[PbikConnector])))
    .overrides(bind[SessionService].toInstance(mock(classOf[SessionService])))
    .build()

  implicit val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]

  private val messages: Messages                                   = app.injector.instanceOf[MessagesApi].preferred(Seq(lang))
  private val formMappings: FormMappings                           = app.injector.instanceOf[FormMappings]
  private val (beginIndex, endIndex): (Int, Int)                   = (0, 10)
  private val (iabdType, iabdString): (String, String)             = ("31", "car")
  private val registrationController: ManageRegistrationController =
    app.injector.instanceOf[ManageRegistrationController]

  val responseHeaders: Map[String, String] = HeaderTags.createResponseHeaders()

  private val cyBenefitTypes: BenefitTypes = BenefitTypes(IabdType.values)
  private val cyBiks: Set[Bik]             =
    cyBenefitTypes.benefitTypes.map(x => Bik(x.id.toString, PbikStatus.ValidPayrollingBenefitInKind.id))

  when(app.injector.instanceOf[PbikConnector].getAllAvailableBiks(any[Int])(any[HeaderCarrier]))
    .thenReturn(Future.successful(Right(cyBenefitTypes)))

  when(
    app.injector
      .instanceOf[PbikConnector]
      .updateOrganisationsRegisteredBiks(
        any[Int],
        any
      )(any[HeaderCarrier], any[AuthenticatedRequest[_]])
  ).thenReturn(Future.successful(OK))

  when(
    app.injector
      .instanceOf[PbikConnector]
      .getRegisteredBiks(
        any[EmpRef],
        any[Int]
      )(any[HeaderCarrier], any[AuthenticatedRequest[_]])
  ).thenReturn(
    Future.successful(
      BikResponse(
        responseHeaders,
        cyBiks.filter { x: Bik =>
          Integer.parseInt(x.iabdType) >= 15
        }
      )
    )
  )

  when(
    app.injector
      .instanceOf[SessionService]
      .storeRegistrationList(any[RegistrationList])(any[HeaderCarrier])
  ).thenReturn(Future.successful(PbikSession(sessionId)))

  "ManageRegistrationController" when {
    "loading the currentTaxYearOnPageLoad, an authorised user" should {
      "be directed to cy page with list of biks" in {
        val title  = messages("AddBenefits.Heading")
        val result = registrationController.currentTaxYearOnPageLoad()(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(title)
        contentAsString(result) must include(messages("BenefitInKind.label.8"))
      }
    }

    "loading the nextTaxYearAddOnPageLoad, an authorised user" should {
      "be directed to cy + 1 page with list of biks" in {
        val title  = messages("AddBenefits.Heading")
        val result = registrationController.nextTaxYearAddOnPageLoad()(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(title)
        contentAsString(result) must include(messages("BenefitInKind.label.8"))
      }
    }

    "loading checkYourAnswersAddCurrentTaxYear, an authorised user" should {
      "be taken to the check your answers page when the form is correctly filled" in {
        val mockRegistrationList = RegistrationList(
          None,
          List(RegistrationItem(iabdType, active = true, enabled = true)),
          Some(BinaryRadioButtonWithDesc("software", None))
        )
        val form                 = formMappings.objSelectedForm.fill(mockRegistrationList)
        val mockRequestForm      = mockRequest
          .withFormUrlEncodedBody(form.data.toSeq: _*)

        val result = registrationController.checkYourAnswersAddCurrentTaxYear()(mockRequestForm)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/payrollbik/cy/check-the-benefits")
      }

      "be shown the form with errors if not filled in correctly" in {
        val mockRegistrationList = RegistrationList(None, List.empty[RegistrationItem], None)
        val form                 = formMappings.objSelectedForm.fill(mockRegistrationList)
        val mockRequestForm      = mockRequest
          .withFormUrlEncodedBody(form.data.toSeq: _*)

        val result = registrationController.checkYourAnswersAddCurrentTaxYear()(mockRequestForm)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(messages("AddBenefits.noselection.error"))
      }
    }

    "loading showCheckYourAnswersAddCurrentTaxYear, an authorised user" should {
      "be shown the check your answers screen if correct data is present in the cache" in {
        when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(RegistrationList(active = List(RegistrationItem(iabdType, active = true, enabled = true)))),
                  None,
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val result = registrationController.showCheckYourAnswersAddCurrentTaxYear()(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(messages("AddBenefits.Confirm.Multiple.Title"))
        contentAsString(result) must include(messages(s"BenefitInKind.label.$iabdType"))
      }
    }

    "loading checkYourAnswersAddNextTaxYear, and authenticated user" should {
      "be taken to the check your answers screen if the form is filled in correctly" in {
        val mockRegistrationList = RegistrationList(
          None,
          List(RegistrationItem(iabdType, active = true, enabled = true)),
          Some(BinaryRadioButtonWithDesc("software", None))
        )
        val form                 = formMappings.objSelectedForm.fill(mockRegistrationList)
        val mockRequestForm      = mockRequest
          .withFormUrlEncodedBody(form.data.toSeq: _*)

        val result = registrationController.checkYourAnswersAddNextTaxYear()(mockRequestForm)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe "/payrollbik/cy1/check-the-benefits"
      }

      "be shown the form with errors if not filled in correctly" in {
        val mockRegistrationList = RegistrationList(None, List.empty[RegistrationItem], None)
        val form                 = formMappings.objSelectedForm.fill(mockRegistrationList)
        val mockRequestForm      = mockRequest
          .withFormUrlEncodedBody(form.data.toSeq: _*)

        val result = registrationController.checkYourAnswersAddNextTaxYear()(mockRequestForm)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(messages("AddBenefits.noselection.error"))
      }
    }

    "loading showCheckYourAnswersNextCurrentTaxYear, an authorised user" should {
      "be shown the check your answers screen if correct data is present in the cache" in {
        when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(RegistrationList(active = List(RegistrationItem(iabdType, active = true, enabled = true)))),
                  None,
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val result = registrationController.showCheckYourAnswersAddNextTaxYear()(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(messages(s"BenefitInKind.label.$iabdType"))
      }
    }

    "loading checkYourAnswersRemoveNextTaxYear, an authorised user" should {
      "be directed cy + 1 confirmation page to remove bik" in {
        when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(RegistrationList(active = List(RegistrationItem(iabdType, active = true, enabled = true)))),
                  None,
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val title  = messages("RemoveBenefits.reason.Title").substring(beginIndex, endIndex)
        val result = registrationController.checkYourAnswersRemoveNextTaxYear(iabdString)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(title)
      }
    }

    "loading the updateCurrentYearRegisteredBenefitTypes, an authorised user" should {
      "persist their changes and be redirected to the what next page" in {
        val mockRegistrationList = RegistrationList(
          None,
          List(RegistrationItem(iabdType, active = true, enabled = true)),
          Some(BinaryRadioButtonWithDesc("software", None))
        )
        when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(mockRegistrationList),
                  Some(RegistrationItem(iabdType, active = true, enabled = true)),
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val form                 = formMappings.objSelectedForm.fill(mockRegistrationList)
        val mockRequestForm      = mockRequest
          .withFormUrlEncodedBody(form.data.toSeq: _*)
        val result               = registrationController.updateCurrentYearRegisteredBenefitTypes()(mockRequestForm)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/payrollbik/cy/registration-complete")
      }
    }

    "loading the addNextYearRegisteredBenefitTypes" should {
      "persist changes of an authorised user and redirect this user to the what next page" in {
        val mockRegistrationList = RegistrationList(
          None,
          List(RegistrationItem(iabdType, active = true, enabled = true)),
          Some(BinaryRadioButtonWithDesc("software", None))
        )
        when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(mockRegistrationList),
                  Some(RegistrationItem(iabdType, active = true, enabled = true)),
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val form                 = formMappings.objSelectedForm.fill(mockRegistrationList)
        val mockRequestForm      = mockRequest.withFormUrlEncodedBody(form.data.toSeq: _*)
        val result               = registrationController.addNextYearRegisteredBenefitTypes()(mockRequestForm)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/payrollbik/cy1/registration-complete")
      }

      "direct an unauthorised user to the login page" in {
        val result = registrationController.addNextYearRegisteredBenefitTypes()(noSessionIdRequest)

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) must include(
          "Request was not authenticated user should be redirected"
        )
      }
    }

    "loading the removeNextYearRegisteredBenefitTypes, an unauthorised user" should {
      "be directed to the login page" in {
        val result = registrationController.removeNextYearRegisteredBenefitTypes("")(noSessionIdRequest)

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) must include(
          "Request was not authenticated user should be redirected"
        )
      }
    }

    "a user removes a benefit" should {
      "redirect to what next page" when {
        def test(selectionValue: String): Unit =
          s"$selectionValue is selected" in {
            val mockRegistrationList = RegistrationList(
              None,
              List(RegistrationItem(iabdType, active = true, enabled = true)),
              Some(BinaryRadioButtonWithDesc(selectionValue, None))
            )
            when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
              .thenReturn(
                Future.successful(
                  Some(
                    PbikSession(
                      sessionId,
                      Some(mockRegistrationList),
                      Some(RegistrationItem(iabdType, active = true, enabled = true)),
                      None,
                      None,
                      None,
                      None,
                      None
                    )
                  )
                )
              )
            val form                 = formMappings.removalReasonForm.fill(BinaryRadioButtonWithDesc(selectionValue, None))
            val mockRequestForm      = mockRequest
              .withFormUrlEncodedBody(form.data.toSeq: _*)
            val result               = registrationController.removeNextYearRegisteredBenefitTypes(iabdString).apply(mockRequestForm)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(s"/payrollbik/cy1/$iabdString/declare-remove-benefit-expense")
          }

        Seq("software", "guidance", "not-clear", "not-offering").foreach(test)
      }

      "redirect to why-remove-benefit-expense page when other is selected" in {
        val mockRegistrationList = RegistrationList(
          None,
          List(RegistrationItem(iabdType, active = true, enabled = true)),
          Some(BinaryRadioButtonWithDesc("other", Some("Here's our other info")))
        )
        when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(mockRegistrationList),
                  Some(RegistrationItem(iabdType, active = true, enabled = true)),
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val form                 = formMappings.removalReasonForm.fill(BinaryRadioButtonWithDesc("other", None))
        val mockRequestForm      = mockRequest
          .withFormUrlEncodedBody(form.data.toSeq: _*)
        val result               = registrationController.removeNextYearRegisteredBenefitTypes("").apply(mockRequestForm)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/cy1/$iabdString/why-remove-benefit-expense")
      }
    }

    "selecting nothing should return to the same page with an error" in {
      val (bikStatus, year)                                               = (10, 2017)
      val mockRegistrationList                                            = RegistrationList(
        None,
        List(
          RegistrationItem(iabdType, active = true, enabled = true),
          RegistrationItem(IabdType.EmployerProvidedServices.id.toString, active = true, enabled = true)
        ),
        None
      )
      val bikList                                                         = List(Bik("8", bikStatus))
      implicit val request: FakeRequest[AnyContentAsEmpty.type]           = mockRequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(
          EmpRef("taxOfficeNumber", "taxOfficeReference"),
          UserName(Name(None, None)),
          request,
          None
        )
      val errorMsg                                                        = messages("RemoveBenefits.reason.no.selection")

      val result =
        registrationController.removeBenefitReasonValidation(mockRegistrationList, year, bikList, bikList, "")

      status(result) mustBe OK
      contentAsString(result) must include(errorMsg)

      val resultSelection = registrationController.updateBiksFutureAction(year, bikList, additive = false)

      status(resultSelection) mustBe OK
      contentAsString(resultSelection) must include(errorMsg)
    }

    "loading the why-remove-benefit-expense, an unauthorised user" should {
      "be directed to the login page" in {
        val result = registrationController.showRemoveBenefitOtherReason("")(noSessionIdRequest)

        status(result) mustBe UNAUTHORIZED
        contentAsString(result) must include(
          "Request was not authenticated user should be redirected"
        )
      }
    }

    "loading why-remove-benefit-expense, an authorised user" should {
      "be directed cy + 1 confirmation page to remove bik for other reason" in {
        when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(RegistrationList(active = List(RegistrationItem(iabdType, active = true, enabled = true)))),
                  None,
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )

        val title  = messages("RemoveBenefits.other.title").substring(beginIndex, endIndex)
        val result = registrationController.showRemoveBenefitOtherReason(iabdString)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(title)
      }

      "be redirected to what next page when a valid other reason is provided" in {
        val otherReason          = "Here's our other info"
        val mockRegistrationList = RegistrationList(
          None,
          List(RegistrationItem(iabdType, active = true, enabled = true)),
          Some(BinaryRadioButtonWithDesc("other", Some(otherReason)))
        )
        when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(mockRegistrationList),
                  Some(RegistrationItem(iabdType, active = true, enabled = true)),
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )
        val form                 = formMappings.removalOtherReasonForm.fill(OtherReason(otherReason))
        val mockRequestForm      = mockRequest
          .withFormUrlEncodedBody(form.data.toSeq: _*)
        val result               = registrationController.submitRemoveBenefitOtherReason(iabdString)(mockRequestForm)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"/payrollbik/cy1/$iabdString/declare-remove-benefit-expense")
      }

      "return to the same page with an error when other reason is not provided" in {
        val errorMsg        = messages("RemoveBenefits.other.error.required")
        val form            = formMappings.removalOtherReasonForm.fill(OtherReason(""))
        val mockRequestForm = mockRequest
          .withFormUrlEncodedBody(form.data.toSeq: _*)
        val result          = registrationController.submitRemoveBenefitOtherReason(iabdString)(mockRequestForm)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(errorMsg)
      }

      "return to the same page with an error when other reason of more than 100 chars is provided" in {
        val errorMsg        = messages("RemoveBenefits.other.error.length")
        val reason          =
          "this is a test other reason to remove the benefits, if user wants to remove the benefits from payroll"
        val form            = formMappings.removalOtherReasonForm.fill(OtherReason(reason))
        val mockRequestForm = mockRequest
          .withFormUrlEncodedBody(form.data.toSeq: _*)
        val result          = registrationController.submitRemoveBenefitOtherReason(iabdString)(mockRequestForm)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(errorMsg)
      }
    }

    "loading showConfirmRemoveNextTaxYear, an authorised user" should {
      "be directed cy + 1 confirmation page to confirm remove bik" in {
        when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                PbikSession(
                  sessionId,
                  Some(RegistrationList(active = List(RegistrationItem(iabdType, active = true, enabled = true)))),
                  None,
                  None,
                  None,
                  None,
                  None,
                  None
                )
              )
            )
          )

        val title  = messages("RemoveBenefits.confirm.heading")
        val result = registrationController.showConfirmRemoveNextTaxYear(iabdString)(mockRequest)

        status(result) mustBe OK
        contentAsString(result) must include(title)
      }
    }

    "loading submitConfirmRemoveNextTaxYear, an authorised user" should {
      Seq(
        BinaryRadioButtonWithDesc("other", Some("Here's our other info")),
        BinaryRadioButtonWithDesc("software", None)
      ).foreach { reason =>
        s"be directed cy + 1 removal confirmation page for reason $reason" in {
          when(registrationController.sessionService.fetchPbikSession()(any[HeaderCarrier]))
            .thenReturn(
              Future.successful(
                Some(
                  PbikSession(
                    sessionId,
                    Some(
                      RegistrationList(
                        active = List(RegistrationItem(iabdType, active = true, enabled = true)),
                        reason = Some(reason)
                      )
                    ),
                    None,
                    None,
                    None,
                    None,
                    None,
                    None
                  )
                )
              )
            )

          val result = registrationController.submitConfirmRemoveNextTaxYear(iabdString)(mockRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/payrollbik/cy1/car/benefit-removed")
        }
      }
    }
  }
}

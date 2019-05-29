/*
 * Copyright 2019 HM Revenue & Customs
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

import config.{AppConfig, LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import controllers.registration.ManageRegistrationController
import javax.inject.Inject
import models._
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.http.HttpEntity.Strict
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.mvc.{Result, _}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration, Environment}
import play.twirl.api.HtmlFormat
import services.{BikListService, RegistrationService}
import support.{TestAuthUser, TestCYEnabledConfig}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{ControllersReferenceData, _}

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class ManageRegistrationControllerSpec extends PlaySpec with FormMappings
  with TestAuthUser with FakePBIKApplication {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[AppConfig].toInstance(TestCYEnabledConfig))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .build()


  implicit val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))
  lazy val CYRegistrationItems: List[RegistrationItem] = List.tabulate(21)(n=> RegistrationItem("" + (n + 1), active = true, enabled = true))
  val timeoutValue: FiniteDuration = 15 seconds

  def YEAR_RANGE:TaxYearRange = taxDateUtils.getTaxYearRange()

  class StubBikListService @Inject()(val pbikAppConfig: AppConfig,
                                     tierConnector: HmrcTierConnector,
                                     runModeConfiguration: Configuration,
                                     controllersReferenceData: ControllersReferenceData,
                                     environment: Environment,
                                     uriInformation: URIInformation,
                                      bikListService : BikListService
                                    ){

    lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))

     def currentYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]):
        Future[(Map[String, String], List[Bik])] = {

      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik => Integer.parseInt(x.iabdType) <= 10 }))
    }

     def nextYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]):
        Future[(Map[String, String], List[Bik])] = {

      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik => Integer.parseInt(x.iabdType) > 10 }))
    }

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(""),
      any[EmpRef], mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      EmpRef("",""), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      EmpRef("",""), mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      EmpRef("",""), mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(2020))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 5
    }))

    when(tierConnector.genericPostCall(anyString, mockEq(uriInformation.updateBenefitTypesPath),
      any[EmpRef], anyInt, any)(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getRegisteredPath),
      any[EmpRef], anyInt)(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) >= 15
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))


    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(""),
      any[EmpRef], mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      EmpRef("",""), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getBenefitTypesPath),
      EmpRef("",""), mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericPostCall(anyString, mockEq(uriInformation.updateBenefitTypesPath),
      any[EmpRef], anyInt, any)(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericGetCall[List[Bik]](anyString, mockEq(uriInformation.getRegisteredPath),
      any[EmpRef], anyInt)(any[HeaderCarrier], any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) >= 15
    }))

  }

  class FakeResponse extends HttpResponse {
    override def status = 200
  }

  class StubbedRegistrationService @Inject()(tierConnector: HmrcTierConnector,
                                             bikListService: BikListService,
                                             runModeConfiguration: Configuration,
                                             environment: Environment,
                                             taxDateUtils: TaxDateUtils,
                                             controllersReferenceData: ControllersReferenceData,
                                             uRIInformation: URIInformation,
                                             pbikAppConfig: PbikAppConfig,
                                             context: PbikContext,
                                             externalURLs: ExternalUrls,
                                             localFormPartialRetriever: LocalFormPartialRetriever) extends RegistrationService(

    tierConnector,
    bikListService,
    runModeConfiguration,
    environment,
    taxDateUtils,
    controllersReferenceData,
    uRIInformation)(
    pbikAppConfig,
    context,
    externalURLs,
    localFormPartialRetriever
 ) {
    val dateRange: TaxYearRange = taxDateUtils.getTaxYearRange()

    val registeredListOption = List.empty[Bik]
    val allRegisteredListOption: List[Bik] = CYCache
    val mockRegistrationItemList = List.empty[RegistrationItem]
    val mockFormRegistrationList: Form[RegistrationList] = objSelectedForm.fill(RegistrationList(None, CYRegistrationItems))

    implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()


    override def generateViewForBikRegistrationSelection(year: Int, cachingSuffix: String,
                                                generateViewBasedOnFormItems: (Form[RegistrationList],
                                                  List[RegistrationItem], List[Bik], List[Int], List[Int], Option[Int]) => HtmlFormat.Appendable)
                                               (implicit hc:HeaderCarrier, request: AuthenticatedRequest[AnyContent]):
    Future[Result] = {
      year match {
        case dateRange.cyminus1 => {
          Future.successful(Ok(
            views.html.registration.currentTaxYear(
              bikForm = mockFormRegistrationList,
              taxYearRange = dateRange,
              previouslySelectedBenefits = mockRegistrationItemList,
              registeredBiks = allRegisteredListOption,
              nonLegislationBiks = List(0),
              decommissionedBiks = pbikAppConfig.biksNotSupported,
              biksAvailableCount=Some(17),
              empRef = request.empRef
            )(implicitly, context,
              implicitly,
              externalURLs,
              pbikAppConfig,
              localFormPartialRetriever)
          ))
        }
        case _ => {
          Future.successful(Ok(
            views.html.registration.nextTaxYear(
              mockFormRegistrationList,
              additive = true,
              dateRange,
              mockRegistrationItemList,
              registeredListOption,
              nonLegislationBiks = List(0),
              pbikAppConfig.biksNotSupported,
              biksAvailableCount=Some(17),
              empRef = request.empRef)(implicitly, context,
              implicitly,
              externalURLs,
              pbikAppConfig,
              localFormPartialRetriever)
          ))
        }
      }
    }

  }

  val registrationController: ManageRegistrationController = {


    val r = app.injector.instanceOf[ManageRegistrationController]

    implicit val defaultPatience: ScalaFutures.PatienceConfig =
      PatienceConfig(timeout = Span(7, Seconds), interval = Span(600, Millis))

    def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Future[AuditResult] = {
      Future.successful(AuditResult.Success)
    }



    implicit lazy val mr: FakeRequest[AnyContentAsEmpty.type] = mockrequest

    //val tierConnector: HmrcTierConnector = mock[HmrcTierConnector]

    val dateRange: TaxYearRange = taxDateUtils.getTaxYearRange()

    when(app.injector.instanceOf[HmrcTierConnector].genericGetCall[List[Bik]](anyString, mockEq(""),
      any[EmpRef], mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(app.injector.instanceOf[HmrcTierConnector].genericGetCall[List[Bik]](anyString, mockEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
      mockEq(EmpRef.empty), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(app.injector.instanceOf[HmrcTierConnector].genericGetCall[List[Bik]](anyString, mockEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
      mockEq(EmpRef.empty), mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(app.injector.instanceOf[HmrcTierConnector].genericGetCall[List[Bik]](anyString, mockEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
      mockEq(EmpRef.empty), mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(app.injector.instanceOf[HmrcTierConnector].genericGetCall[List[Bik]](anyString, anyString,
      any[EmpRef], mockEq(2020))(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 5
    }))

    when(app.injector.instanceOf[HmrcTierConnector].genericPostCall(anyString, mockEq(app.injector.instanceOf[URIInformation].updateBenefitTypesPath),
      any[EmpRef], anyInt, any)(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    when(app.injector.instanceOf[HmrcTierConnector].genericGetCall[List[Bik]](anyString, mockEq(app.injector.instanceOf[URIInformation].getRegisteredPath),
      any[EmpRef], anyInt)(any[HeaderCarrier], any[Request[_]],
      any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) >= 15
    }))

    r
  }

  "When loading the next tax years on-remove data, the RegistrationController" should {
      "state the status is ok" in {
        implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
        implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
          EmpRef("taxOfficeNumber", "taxOfficeReference"),
          UserName(Name(None, None)),
          request)
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
        implicit val timeout: FiniteDuration = timeoutValue
        val result = await(registrationController.loadNextTaxYearOnRemoveData)(timeout)
        result.header.status must be(OK) // 200
      }
    }

  "The Registration Controller" should {
    "display the correct Biks on the removal screen" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val title = Messages("RemoveBenefits.Heading").substring(0,44)
      implicit lazy val authenticatedRequest: AuthenticatedRequest[AnyContent] = createDummyUser(mockrequest)

      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.loadNextTaxYearOnRemoveData)(timeout)
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.15"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.16"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.17"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.18"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.19"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.20"))
    }
  }

  "When loading the currentTaxYearOnPageLoad, an authorised user" should {
    "be directed to cy page with list of biks" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      val title = Messages("AddBenefits.Heading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.currentTaxYearOnPageLoad(mockrequest))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.3"))
    }
  }

  "When loading the nextTaxYearAddOnPageLoad, an authorised user" should {
    "be directed to cy + 1 page with list of biks" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      val title = Messages("AddBenefits.Heading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.nextTaxYearAddOnPageLoad(mockrequest))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.1"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.3"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("AddBenefits.ChooseBenefitsLabel.1", "" + YEAR_RANGE.cy, "" + YEAR_RANGE.cyplus1))
    }
  }


  "When loading the nextTaxYearRemoveOnPageLoad, an authorised user" should {
    "be directed to the cy + 1 remove page with biks to remove" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      val title = Messages("RemoveBenefits.Heading").substring(0, 10)
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.nextTaxYearRemoveOnPageLoad.apply(mockrequest))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.15"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.16"))
      result.body.asInstanceOf[Strict].data.utf8String must not include Messages("ManagingRegistration.add.hint")
      result.body.asInstanceOf[Strict].data.utf8String must not include Messages("ManagingRegistration.cant.hint")
    }
  }

  "When loading the addOrRemovePageLoad, an authorised user" should {
    "be directed to options to add or remove page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      val title = Messages("RemoveBenefits.Heading").substring(0, 10)
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.nextTaxYearRemoveOnPageLoad.apply(mockrequest))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.15"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.16"))
    }
  }

  "When loading the addOrRemoveDecision, an authorised user" should {
    "be directed to the login page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      val title = Messages("RemoveBenefits.Heading").substring(0, 10)
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.nextTaxYearRemoveOnPageLoad.apply(mockrequest))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.15"))
      result.body.asInstanceOf[Strict].data.utf8String must include(Messages("BenefitInKind.label.16"))
    }
  }

  "When loading the confirmRemoveNextTaxYear, an authorised user" should {
    "be directed cy + 1 confirmation page to remove bik" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      val title = Messages("RemoveBenefits.Heading").substring(0, 10)
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.confirmRemoveNextTaxYear.apply(mockrequest))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When loading the updateRegisteredBenefitTypes, an authorised user" should {
    "persist their changes and be redirected to the what next page" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true)), Some(BinaryRadioButtonWithDesc("software", None)))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = mockRequestForm
      val title = Messages("whatNext.subHeading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.updateRegisteredBenefitTypes.apply(mockRequestForm))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When loading the addNextYearRegisteredBenefitTypes, an unauthorised user" should {
    "be directed to the login page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      val title = Messages("whatNext.add.heading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.addNextYearRegisteredBenefitTypes.apply(noSessionIdRequest))(timeout)
      result.header.status must be(UNAUTHORIZED)
      result.body.asInstanceOf[Strict].data.utf8String must include("Request was not authenticated user should be redirected")
    }
  }

  "When loading the removeNextYearRegisteredBenefitTypes, an unauthorised user" should {
    "be directed to the login page" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      val title = Messages("whatNext.add.heading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.removeNextYearRegisteredBenefitTypes.apply(noSessionIdRequest))(timeout)
      result.header.status must be(UNAUTHORIZED)
      result.body.asInstanceOf[Strict].data.utf8String must include("Request was not authenticated user should be redirected")
    }
  }

  "When a user removes a benefit" should {
    "selecting 'software' should redirect to what next page" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true)), Some(BinaryRadioButtonWithDesc("software", None)))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = mockRequestForm
      val title = Messages("whatNext.subHeading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.updateRegisteredBenefitTypes.apply(mockRequestForm))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When a user removes a benefit" should {
    "selecting 'guidance' should redirect to what next page" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true)), Some(BinaryRadioButtonWithDesc("guidance", None)))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = mockRequestForm
      val title = Messages("whatNext.subHeading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.updateRegisteredBenefitTypes.apply(mockRequestForm))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When a user removes a benefit" should {
    "selecting 'not-clear' should redirect to what next page" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true)), Some(BinaryRadioButtonWithDesc("not-clear", None)))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = mockRequestForm
      val title = Messages("whatNext.subHeading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.updateRegisteredBenefitTypes.apply(mockRequestForm))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When a user removes a benefit" should {
    "selecting 'not-offering' should redirect to what next page" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true)), Some(BinaryRadioButtonWithDesc("not-offering", None)))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = mockRequestForm
      val title = Messages("whatNext.subHeading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.updateRegisteredBenefitTypes.apply(mockRequestForm))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When a user removes a benefit" should {
    "selecting 'other' & providing 'info' should redirect to what next page" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true)), Some(BinaryRadioButtonWithDesc("other", Some("other info here"))))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = mockRequestForm
      val title = Messages("whatNext.subHeading")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.updateRegisteredBenefitTypes.apply(mockRequestForm))(timeout)
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include(title)
    }
  }

  "When a user removes a benefit" should {
    "selecting 'other' & not providing 'info' should redirect with error" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true)), Some(BinaryRadioButtonWithDesc("other", None)))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = mockRequestForm
      val errorMsg = Messages("RemoveBenefits.reason.other.required")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(registrationController.removeNextYearRegisteredBenefitTypes.apply(mockRequestForm))(timeout)
      result.header.status must be(SEE_OTHER) // 303
    }
  }

  "When a user removes a benefit" should {
    "selecting no reason should redirect with error" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true), RegistrationItem("8", active = true, enabled = true)), None)
      val bikList = List(Bik("8", 10))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      val errorMsg = Messages("RemoveBenefits.reason.no.selection")
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(Future{registrationController.removeBenefitReasonValidation(mockRegistrationList, form, 2017, bikList, bikList)}(scala.concurrent.ExecutionContext.Implicits.global))
      result.header.status must be(SEE_OTHER) // 303
      result.header.headers.getOrElse("Location","") must be("/payrollbik/services/remove-benefit-expense")
      result.header.headers.getOrElse("Set-Cookie","").replace("+"," ").replace("%27", "'") must be("PLAY_FLASH=error=" + errorMsg + "; Path=/; HTTPOnly")
    }

    "selecting 'other' reason but no explanation should redirect with error" in {
        val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true), RegistrationItem("8", active = true, enabled = true)),
          Some(BinaryRadioButtonWithDesc("other", Some(""))))
        val bikList = List(Bik("8", 10))
        val form = objSelectedForm.fill(mockRegistrationList)
        val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
        val errorMsg = Messages("RemoveBenefits.reason.other.required")
        implicit val timeout: FiniteDuration = timeoutValue
        val result = await(Future{registrationController.removeBenefitReasonValidation(mockRegistrationList, form, 2017, bikList, bikList)}(scala.concurrent.ExecutionContext.Implicits.global))
        result.header.status must be(SEE_OTHER) // 303
        result.header.headers.getOrElse("Location","") must be("/payrollbik/services/remove-benefit-expense")
        result.header.headers.getOrElse("Set-Cookie","").replace("+"," ").replace("%E2%80%99", "’") must be("PLAY_FLASH=error=" + errorMsg + "; Path=/; HTTPOnly")
    }

    "selecting 'other' reason but and providing explanation should redirect to what-next" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true), RegistrationItem("8", true, true)),
        Some(BinaryRadioButtonWithDesc("other", Some("bla bla other reason text"))))
      val bikList = List(Bik("8", 10))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(Future{registrationController.removeBenefitReasonValidation(mockRegistrationList, form, 2017, bikList, bikList)}(scala.concurrent.ExecutionContext.Implicits.global))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include("Benefit removed")
    }

    "selecting 'software' reason should redirect to what-next" in {
      val mockRegistrationList = RegistrationList(None, List(RegistrationItem("31", active = true, enabled = true), RegistrationItem("8", active = true, enabled = true)),
        Some(BinaryRadioButtonWithDesc("software", None)))
      val bikList = List(Bik("8", 10))
      val form = objSelectedForm.fill(mockRegistrationList)
      val mockRequestForm = mockrequest.withFormUrlEncodedBody(form.data.toSeq: _*)
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = mockRequestForm
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        EmpRef("taxOfficeNumber", "taxOfficeReference"),
        UserName(Name(None, None)),
        request)
      implicit val timeout: FiniteDuration = timeoutValue
      val result = await(Future{registrationController.removeBenefitReasonValidation(mockRegistrationList, form, 2017, bikList, bikList)}(scala.concurrent.ExecutionContext.Implicits.global))
      result.header.status must be(OK) // 200
      result.body.asInstanceOf[Strict].data.utf8String must include("Benefit removed")
    }
  }

}

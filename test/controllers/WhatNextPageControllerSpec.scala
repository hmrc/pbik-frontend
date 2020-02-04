/*
 * Copyright 2020 HM Revenue & Customs
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

import config._
import connectors.HmrcTierConnector
import javax.inject.Inject
import models._
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.data.Form
import play.api.http.HttpEntity.Strict
import play.api.i18n.{Lang, Messages}
import play.api.i18n.Messages.Implicits._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BikListService
import support.TestAuthUser
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.time.TaxYear
import utils.{ControllersReferenceData, FormMappings, TaxDateUtils, URIInformation}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

class WhatNextPageControllerSpec extends PlaySpec with FakePBIKApplication with TestAuthUser {

  // TODO The following needs refactoring as it similar to registrationcontrollertest, consider moving to utils

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[BikListService].toInstance(mock(classOf[StubBikListService])))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .build()

  implicit val lang = Lang("en-GB")

  val formMappings: FormMappings = app.injector.instanceOf[FormMappings]
  val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  implicit val pbikContext: PbikContext = app.injector.instanceOf[PbikContext]

  lazy val listOfPeople: List[EiLPerson] = List(
    EiLPerson("AA111111", "John", Some("Stones"), "Smith", Some("123"), Some("01/01/1980"), Some("male"), Some(10), 0),
    EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None, 0),
    EiLPerson(
      "AC111111",
      "Humpty",
      Some("Alexander"),
      "Dumpty",
      Some("123"),
      Some("01/01/1980"),
      Some("male"),
      Some(10),
      0),
    EiLPerson("AD111111", "Peter", Some("James"), "Johnson", None, None, None, None, 0),
    EiLPerson(
      "AE111111",
      "Alice",
      Some("In"),
      "Wonderland",
      Some("123"),
      Some("03/02/1978"),
      Some("female"),
      Some(10),
      0),
    EiLPerson(
      "AF111111",
      "Humpty",
      Some("Alexander"),
      "Dumpty",
      Some("123"),
      Some("01/01/1980"),
      Some("male"),
      Some(10),
      0)
  )

  lazy val listOfPeopleForm: Form[EiLPersonList] = formMappings.individualsForm.fill(EiLPersonList(listOfPeople))
  lazy val registrationList = RegistrationList(None, List(RegistrationItem("30", active = true, enabled = true)))
  lazy val registrationListMultiple = RegistrationList(
    None,
    List(RegistrationItem("30", active = true, enabled = true), RegistrationItem("8", true, true)))
  lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))

  def YEAR_RANGE: TaxYearRange = taxDateUtils.getTaxYearRange()

  class StubBikListService @Inject()(
    pbikAppConfig: AppConfig,
    tierConnector: HmrcTierConnector,
    controllersReferenceData: ControllersReferenceData,
    uriInformation: URIInformation)
      extends BikListService(
        pbikAppConfig,
        tierConnector,
        controllersReferenceData,
        uriInformation
      ) {

    lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))

    override def currentYearList(
      implicit hc: HeaderCarrier,
      request: AuthenticatedRequest[_]): Future[(Map[String, String], List[Bik])] =
      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 10
      }))

    override def nextYearList(
      implicit hc: HeaderCarrier,
      request: AuthenticatedRequest[_]): Future[(Map[String, String], List[Bik])] =
      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) > 10
      }))

    when(
      tierConnector.genericGetCall[List[Bik]](anyString, anyString, any[EmpRef], mockEq(YEAR_RANGE.cy))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](anyString, anyString, any[EmpRef], mockEq(YEAR_RANGE.cyminus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](anyString, anyString, any[EmpRef], mockEq(YEAR_RANGE.cyplus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](anyString, mockEq(""), any[EmpRef], mockEq(YEAR_RANGE.cy))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](
        anyString,
        mockEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
        mockEq(EmpRef.empty),
        mockEq(YEAR_RANGE.cy))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](
        anyString,
        mockEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
        mockEq(EmpRef.empty),
        mockEq(YEAR_RANGE.cyminus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](
        anyString,
        mockEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
        mockEq(EmpRef.empty),
        mockEq(YEAR_RANGE.cyplus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](anyString, anyString, any[EmpRef], mockEq(2020))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 5
    }))

    when(
      tierConnector.genericPostCall(
        anyString,
        mockEq(app.injector.instanceOf[URIInformation].updateBenefitTypesPath),
        any[EmpRef],
        anyInt,
        any)(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]]))
      .thenReturn(Future.successful(new FakeResponse()))

    when(
      tierConnector.genericGetCall[List[Bik]](
        anyString,
        mockEq(app.injector.instanceOf[URIInformation].getRegisteredPath),
        any[EmpRef],
        anyInt)(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) >= 15
      }))

  }

  class FakeResponse extends HttpResponse {
    override def status = 200
  }

  val whatNextPageController: WhatNextPageController = {
    val w = injected[WhatNextPageController]

    val dateRange: TaxYearRange = taxDateUtils.getTaxYearRange()

    when(
      w.tierConnector.genericGetCall[List[Bik]](anyString, mockEq(""), any[EmpRef], mockEq(YEAR_RANGE.cy))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      w.tierConnector.genericGetCall[List[Bik]](
        anyString,
        mockEq(injected[URIInformation].getBenefitTypesPath),
        mockEq(EmpRef.empty),
        mockEq(YEAR_RANGE.cy))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      w.tierConnector.genericGetCall[List[Bik]](
        anyString,
        mockEq(injected[URIInformation].getBenefitTypesPath),
        mockEq(EmpRef.empty),
        mockEq(YEAR_RANGE.cyminus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      w.tierConnector.genericGetCall[List[Bik]](
        anyString,
        mockEq(injected[URIInformation].getBenefitTypesPath),
        mockEq(EmpRef.empty),
        mockEq(YEAR_RANGE.cyplus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      w.tierConnector.genericGetCall[List[Bik]](anyString, anyString, any[EmpRef], mockEq(2020))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 5
    }))

    when(
      w.tierConnector
        .genericPostCall(anyString, mockEq(injected[URIInformation].updateBenefitTypesPath), any[EmpRef], anyInt, any)(
          any[HeaderCarrier],
          any[Request[_]],
          any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    when(
      w.tierConnector.genericGetCall[List[Bik]](
        anyString,
        mockEq(injected[URIInformation].getRegisteredPath),
        any[EmpRef],
        anyInt)(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) >= 15
      }))

    w
  }

  "When loading the what next page" must {
    "(Register a BIK current year) Single benefit- state the status is ok and correct page is displayed" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val formRegistrationList: Form[RegistrationList] = formMappings.objSelectedForm
      val formFilled = formRegistrationList.fill(registrationList)
      val year = TaxYear.taxYearFor(LocalDate.now).currentYear
      val result = whatNextPageController.loadWhatNextRegisteredBIK(formFilled, year)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include("Registration complete")
      result.body.asInstanceOf[Strict].data.utf8String must include(
        s"Now tax Private medical treatment or insurance through your payroll from 6 April $year.")
    }

    "(Register a BIK next year) Single benefit - state the status is ok and correct page is displayed" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val formRegistrationList: Form[RegistrationList] = formMappings.objSelectedForm
      val formFilled = formRegistrationList.fill(registrationList)
      formRegistrationList.fill(registrationList)
      val result = whatNextPageController.loadWhatNextRegisteredBIK(formFilled, 2017)
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include("Registration complete")
      result.body.asInstanceOf[Strict].data.utf8String must include(
        "Now tax Private medical treatment or insurance through your payroll from 6 April")
    }

    "(Register a BIK next year) Multiple benefits - state the status is ok and correct page is displayed" in {
      import play.api.libs.concurrent.Execution.Implicits._

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
      val formRegistrationList: Form[RegistrationList] = formMappings.objSelectedForm.fill(registrationListMultiple)
      val result = await(Future {
        whatNextPageController.loadWhatNextRegisteredBIK(formRegistrationList, 2016)
      })
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include("Registration complete")
      result.body.asInstanceOf[Strict].data.utf8String must include("Private medical treatment or insurance")
      result.body.asInstanceOf[Strict].data.utf8String must include("Services supplied")
    }

    "(Remove a BIK)- state the status is ok and correct page is displayed" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
      val whatNextRemoveMsg: String = Messages("whatNext.remove.p1")
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))

      val formRegistrationList: Form[RegistrationList] = formMappings.objSelectedForm.fill(registrationList)
      val result = await(Future {
        whatNextPageController.loadWhatNextRemovedBIK(formRegistrationList, year = 2015)
      })
      result.header.status must be(OK)
      result.body.asInstanceOf[Strict].data.utf8String must include("Benefit removed")
      result.body.asInstanceOf[Strict].data.utf8String must include(whatNextRemoveMsg)
    }

  }

}

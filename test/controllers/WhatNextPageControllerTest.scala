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

import java.util.UUID

import config._
import play.api.data.Form

import scala.concurrent.Future
import connectors.{HmrcTierConnector, TierConnector}
import models._
import org.joda.time.{DateTime, LocalDate}
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.http.HttpEntity.Strict
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.{Crypto, json}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.UnsignedTokenProvider
import services.BikListService
import support.TestAuthUser
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.test.UnitSpec
import utils.{ControllersReferenceData, FormMappings, TaxDateUtils}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.time.TaxYearResolver


class WhatNextPageControllerTest extends PlaySpec with OneAppPerSuite with FakePBIKApplication
                                              with FormMappings with TestAuthUser{

  // TODO The following needs refactoring as it similar to registrationcontrollertest, consider moving to utils
  // val sessionId = s"session-${UUID.randomUUID}"
  // val userId = s"user-${UUID.randomUUID}"
  implicit val user = createDummyUser("testid")
  implicit val context: PbikContext = PbikContextImpl

  lazy val listOfPeople: List[EiLPerson] = List(EiLPerson("AA111111","John", Some("Stones") ,"Smith",Some("123"),Some("01/01/1980"),Some("male"), Some(10),0),
    EiLPerson("AB111111","Adam", None ,"Smith",None, Some("01/01/1980"),Some("male"), None, 0),
    EiLPerson("AC111111", "Humpty", Some("Alexander"),"Dumpty", Some("123"), Some("01/01/1980"),Some("male"), Some(10), 0),
    EiLPerson("AD111111", "Peter", Some("James"),"Johnson",None, None, None, None, 0),
    EiLPerson("AE111111", "Alice", Some("In") ,"Wonderland", Some("123"),Some("03/02/1978"), Some("female"), Some(10), 0),
    EiLPerson("AF111111", "Humpty", Some("Alexander"),"Dumpty", Some("123"), Some("01/01/1980"), Some("male"), Some(10), 0))

  lazy val listOfPeopleForm: Form[EiLPersonList] = individualsForm.fill(new EiLPersonList(listOfPeople))
  lazy val registrationList = RegistrationList(None, List(RegistrationItem("30", true, true)))
  lazy val registrationListMultiple = RegistrationList(None, List(RegistrationItem("30", true, true), RegistrationItem("8", true, true)))
  lazy val CYCache = List.tabulate(21)(n => new Bik("" + (n + 1), 10))
  def YEAR_RANGE:TaxYearRange = TaxDateUtils.getTaxYearRange()

  class StubBikListService extends BikListService {
    override lazy val pbikAppConfig = mock[AppConfig]
    override val tierConnector = mock[HmrcTierConnector]
    lazy val CYCache = List.tabulate(21)(n => new Bik("" + (n + 1), 10))

    override def currentYearList(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]):
        Future[(Map[String, String], List[Bik])] = {

      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik => (Integer.parseInt(x.iabdType) <= 10) }))
    }

    override def nextYearList(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]):
        Future[(Map[String, String], List[Bik])] = {

      Future.successful((Map(HeaderTags.ETAG -> "1"), CYCache.filter { x: Bik => (Integer.parseInt(x.iabdType) > 10) }))
    }

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cy))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))


    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(""),
      anyString, mockEq(YEAR_RANGE.cy))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, mockEq(2020))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 5) }))

    when(tierConnector.genericPostCall(anyString, mockEq(updateBenefitTypesPath),
      anyString, anyInt, any)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericGetCall[List[Bik]](anyString,  mockEq(getRegisteredPath),
      anyString, anyInt)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) >= 15) }))

  }

  class FakeResponse extends HttpResponse {
    override def status = 200
  }

  class MockWhatNextPageController extends WhatNextPageController with TierConnector {

    override def AuthorisedForPbik(body: AuthContext => Request[AnyContent] => Future[Result]): Action[AnyContent] = {
      val user = createDummyUser("testid")
      Action.async { implicit request =>
        if (request.session.get("sessionId").getOrElse("").startsWith("session")) {
          body(user)(request)
        } else {
          Future(Unauthorized("Request was not authenticated user should be redirected"))
        }
      }
    }

    override lazy val pbikAppConfig = mock[AppConfig]
    override def bikListService: BikListService = new StubBikListService
    override val tierConnector = mock[HmrcTierConnector]

    val dateRange = TaxDateUtils.getTaxYearRange()

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(""),
      anyString, mockEq(YEAR_RANGE.cy))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cy))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cyminus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, mockEq(getBenefitTypesPath),
      mockEq(""), mockEq(YEAR_RANGE.cyplus1))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 10) }))

    when(tierConnector.genericGetCall[List[Bik]]( anyString, anyString,
      anyString, mockEq(2020))(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) <= 5) }))

    when(tierConnector.genericPostCall(anyString, mockEq(updateBenefitTypesPath),
      anyString, anyInt, any)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

    when(tierConnector.genericGetCall[List[Bik]](anyString,  mockEq(getRegisteredPath),
      anyString, anyInt)(any[HeaderCarrier],any[Request[_]],
        any[json.Format[List[Bik]]], any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter{ x:Bik =>
      (Integer.parseInt(x.iabdType) >= 15) }))

  }


  // start tests
  "When loading the what next page " must {
       "(Register a BIK current year) Single benefit- state the status is ok and correct page is displayed" in {
         import play.api.libs.concurrent.Execution.Implicits._

         val mockWhatNextPageController = new MockWhatNextPageController
         def csrfToken = "csrfToken" ->  Crypto.generateToken //"csrfToken"Name -> UnsignedTokenProvider.generateToken
         implicit val request = mockrequest
         implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
         val formRegistrationList: Form[RegistrationList] = objSelectedForm
         val formFilled = formRegistrationList.fill(registrationList)
         val year = TaxYearResolver.taxYearFor(LocalDate.now)
         val result = await(Future{mockWhatNextPageController.loadWhatNextRegisteredBIK(formFilled, year)})
         result.header.status must be(OK)
         result.body.asInstanceOf[Strict].data.utf8String must include("Registration complete")
         result.body.asInstanceOf[Strict].data.utf8String must include(
           s"Now tax Private medical treatment or insurance through your payroll from 6 April ${year}.")
       }

      "(Register a BIK next year) Single benefit - state the status is ok and correct page is displayed" in {
        import play.api.libs.concurrent.Execution.Implicits._

        val mockWhatNextPageController = new MockWhatNextPageController
        def csrfToken = "csrfToken" ->  Crypto.generateToken //"csrfToken"Name -> UnsignedTokenProvider.generateToken
        implicit val request = mockrequest
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        val formRegistrationList: Form[RegistrationList] = objSelectedForm
        val formFilled = formRegistrationList.fill(registrationList)
        formRegistrationList.fill(registrationList)
        val result = await(Future{mockWhatNextPageController.loadWhatNextRegisteredBIK(formFilled, 2017)})
        result.header.status must be(OK)
        result.body.asInstanceOf[Strict].data.utf8String must include("Registration complete")
        result.body.asInstanceOf[Strict].data.utf8String must include("Now tax Private medical treatment or insurance through your payroll from 6 April")
      }

      "(Register a BIK next year) Multiple benefits - state the status is ok and correct page is displayed" in {
        import play.api.libs.concurrent.Execution.Implicits._

        val mockWhatNextPageController = new MockWhatNextPageController
        def csrfToken = "csrfToken" ->  Crypto.generateToken //"csrfToken"Name -> UnsignedTokenProvider.generateToken
        implicit val request = mockrequest
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))
        val formRegistrationList: Form[RegistrationList] = objSelectedForm.fill(registrationListMultiple)
        val result = await(Future{mockWhatNextPageController.loadWhatNextRegisteredBIK(formRegistrationList, 2016)})
        result.header.status must be(OK)
        result.body.asInstanceOf[Strict].data.utf8String must include("Registration complete")
        result.body.asInstanceOf[Strict].data.utf8String must include("Private medical treatment or insurance")
        result.body.asInstanceOf[Strict].data.utf8String must include("Services supplied")
      }

     "(Remove a BIK)- state the status is ok and correct page is displayed" in {
         import play.api.libs.concurrent.Execution.Implicits._

         val mockWhatNextPageController = new MockWhatNextPageController
         def csrfToken = "csrfToken" ->  Crypto.generateToken //"csrfToken"Name -> UnsignedTokenProvider.generateToken
         implicit val request = mockrequest
          val whatNextRemoveMsg = Messages("whatNext.remove.p1")
         implicit val hc = new HeaderCarrier(sessionId = Some(SessionId("session001")))

         val formRegistrationList: Form[RegistrationList] = objSelectedForm.fill(registrationList)
         val result = await(Future{mockWhatNextPageController.loadWhatNextRemovedBIK(formRegistrationList, 2015)})
         result.header.status must be(OK)
         result.body.asInstanceOf[Strict].data.utf8String must include("Benefit removed")
         result.body.asInstanceOf[Strict].data.utf8String must include(
           whatNextRemoveMsg
         )
     }

  }

}

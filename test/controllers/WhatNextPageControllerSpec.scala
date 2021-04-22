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

import config._
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import javax.inject.Inject
import models._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{eq => argEq, any}
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
import services.{BikListService, SessionService}
import support.TestAuthUser
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId, SessionKeys}
import uk.gov.hmrc.time.TaxYear
import utils.{ControllersReferenceData, FormMappings, TaxDateUtils, TestAuthAction, TestNoSessionCheckAction, URIInformation}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

class WhatNextPageControllerSpec extends PlaySpec with FakePBIKApplication with TestAuthUser {

  override lazy val fakeApplication: Application = GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .overrides(bind[AuthAction].to(classOf[TestAuthAction]))
    .overrides(bind[NoSessionCheckAction].to(classOf[TestNoSessionCheckAction]))
    .overrides(bind[BikListService].toInstance(mock(classOf[StubBikListService])))
    .overrides(bind[HmrcTierConnector].toInstance(mock(classOf[HmrcTierConnector])))
    .overrides(bind[SessionService].toInstance(mock(classOf[SessionService])))
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
      tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], argEq(YEAR_RANGE.cy))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], argEq(YEAR_RANGE.cyminus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], argEq(YEAR_RANGE.cyplus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](any[String], argEq(""), any[EmpRef], argEq(YEAR_RANGE.cy))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(tierConnector.genericGetCall[List[Bik]](
      any[String],
      argEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
      argEq(EmpRef.empty),
      argEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 10
      }))

    when(
      tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
        argEq(EmpRef.empty),
        argEq(YEAR_RANGE.cyminus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(app.injector.instanceOf[URIInformation].getBenefitTypesPath),
        argEq(EmpRef.empty),
        argEq(YEAR_RANGE.cyplus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], argEq(2020))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 5
    }))

    when(
      tierConnector.genericPostCall(
        any[String],
        argEq(app.injector.instanceOf[URIInformation].updateBenefitTypesPath),
        any[EmpRef],
        any[Int],
        any)(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]]))
      .thenReturn(Future.successful(new FakeResponse()))

    when(
      tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(app.injector.instanceOf[URIInformation].getRegisteredPath),
        any[EmpRef],
        any[Int])(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) >= 15
      }))

  }

  class FakeResponse extends HttpResponse {
    override def status = 200
    override def allHeaders: Map[String, Seq[String]] = Map()
    override def body: String = "empty"
  }

  val whatNextPageController: WhatNextPageController = {
    val w = app.injector.instanceOf[WhatNextPageController]

    val dateRange: TaxYearRange = taxDateUtils.getTaxYearRange()

    when(
      w.tierConnector.genericGetCall[List[Bik]](any[String], argEq(""), any[EmpRef], argEq(YEAR_RANGE.cy))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(w.tierConnector.genericGetCall[List[Bik]](
      any[String],
      argEq(injected[URIInformation].getBenefitTypesPath),
      argEq(EmpRef.empty),
      argEq(YEAR_RANGE.cy))(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) <= 10
      }))

    when(
      w.tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(injected[URIInformation].getBenefitTypesPath),
        argEq(EmpRef.empty),
        argEq(YEAR_RANGE.cyminus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      w.tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(injected[URIInformation].getBenefitTypesPath),
        argEq(EmpRef.empty),
        argEq(YEAR_RANGE.cyplus1))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 10
    }))

    when(
      w.tierConnector.genericGetCall[List[Bik]](any[String], any[String], any[EmpRef], argEq(2020))(
        any[HeaderCarrier],
        any[Request[_]],
        any[json.Format[List[Bik]]],
        any[Manifest[List[Bik]]])).thenReturn(Future.successful(CYCache.filter { x: Bik =>
      Integer.parseInt(x.iabdType) <= 5
    }))

    when(
      w.tierConnector
        .genericPostCall(
          any[String],
          argEq(injected[URIInformation].updateBenefitTypesPath),
          any[EmpRef],
          any[Int],
          any)(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]]))
      .thenReturn(Future.successful(new FakeResponse()))

    when(
      w.tierConnector.genericGetCall[List[Bik]](
        any[String],
        argEq(injected[URIInformation].getRegisteredPath),
        any[EmpRef],
        any[Int])(any[HeaderCarrier], any[Request[_]], any[json.Format[List[Bik]]], any[Manifest[List[Bik]]]))
      .thenReturn(Future.successful(CYCache.filter { x: Bik =>
        Integer.parseInt(x.iabdType) >= 15
      }))
    w
  }

  "(Register a BIK next year) Single benefit - state the status is ok and correct page is displayed" in {
    when(whatNextPageController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
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
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
    implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
      AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
    val result = whatNextPageController.showWhatNextRegisteredBik("cy1").apply(authenticatedRequest)
    (scala.concurrent.ExecutionContext.Implicits.global)
    status(result) must be(OK)
    contentAsString(result) must include("Registration complete")
    contentAsString(result) must include(
      "Now tax Private medical treatment or insurance through your payroll from 6 April")
  }

  "(Register a BIK next year) Multiple benefits - state the status is ok and correct page is displayed" in {
    when(whatNextPageController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
      .thenReturn(
        Future.successful(
          Some(
            PbikSession(
              Some(
                RegistrationList(active = List(RegistrationItem("30", true, true), RegistrationItem("8", true, true)))),
              None,
              None,
              None,
              None,
              None,
              None
            ))))
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
    implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
      AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session002")))
    val result = whatNextPageController.showWhatNextRegisteredBik("cy1").apply(authenticatedRequest)
    (scala.concurrent.ExecutionContext.Implicits.global)
    status(result) must be(OK)
    contentAsString(result) must include("Registration complete")
    contentAsString(result) must include("Private medical treatment or insurance")
    contentAsString(result) must include("Services supplied")
  }

  "(Remove a BIK)- state the status is ok and correct page is displayed" in {
    when(whatNextPageController.cachingService.fetchPbikSession()(any[HeaderCarrier]))
      .thenReturn(
        Future.successful(
          Some(
            PbikSession(
              None,
              Some(RegistrationItem("30", true, true)),
              None,
              None,
              None,
              None,
              None
            ))))
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = mockrequest
    implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
      AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(None, None)), request)
    val whatNextRemoveMsg: String = Messages("whatNext.remove.p1")
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session001")))
    val result = whatNextPageController.showWhatNextRemovedBik().apply(authenticatedRequest)
    (scala.concurrent.ExecutionContext.Implicits.global)
    status(result) must be(OK)
    contentAsString(result) must include("Benefit removed")
    contentAsString(result) must include(whatNextRemoveMsg)
  }

  "The calculateTaxYear method" should {
    val startYear = TaxYear(new LocalDate().getYear)

    "return this tax year and next year if given true" in {
      val result = whatNextPageController.calculateTaxYear(true)
      assert(result._1 == startYear.startYear)
      assert(result._2 == startYear.startYear + 1)
    }

    "return next year and the year after if given false" in {
      val result = whatNextPageController.calculateTaxYear(false)
      assert(result._1 == startYear.startYear + 1)
      assert(result._2 == startYear.startYear + 2)
    }
  }

}

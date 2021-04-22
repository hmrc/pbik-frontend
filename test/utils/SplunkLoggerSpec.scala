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

package utils

import controllers.FakePBIKApplication
import models._
import play.api.libs.crypto.CSRFTokenSigner
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import support.{TestAuthUser, TestSplunkLogger}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

class SplunkLoggerSpec extends WordSpecLike with Matchers with OptionValues with FakePBIKApplication with TestAuthUser {

  val testList: List[EiLPerson] =
    List[EiLPerson](new EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None, 0))
  val testPersonList: EiLPersonList = EiLPersonList(testList)

  class SetUp {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller: TestSplunkLogger = app.injector.instanceOf[TestSplunkLogger]
    val msg = "Hello"

    val csrfTokenSigner = app.injector.instanceOf[CSRFTokenSigner]
    def csrfToken: (String, String) =
      "csrfToken" -> csrfTokenSigner.generateToken //"csrfToken"Name -> UnsignedTokenProvider.generateToken
    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken)

    def fakeAuthenticatedRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest().withSession(csrfToken).withHeaders()

    val pbikDataEvent = DataEvent(
      auditSource = SplunkLogger.pbik_audit_source,
      auditType = SplunkLogger.pbik_benefit_type,
      detail = Map(
        SplunkLogger.key_event_name -> SplunkLogger.pbik_event_name,
        SplunkLogger.key_gateway_user -> EmpRef(
          taxOfficeNumber = "taxOfficeNumber",
          taxOfficeReference = "taxOfficeReference").toString,
        SplunkLogger.key_tier    -> controller.FRONTEND.toString,
        SplunkLogger.key_action  -> controller.ADD.toString,
        SplunkLogger.key_target  -> controller.BIK.toString,
        SplunkLogger.key_period  -> controller.CYP1.toString,
        SplunkLogger.key_message -> msg
      )
    )
  }

  "When logging events, the SplunkLogger" should {
    "return a properly formatted DataEvent for Pbik events" in new SetUp {
      val d: DataEvent = controller.createDataEvent(
        controller.FRONTEND,
        controller.ADD,
        controller.BIK,
        controller.CYP1,
        msg = "Employer Added Bik to CY Plus 1",
        name = Option(UserName(Name(Some("TEST_USER"), None))),
        empRef = Some(models.EmpRef("taxOfficeNumber", "taxOfficeReference"))
      )
      assert(d.auditSource == SplunkLogger.pbik_audit_source)
      assert(d.detail.nonEmpty)
      assert(d.detail.contains(SplunkLogger.key_event_name))
      assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
      assert(d.detail.get(SplunkLogger.key_empref).get == "taxOfficeNumber/taxOfficeReference")
      assert(d.detail.get(SplunkLogger.key_gateway_user).get == "TEST_USER")
      assert(d.detail.get(SplunkLogger.key_action).get == controller.ADD.toString)
      assert(d.detail.get(SplunkLogger.key_tier).get == controller.FRONTEND.toString)
      assert(d.detail.get(SplunkLogger.key_target).get == controller.BIK.toString)
      assert(d.detail.get(SplunkLogger.key_period).get == controller.CYP1.toString)
    }
  }

  "When logging events, the SplunkLogger without a ePaye account" should {
    "return a properly formatted DataEvent with a empref default for Pbik events" in new SetUp {
      val d: DataEvent = controller.createDataEvent(
        controller.FRONTEND,
        controller.ADD,
        controller.BIK,
        controller.CYP1,
        msg = "Employer Added Bik to CY Plus 1",
        name = Option(UserName(Name(Some("TEST_USER"), None))),
        empRef = None
      )

      assert(d.auditSource == SplunkLogger.pbik_audit_source)
      assert(d.detail.nonEmpty)
      assert(d.detail.contains(SplunkLogger.key_event_name))
      assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
      assert(d.detail.get(SplunkLogger.key_empref).get == SplunkLogger.pbik_no_ref)
      assert(d.detail.get(SplunkLogger.key_gateway_user).get == "TEST_USER")
      assert(d.detail.get(SplunkLogger.key_action).get == controller.ADD.toString)
      assert(d.detail.get(SplunkLogger.key_tier).get == controller.FRONTEND.toString)
      assert(d.detail.get(SplunkLogger.key_target).get == controller.BIK.toString)
      assert(d.detail.get(SplunkLogger.key_period).get == controller.CYP1.toString)
    }
  }

  "When logging events, the SplunkLogger" should {

    "complete successfully when sending a PBIK DataEvent" in new SetUp {
      val r: AuditResult = await(controller.logSplunkEvent(pbikDataEvent))
      assert(r == Success)
    }
  }

  "When logging events, the SplunkLogger" should {
    "complete successfully when sending a general DataEvent" in new SetUp {
      val nonPbikEvent = DataEvent(
        auditSource = "TEST",
        auditType = "TEST-AUDIT-TYPE",
        detail = Map(
          "event"     -> "APPLY-THE-TEST",
          "id"        -> "Test Id",
          "key1"      -> "val1",
          "key2"      -> "val2",
          "key3"      -> "val3",
          "key4"      -> "val4",
          "key5"      -> "val5",
          "other key" -> "other data")
      )
      val r: AuditResult = await(controller.logSplunkEvent(nonPbikEvent))
      assert(r == Success)
    }
  }

  "When logging events, the SplunkLogger" should {
    "return a properly formatted DataEvent for Pbik errors" in new SetUp {
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        models.EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference = "taxOfficeReference"),
        UserName(Name(Some("TEST_USER"), None)),
        FakeRequest()
      )

      val d: DataEvent =
        controller.createErrorEvent(controller.FRONTEND, controller.EXCEPTION, "No PAYE Scheme found for user")

      assert(d.auditSource == SplunkLogger.pbik_audit_source)
      assert(d.detail.nonEmpty)
      assert(d.detail.contains(SplunkLogger.key_event_name))
      assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
      assert(
        d.detail.get(SplunkLogger.key_empref).get == EmpRef(
          taxOfficeNumber = "taxOfficeNumber",
          taxOfficeReference = "taxOfficeReference").toString)
      assert(d.detail.get(SplunkLogger.key_gateway_user).get == "TEST_USER")
      assert(d.detail.get(SplunkLogger.key_error).get == controller.EXCEPTION.toString)
      assert(d.detail.get(SplunkLogger.key_message).get == "No PAYE Scheme found for user")
    }
  }

  "When logging events, the SplunkLogger" should {
    "mark the empref with a default if one is not present" in new SetUp {
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        models.EmpRef.empty,
        UserName(Name(Some("TEST_USER"), None)),
        FakeRequest()
      )

      val d: DataEvent = controller.createErrorEvent(controller.FRONTEND, controller.EXCEPTION, msg = "No Empref")

      assert(d.auditSource == SplunkLogger.pbik_audit_source)
      assert(d.detail.nonEmpty)
      assert(d.detail.contains(SplunkLogger.key_event_name))
      assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
      assert(d.detail.get(SplunkLogger.key_empref).get == SplunkLogger.pbik_no_ref)
      assert(d.detail.get(SplunkLogger.key_gateway_user).get == "TEST_USER")
      assert(d.detail.get(SplunkLogger.key_error).get == controller.EXCEPTION.toString)
      assert(d.detail.get(SplunkLogger.key_message).get == "No Empref")
    }
  }

  "When extracting a Nino from an empty list the conroller" should {
    "return a not found label" in new SetUp {
      assert(controller.extractListNino(List[EiLPerson]()) == SplunkLogger.pbik_no_ref)
    }
  }

  "When extracting a Nino from a list the conroller" should {
    "return a not found label" in new SetUp {
      assert(controller.extractListNino(testList) == "AB111111")
    }
  }

  "When extracting a Nino from a Person list the conroller" should {
    "return a not found label" in new SetUp {
      assert(controller.extractPersonListNino(testPersonList) == "AB111111")
    }
  }

  "When extracting a Nino from an empty Person List the conroller" should {
    "return a not found label" in new SetUp {
      assert(controller.extractPersonListNino(EiLPersonList(List[EiLPerson]())) == SplunkLogger.pbik_no_ref)
    }
  }

  "When extracting the Government Gateway Id from a valid user the controller" should {
    "return the Government Gateway name" in new SetUp {

      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        models.EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference = "taxOfficeReference"),
        UserName(Name(Some("TEST_USER"), None)),
        FakeRequest()
      )
      assert(controller.extractGovernmentGatewayString == "TEST_USER")
    }
  }

  "When extracting the Government Gateway Id from an invalid user the controller" should {
    "return the default" in new SetUp {

      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
        models.EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference = "taxOfficeReference"),
        UserName(Name(None, None)),
        FakeRequest()
      )
      assert(controller.extractGovernmentGatewayString == SplunkLogger.pbik_no_ref)
    }
  }

}

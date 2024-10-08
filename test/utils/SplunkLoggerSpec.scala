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

package utils

import base.FakePBIKApplication
import models.auth.AuthenticatedRequest
import play.api.libs.crypto.CSRFTokenSigner
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import support.TestSplunkLogger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent

class SplunkLoggerSpec extends FakePBIKApplication {

  class SetUp {
    implicit val hc: HeaderCarrier   = HeaderCarrier()
    val controller: TestSplunkLogger = injected[TestSplunkLogger]
    val msg                          = "Hello"

    val csrfTokenSigner: CSRFTokenSigner = injected[CSRFTokenSigner]
    val pbikDataEvent: DataEvent         = DataEvent(
      auditSource = SplunkLogger.pbik_audit_source,
      auditType = SplunkLogger.pbik_benefit_type,
      detail = Map(
        SplunkLogger.key_event_name   -> SplunkLogger.pbik_event_name,
        SplunkLogger.key_gateway_user -> empRef.toString(),
        SplunkLogger.key_tier         -> controller.FRONTEND.toString,
        SplunkLogger.key_action       -> controller.ADD.toString,
        SplunkLogger.key_target       -> controller.BIK.toString,
        SplunkLogger.key_period       -> controller.CYP1.toString,
        SplunkLogger.key_message      -> msg
      )
    )

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken)

    def csrfToken: (String, String) =
      "csrfToken" -> csrfTokenSigner.generateToken

    def fakeAuthenticatedRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest().withSession(csrfToken).withHeaders()
  }

  "When logging events, the SplunkLogger" should {
    "return a properly formatted DataEvent for Pbik events" in new SetUp {
      val d: DataEvent = controller.createDataEvent(
        controller.FRONTEND,
        controller.ADD,
        controller.BIK,
        controller.CYP1,
        msg = "Employer Added Bik to CY Plus 1",
        name = Some("TEST_USER"),
        empRef = Some(empRef)
      )
      assert(d.auditSource == SplunkLogger.pbik_audit_source)
      assert(d.detail.nonEmpty)
      assert(d.detail.contains(SplunkLogger.key_event_name))
      assert(d.detail(SplunkLogger.key_event_name) == SplunkLogger.pbik_event_name)
      assert(d.detail(SplunkLogger.key_empref) == empRef.toString())
      assert(d.detail(SplunkLogger.key_gateway_user) == "TEST_USER")
      assert(d.detail(SplunkLogger.key_action) == controller.ADD.toString)
      assert(d.detail(SplunkLogger.key_tier) == controller.FRONTEND.toString)
      assert(d.detail(SplunkLogger.key_target) == controller.BIK.toString)
      assert(d.detail(SplunkLogger.key_period) == controller.CYP1.toString)
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
        name = Some("TEST_USER"),
        empRef = None
      )

      assert(d.auditSource == SplunkLogger.pbik_audit_source)
      assert(d.detail.nonEmpty)
      assert(d.detail.contains(SplunkLogger.key_event_name))
      assert(d.detail(SplunkLogger.key_event_name) == SplunkLogger.pbik_event_name)
      assert(d.detail(SplunkLogger.key_empref) == SplunkLogger.pbik_no_ref)
      assert(d.detail(SplunkLogger.key_gateway_user) == "TEST_USER")
      assert(d.detail(SplunkLogger.key_action) == controller.ADD.toString)
      assert(d.detail(SplunkLogger.key_tier) == controller.FRONTEND.toString)
      assert(d.detail(SplunkLogger.key_target) == controller.BIK.toString)
      assert(d.detail(SplunkLogger.key_period) == controller.CYP1.toString)
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
      val nonPbikEvent: DataEvent = DataEvent(
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
          "other key" -> "other data"
        )
      )
      val r: AuditResult          = await(controller.logSplunkEvent(nonPbikEvent))
      assert(r == Success)
    }
  }

  "When logging events, the SplunkLogger" should {
    "return a properly formatted DataEvent for Pbik errors" in new SetUp {
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        createAuthenticatedRequest(mockRequest, userId = Some("TEST_USER"))

      val d: DataEvent =
        controller.createErrorEvent(controller.FRONTEND, controller.EXCEPTION, "No PAYE Scheme found for user")

      assert(d.auditSource == SplunkLogger.pbik_audit_source)
      assert(d.detail.nonEmpty)
      assert(d.detail.contains(SplunkLogger.key_event_name))
      assert(d.detail(SplunkLogger.key_event_name) == SplunkLogger.pbik_event_name)
      assert(d.detail(SplunkLogger.key_empref) == empRef.toString)
      assert(d.detail(SplunkLogger.key_gateway_user) == "TEST_USER")
      assert(d.detail(SplunkLogger.key_error) == controller.EXCEPTION.toString)
      assert(d.detail(SplunkLogger.key_message) == "No PAYE Scheme found for user")
    }

    "return a properly formatted DataEvent for Pbik errors when no user_id" in new SetUp {
      implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] =
        createAuthenticatedRequest(mockRequest, userId = None)

      val d: DataEvent =
        controller.createErrorEvent(controller.FRONTEND, controller.EXCEPTION, "No PAYE Scheme found for user")

      assert(d.auditSource == SplunkLogger.pbik_audit_source)
      assert(d.detail.nonEmpty)
      assert(d.detail.contains(SplunkLogger.key_event_name))
      assert(d.detail(SplunkLogger.key_event_name) == SplunkLogger.pbik_event_name)
      assert(d.detail(SplunkLogger.key_empref) == empRef.toString)
      assert(d.detail(SplunkLogger.key_gateway_user) == SplunkLogger.pbik_no_ref)
      assert(d.detail(SplunkLogger.key_error) == controller.EXCEPTION.toString)
      assert(d.detail(SplunkLogger.key_message) == "No PAYE Scheme found for user")
    }
  }

}

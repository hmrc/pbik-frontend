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

package utils

import controllers.FakePBIKApplication
import models.{AuthenticatedRequest, EiLPerson, EiLPersonList, UserName}
import play.api.libs.Crypto
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.AuthorityUtils._
import support.TestAuthUser
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{LoggedInUser, Principal}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class SplunkLoggerSpec extends UnitSpec with FakePBIKApplication with TestAuthUser {

  val testList: List[EiLPerson] = List[EiLPerson](new EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None, 0))
  val testPersonList: EiLPersonList = EiLPersonList(testList)

  class TestSplunkLogger extends SplunkLogger /*with TestAuditConnector*/ {

    // Dont want to generate actual audit events.
    // If you want to test these, comment out this and it will use the auditConnector
    // If you want to test the auditConnectors disabled state, update the application config for the root level
    // Test section and use the following
    //    Test {
    //      auditing {
    //        enabled = true
    //        traceRequests = false
    //        consumer {
    //          baseUri {
    //            host = localhost
    //            port = 8100
    //          }
    //        }
    //      }
    //    }

    override def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Future[AuditResult] = {
      Future.successful(AuditResult.Success)
    }
  }

  class SetUp {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val controller = new TestSplunkLogger
    val msg = "Hello"

    def csrfToken: (String, String) = "csrfToken" -> Crypto.generateToken //"csrfToken"Name -> UnsignedTokenProvider.generateToken
    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken)

    def fakeAuthenticatedRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken).withHeaders()

    val pbikDataEvent = DataEvent(auditSource = SplunkLogger.pbik_audit_source, auditType = SplunkLogger.pbik_benefit_type, detail = Map(
      SplunkLogger.key_event_name -> SplunkLogger.pbik_event_name,
      SplunkLogger.key_gateway_user -> EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference ="taxOfficeReference").toString,
      SplunkLogger.key_tier -> controller.spTier.FRONTEND.toString,
      SplunkLogger.key_action -> controller.spAction.ADD.toString,
      SplunkLogger.key_target -> controller.spTarget.BIK.toString,
      SplunkLogger.key_period -> controller.spPeriod.CYP1.toString,
      SplunkLogger.key_message -> msg
    ))
  }

  "When logging events, the SplunkLogger" should {
    "return a properly formatted DataEvent for Pbik events" in new SetUp {
      running(fakeApplication) {
        val d: DataEvent = controller.createDataEvent(controller.spTier.FRONTEND,
          controller.spAction.ADD,
          controller.spTarget.BIK,
          controller.spPeriod.CYP1,
          msg = "Employer Added Bik to CY Plus 1",
          name = Option(UserName(Name(Some("TEST_USER"), None))),
          empRef = Some(models.EmpRef("taxOfficeNumber", "taxOfficeReference")))

        assert(d.auditSource == SplunkLogger.pbik_audit_source)
        assert(d.detail.nonEmpty)
        assert(d.detail.contains(SplunkLogger.key_event_name))
        assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
        assert(d.detail.get(SplunkLogger.key_empref).get == "taxOfficeNumber/taxOfficeReference")
        assert(d.detail.get(SplunkLogger.key_gateway_user).get == "TEST_USER")
        assert(d.detail.get(SplunkLogger.key_action).get == controller.spAction.ADD.toString)
        assert(d.detail.get(SplunkLogger.key_tier).get == controller.spTier.FRONTEND.toString)
        assert(d.detail.get(SplunkLogger.key_target).get == controller.spTarget.BIK.toString)
        assert(d.detail.get(SplunkLogger.key_period).get == controller.spPeriod.CYP1.toString)
      }
    }
  }

  "When logging events, the SplunkLogger without a ePaye account" should {
    "return a properly formatted DataEvent with a empref default for Pbik events" in new SetUp {
      running(fakeApplication) {
        val epayeAccount = None
        val accounts = Accounts(epaye = epayeAccount)
        val authority = ctAuthority("nonpayeId", "ctref")
        val user = LoggedInUser(userId = "nonpayeId", None, None, None, CredentialStrength.None, ConfidenceLevel.L50, oid = "testOId")
        val principal = Principal(name = Some("TEST_USER"), accounts)
        val d: DataEvent = controller.createDataEvent(controller.spTier.FRONTEND,
          controller.spAction.ADD,
          controller.spTarget.BIK,
          controller.spPeriod.CYP1,
          msg = "Employer Added Bik to CY Plus 1",
          name = Option(UserName(Name(Some("TEST_USER"), None))),
          empRef = None)

        assert(d.auditSource == SplunkLogger.pbik_audit_source)
        assert(d.detail.nonEmpty)
        assert(d.detail.contains(SplunkLogger.key_event_name))
        assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
        assert(d.detail.get(SplunkLogger.key_empref).get == SplunkLogger.pbik_no_ref)
        assert(d.detail.get(SplunkLogger.key_gateway_user).get == "TEST_USER")
        assert(d.detail.get(SplunkLogger.key_action).get == controller.spAction.ADD.toString)
        assert(d.detail.get(SplunkLogger.key_tier).get == controller.spTier.FRONTEND.toString)
        assert(d.detail.get(SplunkLogger.key_target).get == controller.spTarget.BIK.toString)
        assert(d.detail.get(SplunkLogger.key_period).get == controller.spPeriod.CYP1.toString)
      }
    }
  }

  "When logging events, the SplunkLogger" should {

    "complete successfully when sending a PBIK DataEvent" in new SetUp {
      running(fakeApplication) {

        val r: AuditResult = await(controller.logSplunkEvent(pbikDataEvent))
        assert(r == Success)
      }
    }
  }

  "When logging events, the SplunkLogger" should {
    "complete successfully when sending a general DataEvent" in new SetUp {
      running(fakeApplication) {

        val nonPbikEvent = DataEvent(auditSource = "TEST", auditType = "TEST-AUDIT-TYPE", detail = Map(
          "event" -> "APPLY-THE-TEST",
          "id" -> "Test Id",
          "key1" -> "val1",
          "key2" -> "val2",
          "key3" -> "val3",
          "key4" -> "val4",
          "key5" -> "val5",
          "other key" -> "other data"))
        val r: AuditResult = await(controller.logSplunkEvent(nonPbikEvent))
        assert(r == Success)
      }
    }
  }

  "When logging events, the SplunkLogger" should {

    "return a properly formatted DataEvent for Pbik errors" in new SetUp {
      running(fakeApplication) {

        implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
          models.EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference ="taxOfficeReference"),
          UserName(Name(Some("TEST_USER"),None)),
          FakeRequest()
        )

        val d: DataEvent = controller.createErrorEvent(controller.spTier.FRONTEND,
          controller.spError.EXCEPTION,
          "No PAYE Scheme found for user")

        assert(d.auditSource == SplunkLogger.pbik_audit_source)
        assert(d.detail.nonEmpty)
        assert(d.detail.contains(SplunkLogger.key_event_name))
        assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
        assert(d.detail.get(SplunkLogger.key_empref).get == EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference ="taxOfficeReference").toString)
        assert(d.detail.get(SplunkLogger.key_gateway_user).get == "TEST_USER")
        assert(d.detail.get(SplunkLogger.key_error).get == controller.spError.EXCEPTION.toString)
        assert(d.detail.get(SplunkLogger.key_message).get == "No PAYE Scheme found for user")
      }
    }
  }

  "When logging events, the SplunkLogger" should {
    "mark the empref with a default if one is not present" in new SetUp {
      running(fakeApplication) {

        implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
          models.EmpRef.empty,
          UserName(Name(Some("TEST_USER"),None)),
          FakeRequest()
        )


        val d: DataEvent = controller.createErrorEvent(controller.spTier.FRONTEND,
          controller.spError.EXCEPTION,
          msg="No Empref")

        assert(d.auditSource == SplunkLogger.pbik_audit_source)
        assert(d.detail.nonEmpty)
        assert(d.detail.contains(SplunkLogger.key_event_name))
        assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
        assert(d.detail.get(SplunkLogger.key_empref).get == SplunkLogger.pbik_no_ref)
        assert(d.detail.get(SplunkLogger.key_gateway_user).get == "TEST_USER")
        assert(d.detail.get(SplunkLogger.key_error).get == controller.spError.EXCEPTION.toString)
        assert(d.detail.get(SplunkLogger.key_message).get == "No Empref")
      }
    }
  }

  "When logging events, the SplunkLogger" should {
    "return the correct splunk field names" in new SetUp {
      running(fakeApplication) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          // val t1 = spTier.FRONTEND
          assert(spTier.toString != null)
          val t2: spAction.Value = spAction.ADD
          assert(spAction.toString != null)
          val t3: spError.Value = spError.EXCEPTION
          assert(spError.toString != null)
          val t4: spPeriod.Value = spPeriod.BOTH
          assert(spPeriod.toString != null)
          val t5: spTarget.Value = spTarget.BIK
          assert(spTarget.toString != null)
        }
      }
    }
  }

  "When extracting a Nino from an empty list the conroller" should {
    "return a not found label" in new SetUp {
      running(fakeApplication) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert(extractListNino(List[EiLPerson]()) == SplunkLogger.pbik_no_ref)
        }
      }
    }
  }

  "When extracting a Nino from a list the conroller" should {
    "return a not found label" in new SetUp {
      running(fakeApplication) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert(extractListNino(testList) == "AB111111")
        }
      }
    }
  }

  "When extracting a Nino from a Person list the conroller" should {
    "return a not found label" in new SetUp {
      running(fakeApplication) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert(extractPersonListNino(testPersonList) == "AB111111")
        }
      }
    }
  }

  "When extracting a Nino from an empty Person List the conroller" should {
    "return a not found label" in new SetUp {
      running(fakeApplication) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert(extractPersonListNino(EiLPersonList(List[EiLPerson]())) == SplunkLogger.pbik_no_ref)
        }
      }
    }
  }

  "When extracting the Government Gateway Id from a valid user the controller" should {
    "return the Government Gateway name" in new SetUp {
      running(fakeApplication) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
            models.EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference ="taxOfficeReference"),
            UserName(Name(Some("TEST_USER"),None)),
            FakeRequest()
          )
          assert(extractGovernmentGatewayString == "TEST_USER")
        }
      }
    }
  }

  "When extracting the Government Gateway Id from an invalid user the controller" should {
    "return the default" in new SetUp {
      running(fakeApplication) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
            models.EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference ="taxOfficeReference"),
            UserName(Name(None,None)),
            FakeRequest()
          )
          assert(extractGovernmentGatewayString == SplunkLogger.pbik_no_ref)
        }
      }
    }
  }

}

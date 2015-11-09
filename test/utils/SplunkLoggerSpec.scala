/*
 * Copyright 2015 HM Revenue & Customs
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

import models.{EiLPersonList, EiLPerson}
import support.AuthorityUtils._
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.UnsignedTokenProvider
import support.TestAuthUser
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditResult}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.frontend.auth.{Principal, LoggedInUser, AuthContext}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{LevelOfAssurance, Authority, Accounts, EpayeAccount}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.LevelOfAssurance._
import uk.gov.hmrc.play.test.{UnitSpec}
import scala.concurrent.Future

class SplunkLoggerSpec extends UnitSpec with MockitoSugar with TestAuthUser {

  val testList = List[EiLPerson](new EiLPerson("AB111111","Adam", None ,"Smith",None, Some("01/01/1980"),Some("male"), None, 0))
  val testPersonList = EiLPersonList(testList)

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

    override def logSplunkEvent(dataEvent:DataEvent)(implicit hc:HeaderCarrier, ac: AuthContext):Future[AuditResult] = {
      Future.successful(AuditResult.Success)
    }

  }

    class SetUp {
      implicit val hc = HeaderCarrier()
      val epayeAccount = Some(EpayeAccount(empRef = EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference ="taxOfficeReference" ), link =""))
      val accounts = Accounts(epaye = epayeAccount)
      val authority = epayeAuthority("testUserId", "emp/ref")
      val user = LoggedInUser(userId = "testUserId", None, None, None, LevelOfAssurance(2))
      val principal = Principal(name = Some("TEST_USER"), accounts)

      implicit def fakeAuthContext = new AuthContext(user, principal, None)

      val controller = new TestSplunkLogger
      val msg = "Hello"
      def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
      def fakeRequest = FakeRequest().withSession(csrfToken)
      def fakeAuthenticatedRequest = FakeRequest().withSession(csrfToken).withHeaders()
      val pbikDataEvent = DataEvent(auditSource = SplunkLogger.pbik_audit_source, auditType = SplunkLogger.pbik_audit_type, detail = Map(
        SplunkLogger.key_event_name -> SplunkLogger.pbik_event_name,
        SplunkLogger.key_gateway_user -> fakeAuthContext.principal.accounts.epaye.get.empRef.toString,
        SplunkLogger.key_tier -> controller.spTier.FRONTEND.toString,
        SplunkLogger.key_action -> controller.spAction.ADD.toString,
        SplunkLogger.key_target -> controller.spTarget.BIK.toString,
        SplunkLogger.key_period -> controller.spPeriod.CYP1.toString,
        SplunkLogger.key_message -> msg
      ))
    }

  "When logging events, the SplunkLogger " should {
    "return a properly formatted DataEvent for Pbik events " in new SetUp {
      running(new FakeApplication()) {
        val d: DataEvent = controller.createDataEvent(controller.spTier.FRONTEND,
                                                  controller.spAction.ADD,
                                                  controller.spTarget.BIK,
                                                  controller.spPeriod.CYP1,
                                                  "Employer Added Bik to CY Plus 1")(fakeAuthContext)

        assert(d.auditSource == SplunkLogger.pbik_audit_source)
        assert(d.auditType == SplunkLogger.pbik_audit_type)
        assert(d.detail.size > 0)
        assert(d.detail.contains(SplunkLogger.key_event_name))
        assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
        assert(d.detail.get(SplunkLogger.key_empref).get == fakeAuthContext.principal.accounts.epaye.get.empRef.toString)
        assert(d.detail.get(SplunkLogger.key_gateway_user).get == fakeAuthContext.principal.name.get)
        assert(d.detail.get(SplunkLogger.key_action).get == controller.spAction.ADD.toString)
        assert(d.detail.get(SplunkLogger.key_tier).get == controller.spTier.FRONTEND.toString)
        assert(d.detail.get(SplunkLogger.key_target).get == controller.spTarget.BIK.toString)
        assert(d.detail.get(SplunkLogger.key_period).get == controller.spPeriod.CYP1.toString)
//        assert(d.auditSource == controller.pbikAuditSource)
      }
    }
  }

  "When logging events, the SplunkLogger without a ePaye account " should {
    "return a properly formatted DataEvent with a empref default for Pbik events " in new SetUp {
      running(new FakeApplication()) {
        val epayeAccount = None
        val accounts = Accounts(epaye = epayeAccount)
        val authority = ctAuthority("nonpayeId", "ctref")
        val user = LoggedInUser(userId = "nonpayeId", None, None, None, LevelOfAssurance(2))
        val principal = Principal(name = Some("TEST_USER"), accounts)

        implicit def nonPayeUser = new AuthContext(user, principal, None)
        //implicit def nonPayeUser = User(userId = "nonpayeId", userAuthority = ctAuthority("nonpayeId", "ctref"), nameFromGovernmentGateway = Some("TEST_USER"), decryptedToken = None)

        val d: DataEvent = controller.createDataEvent(controller.spTier.FRONTEND,
          controller.spAction.ADD,
          controller.spTarget.BIK,
          controller.spPeriod.CYP1,
          "Employer Added Bik to CY Plus 1")(nonPayeUser)

        assert(d.auditSource == SplunkLogger.pbik_audit_source)
        assert(d.auditType == SplunkLogger.pbik_audit_type)
        assert(d.detail.size > 0)
        assert(d.detail.contains(SplunkLogger.key_event_name))
        assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
        assert(d.detail.get(SplunkLogger.key_empref).get ==  SplunkLogger.pbik_no_ref)
        assert(d.detail.get(SplunkLogger.key_gateway_user).get == fakeAuthContext.principal.name.get)
        assert(d.detail.get(SplunkLogger.key_action).get == controller.spAction.ADD.toString)
        assert(d.detail.get(SplunkLogger.key_tier).get == controller.spTier.FRONTEND.toString)
        assert(d.detail.get(SplunkLogger.key_target).get == controller.spTarget.BIK.toString)
        assert(d.detail.get(SplunkLogger.key_period).get == controller.spPeriod.CYP1.toString)
        //        assert(d.auditSource == controller.pbikAuditSource)
      }
    }
  }



  "When logging events, the SplunkLogger " should {

    "complete successfully when sending a PBIK DataEvent " in new SetUp {
      running(new FakeApplication()) {

        val r: AuditResult = await(controller.logSplunkEvent(pbikDataEvent))
        assert(r == Success)
      }
    }
  }

  "When logging events, the SplunkLogger " should {
    "complete successfully when sending a general DataEvent " in new SetUp {
      running(new FakeApplication()) {

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

  "When logging events, the SplunkLogger " should {

    "return a properly formatted DataEvent for Pbik errors " in new SetUp {
      running(new FakeApplication()) {
        val d: DataEvent = controller.createErrorEvent(controller.spTier.FRONTEND,
          controller.spError.EXCEPTION,
          "No PAYE Scheme found for user")(fakeAuthContext)

        assert(d.auditSource == SplunkLogger.pbik_audit_source)
        assert(d.auditType == SplunkLogger.pbik_audit_type)
        assert(d.detail.size > 0)
        assert(d.detail.contains(SplunkLogger.key_event_name))
        assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
        assert(d.detail.get(SplunkLogger.key_empref).get == fakeAuthContext.principal.accounts.epaye.get.empRef.toString)
        assert(d.detail.get(SplunkLogger.key_gateway_user).get == fakeAuthContext.principal.name.get)
        assert(d.detail.get(SplunkLogger.key_error).get == controller.spError.EXCEPTION.toString)
        assert(d.detail.get(SplunkLogger.key_message).get == "No PAYE Scheme found for user")
      }
    }
  }

  "When logging events, the SplunkLogger " should {
    "mark the empref with a default if one is not present " in new SetUp {
      running(new FakeApplication()) {
        val epayeAccount = None
        val accounts = Accounts(epaye = epayeAccount)
        val authority = ctAuthority("nonpayeId", "ctref")
        val user = LoggedInUser(userId = "nonpayeId", None, None, None, LevelOfAssurance(2))
        val principal = Principal(name = Some("TEST_USER"), accounts)

        implicit def nonPayeUser = new AuthContext(user, principal, None)

        val d: DataEvent = controller.createErrorEvent(controller.spTier.FRONTEND,
          controller.spError.EXCEPTION,
          "No Empref")(nonPayeUser)

        assert(d.auditSource == SplunkLogger.pbik_audit_source)
        assert(d.auditType == SplunkLogger.pbik_audit_type)
        assert(d.detail.size > 0)
        assert(d.detail.contains(SplunkLogger.key_event_name))
        assert(d.detail.get(SplunkLogger.key_event_name).get == SplunkLogger.pbik_event_name)
        assert(d.detail.get(SplunkLogger.key_empref).get == SplunkLogger.pbik_no_ref)
        assert(d.detail.get(SplunkLogger.key_gateway_user).get == nonPayeUser.principal.name.get)
        assert(d.detail.get(SplunkLogger.key_error).get == controller.spError.EXCEPTION.toString)
        assert(d.detail.get(SplunkLogger.key_message).get == "No Empref")
      }
    }
  }

  "When logging events, the SplunkLogger " should {
    "return the correct splunk field names " in new SetUp {
      running(new FakeApplication()) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
         // val t1 = spTier.FRONTEND
          assert(spTier.toString != null)
          val t2 = spAction.ADD
          assert(spAction.toString!=null)
          val t3 = spError.EXCEPTION
          assert(spError.toString != null)
          val t4 = spPeriod.BOTH
          assert(spPeriod.toString != null)
          val t5 = spTarget.BIK
          assert(spTarget.toString != null)
        }
      }
    }
  }

  "When extracting a Nino from an empty list the conroller " should {
    "return a not found label " in new SetUp {
      running(new FakeApplication()) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert (extractListNino(List[EiLPerson]()) == SplunkLogger.pbik_no_ref)
        }
      }
    }
  }

  "When extracting a Nino from a list the conroller " should {
    "return a not found label " in new SetUp {
      running(new FakeApplication()) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert (extractListNino(testList) == "AB111111")
        }
      }
    }
  }

  "When extracting a Nino from a Person list the conroller " should {
    "return a not found label " in new SetUp {
      running(new FakeApplication()) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert (extractPersonListNino(testPersonList) == "AB111111")
        }
      }
    }
  }

  "When extracting a Nino from an empty Person List the conroller " should {
    "return a not found label " in new SetUp {
      running(new FakeApplication()) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert (extractPersonListNino(new EiLPersonList(List[EiLPerson]())) == SplunkLogger.pbik_no_ref)
        }
      }
    }
  }

  "When extracting the Government Gateway Id from a valid user the controller " should {
    "return the Government Gateway name " in new SetUp {
      running(new FakeApplication()) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert (extractGovernmentGatewayString == fakeAuthContext.principal.name.get)
        }
      }
    }
  }

  "When extracting the Government Gateway Id from an invalid user the controller " should {
    "return the default " in new SetUp {
      running(new FakeApplication()) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          val badUser = createDummyNonGatewayUser("NON_GW_USER")
          assert (extractGovernmentGatewayString(badUser) == SplunkLogger.pbik_no_ref)
        }
      }
    }
  }

  "When extracting the Empref Id from a valid user the controller " should {
    "return the Empref " in new SetUp {
      running(new FakeApplication()) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          assert (extractEmprefString == fakeAuthContext.principal.accounts.epaye.get.empRef.toString)
        }
      }
    }
  }

  "When extracting the Empref Id from an invalid user the controller " should {
    "return the default " in new SetUp {
      running(new FakeApplication()) {
        new {
          val testSplunker = "testSplunker"
        } with SplunkLogger {
          val badUser = createDummyNonEpayeUser("NON_GW_USER")
          assert (extractEmprefString(badUser) == SplunkLogger.pbik_no_ref)
        }
      }
    }
  }

}

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

package support

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{SplunkLogger, TaxDateUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestSplunkLogger @Inject() (taxDateUtils: TaxDateUtils, auditConnector: AuditConnector)(implicit
  ec: ExecutionContext
) extends SplunkLogger(
      taxDateUtils,
      auditConnector
    ) /*with TestAuditConnector*/ {

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

  override def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Future[AuditResult] =
    Future.successful(AuditResult.Success)
}

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

import controllers.auth.AuthenticationConnector
import models.{EiLPerson, EiLPersonList, EmpRef, UserName}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.frontend.auth.connectors.domain.EpayeAccount
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Logger
import uk.gov.hmrc.auth.core.retrieve.Name

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object SplunkLogger {

  val pbik_audit_source = "pbik-frontend"
  val pbik_benefit_type = "benefit-event"
  val pbik_exclude_type = "exclusion-event"
  val pbik_error_type = "error-event"
  val pbik_event_name = "PBIK"
  val pbik_no_ref = "Not available"

  val key_event_name = "event"
  val key_gateway_user = "gatewayUser"
  val key_empref = "empref"
  val key_tier = "tier"
  val key_nino = "nino"
  val key_action = "action"
  val key_target = "target"
  val key_period = "period"
  val key_iabd = "iabd"
  val key_message = "message"
  val key_remove_reason = "removeReason"
  val key_remove_reason_desc = "removeReasonDesc"
  val key_error = "error"

}

trait SplunkLogger extends AuthenticationConnector {

  object spTier extends Enumeration {
    type spTier = Value
    val FRONTEND, GATEWAY = Value

    override def toString: String = Value.toString
  }

  object spAction extends Enumeration {
    type spAction = Value
    val LOGIN, VIEW, ADD, REMOVE = Value

    override def toString: String = Value.toString
  }

  object spTarget extends Enumeration {
    type spTarget = Value
    val BIK, EIL = Value

    override def toString: String = Value.toString
  }

  object spPeriod extends Enumeration {
    type spPeriod = Value
    val CY, CYP1, BOTH = Value

    override def toString: String = Value.toString
  }

  object spError extends Enumeration {
    type spError = Value
    val SCHEDULED_OUTAGE, EXCEPTION = Value

    override def toString: String = Value.toString
  }

  import spTier._, spAction._, spError._, spTarget._, spPeriod._, SplunkLogger._

  /**
    * Method creates a PBIK Specific DataEvent which will be sent to splunk so product owners
    * can get granularity on the actions a user of PBIks is under-taking.
    * For example, an audit message may supply createEvent(tier.FRONTEND, action.ADD, Target.EIL, Period.CY ) which
    * would allow the Product Owners to know the specific Employer ( as determined by the implicit User ) has
    * used the Frontend to Add an Exclusion for the current year ( the details of which may be in the message )
    * By creating the fixed format, we can ustilise Splunk's query and daashboards to give the product owner some
    * easily accessible metrics.
    *
    * @param tier   - either the FRONTEND MicroService or the GATEWAY Microservice response
    * @param action - LOGIN, LIST, ADD or REMOVE
    * @param target - BIK, EIL
    * @param period - CY, CYP1, BOTH ( actions such as overview screen apply to both CY & CYP1 )
    * @param msg    - free text message. Note - ensure no personal or sensitive details are included )
    * @param ac     - the implicit Auth Context involved in the audit action
    * @return - a properly formed PBIK DataEvent which may be sent using the logSplunkEvent method.
    */
  def createDataEvent(tier: spTier, action: spAction, target: spTarget, period: spPeriod, msg: String, nino: Option[String] = None, iabd: Option[String] = None, removeReason: Option[String] = None, removeReasonDesc: Option[String] = None, name: Option[UserName], empRef: EmpRef): DataEvent
  = {

    val derivedAuditType = target match {
      case spTarget.BIK => pbik_benefit_type
      case spTarget.EIL => pbik_exclude_type
    }

    val entityIABD = if (iabd.isDefined) Seq((key_iabd -> iabd.get)) else Nil
    val entityNINO = if (nino.isDefined) Seq((key_nino -> nino.get)) else Nil
    val entityRemoveReason = if (removeReason.isDefined) Seq((key_remove_reason -> removeReason.get)) else Nil
    val entityRemoveReasonDesc = if (removeReasonDesc.isDefined) Seq((key_remove_reason_desc -> removeReasonDesc.get)) else Nil

    val entities = Seq(key_event_name -> pbik_event_name,
      key_gateway_user -> name.map(_.toString).getOrElse(pbik_no_ref),
      key_empref -> empRef.toString,
      key_tier -> tier.toString,
      key_action -> action.toString,
      key_target -> target.toString,
      key_period -> period.toString,
      key_message -> msg

    ) ++ entityIABD ++ entityNINO ++ entityRemoveReason ++ entityRemoveReasonDesc

    DataEvent(auditSource = pbik_audit_source, auditType = derivedAuditType, detail = Map(entities: _*))
  }

  /**
    *
    * Method creates a PBIK Specific Error which will be sent to splunk so product owners
    * can get granularity on the actions a user cannot undertake.
    *
    * @param tier - either the FRONTEND MicroService or GATEWAY Microservice response
    * @param msg  - free text message. Note - ensure no personal or sensitive details are included )
    * @param ac
    * @return A DataEvent with the PBIK specific error payload which may be sent using the logSplunkEvent method.
    */
  def createErrorEvent(tier: spTier, error: spError, msg: String)
                      (implicit ac: AuthContext): DataEvent = {

    DataEvent(auditSource = pbik_audit_source, auditType = pbik_error_type,
      detail = Map(
        key_event_name -> pbik_event_name,
        key_gateway_user -> ac.principal.name.getOrElse(pbik_no_ref),
        key_empref -> {
          ac.principal.accounts.epaye.isDefined match {
            case true => ac.principal.accounts.epaye.get.empRef.toString
            case false => pbik_no_ref
          }
        },
        key_tier -> tier.toString,
        key_error -> error.toString,
        key_message -> msg
      ))
  }

  /**
    * This sends explicit DataEvents to Splunk for auditing specific actions or events
    *
    * @param dataEvent The Event which will be persisted to Splunk and may contain an audit payload or an error payload
    * @param hc        HeaderCarrier infomration
    * @param ac        Auth Context for whom the audit is about
    * @return an AuditResult which will determine if the auditing was successful or not
    */
  def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    auditConnector.sendEvent(dataEvent)
  }

  def taxYearToSpPeriod(year: Int): spPeriod.Value = {
    TaxDateUtils.isCurrentTaxYear(year) match {
      case true => spPeriod.CY
      case false => spPeriod.CYP1
    }
  }

  def extractPersonListNino(headlist: EiLPersonList): String = {
    headlist.active.headOption match {
      case Some(x) => x.nino
      case None => SplunkLogger.pbik_no_ref
    }
  }

  def extractListNino(headlist: List[EiLPerson]): String = {
    headlist.headOption match {
      case Some(x) => x.nino
      case None => SplunkLogger.pbik_no_ref
    }
  }

  def extractGovernmentGatewayString(implicit ac: AuthContext): String = {
    ac.principal.name match {
      case e: Some[String] => e.get
      case None => pbik_no_ref
    }
  }

}

/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import models._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import SplunkLogger._

object SplunkLogger {

  val pbik_audit_source = "pbik-frontend"
  val pbik_benefit_type = "benefit-event"
  val pbik_exclude_type = "exclusion-event"
  val pbik_error_type   = "error-event"
  val pbik_event_name   = "PBIK"
  val pbik_no_ref       = "Not available"

  val key_event_name         = "event"
  val key_gateway_user       = "gatewayUser"
  val key_empref             = "empref"
  val key_tier               = "tier"
  val key_nino               = "nino"
  val key_action             = "action"
  val key_target             = "target"
  val key_period             = "period"
  val key_iabd               = "iabd"
  val key_message            = "message"
  val key_remove_reason      = "removeReason"
  val key_remove_reason_desc = "removeReasonDesc"
  val key_error              = "error"

}

@Singleton
class SplunkLogger @Inject() (taxDateUtils: TaxDateUtils, val auditConnector: AuditConnector) {

  sealed trait SpTier
  case object FRONTEND extends SpTier
  case object GATEWAY extends SpTier

  sealed trait SpAction
  case object LOGIN extends SpAction
  case object VIEW extends SpAction
  case object ADD extends SpAction
  case object REMOVE extends SpAction

  sealed trait SpTarget
  case object BIK extends SpTarget
  case object EIL extends SpTarget

  sealed trait SpPeriod
  case object CY extends SpPeriod
  case object CYP1 extends SpPeriod
  case object BOTH extends SpPeriod

  sealed trait SpError
  case object SCHEDULED_OUTAGE extends SpError
  case object EXCEPTION extends SpError

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
    * @return - a properly formed PBIK DataEvent which may be sent using the logSplunkEvent method.
    */
  def createDataEvent(
    tier: SpTier,
    action: SpAction,
    target: SpTarget,
    period: SpPeriod,
    msg: String,
    nino: Option[String] = None,
    iabd: Option[String] = None,
    removeReason: Option[String] = None,
    removeReasonDesc: Option[String] = None,
    name: Option[UserName],
    empRef: Option[EmpRef]
  ): DataEvent = {

    val derivedAuditType = target match {
      case BIK => pbik_benefit_type
      case EIL => pbik_exclude_type
    }

    val entityIABD             = if (iabd.isDefined) Seq(key_iabd -> iabd.get) else Nil
    val entityNINO             = if (nino.isDefined) Seq(key_nino -> nino.get) else Nil
    val entityRemoveReason     = if (removeReason.isDefined) Seq(key_remove_reason -> removeReason.get) else Nil
    val entityRemoveReasonDesc =
      if (removeReasonDesc.isDefined) Seq(key_remove_reason_desc -> removeReasonDesc.get) else Nil

    val entities               = Seq(
      key_event_name   -> pbik_event_name,
      key_gateway_user -> name.map(_.toString).getOrElse(pbik_no_ref),
      key_empref       -> empRef.map(_.toString).getOrElse(pbik_no_ref),
      key_tier         -> tier.toString,
      key_action       -> action.toString,
      key_target       -> target.toString,
      key_period       -> period.toString,
      key_message      -> msg
    ) ++ entityIABD ++ entityNINO ++ entityRemoveReason ++ entityRemoveReasonDesc

    DataEvent(auditSource = pbik_audit_source, auditType = derivedAuditType, detail = Map(entities: _*))
  }

  /**
    * Method creates a PBIK Specific Error which will be sent to splunk so product owners
    * can get granularity on the actions a user cannot undertake.
    *
    * @param tier - either the FRONTEND MicroService or GATEWAY Microservice response
    * @param msg  - free text message. Note - ensure no personal or sensitive details are included )
    * @return A DataEvent with the PBIK specific error payload which may be sent using the logSplunkEvent method.
    */
  def createErrorEvent(tier: SpTier, error: SpError, msg: String)(implicit
    request: AuthenticatedRequest[_]
  ): DataEvent =
    DataEvent(
      auditSource = pbik_audit_source,
      auditType = pbik_error_type,
      detail = Map(
        key_event_name   -> pbik_event_name,
        key_gateway_user -> request.name.getOrElse(pbik_no_ref),
        key_empref       -> request.empRef.getOrElse(pbik_no_ref),
        key_tier         -> tier.toString,
        key_error        -> error.toString,
        key_message      -> msg
      )
    )

  /**
    * This sends explicit DataEvents to Splunk for auditing specific actions or events
    *
    * @param dataEvent The Event which will be persisted to Splunk and may contain an audit payload or an error payload
    * @param hc        HeaderCarrier infomration
    * @return an AuditResult which will determine if the auditing was successful or not
    */
  def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendEvent(dataEvent)

  def taxYearToSpPeriod(year: Int): SpPeriod =
    if (taxDateUtils.isCurrentTaxYear(year)) {
      CY
    } else {
      CYP1
    }

  def extractPersonListNino(headlist: EiLPersonList): String =
    headlist.active.headOption match {
      case Some(x) => x.nino
      case None    => SplunkLogger.pbik_no_ref
    }

  def extractListNino(headlist: List[EiLPerson]): String =
    headlist.headOption match {
      case Some(x) => x.nino
      case None    => SplunkLogger.pbik_no_ref
    }

  def extractGovernmentGatewayString(implicit request: AuthenticatedRequest[_]): String =
    request.name.getOrElse(pbik_no_ref)

}

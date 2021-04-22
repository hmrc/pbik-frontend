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

import java.net.URLEncoder

import config.PbikAppConfig
import connectors._
import controllers.actions.{AuthAction, NoSessionCheckAction}

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.Logger
import play.twirl.api.Html
import services.HelpAndContactSubmissionService
import uk.gov.hmrc.http.{Request => _}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.helpcontact.{ConfirmHelpContact, HelpContact}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HelpAndContactController @Inject()(
  override val messagesApi: MessagesApi,
  cc: MessagesControllerComponents,
  formPartialProvider: FormPartialProvider,
  helpAndContactSubmissionService: HelpAndContactSubmissionService,
  val authenticate: AuthAction,
  val noSessionCheck: NoSessionCheckAction,
  pbikAppConfig: PbikAppConfig,
  helpContactView: HelpContact,
  confirmHelpContactView: ConfirmHelpContact)(implicit val ec: ExecutionContext)
    extends FrontendController(cc) with I18nSupport {

  val contactFrontendPartialBaseUrl: String = pbikAppConfig.contactFrontendService
  val contactFormServiceIdentifier: String = pbikAppConfig.contactFormServiceIdentifier

  private lazy val submitUrl = routes.HelpAndContactController.submitContactHmrcForm().url
  private lazy val contactHmrcFormPartialUrl =
    s"$contactFrontendPartialBaseUrl/contact/contact-hmrc/form?service=$contactFormServiceIdentifier&submitUrl=${urlEncode(submitUrl)}&renderFormOnly=true"
  private lazy val contactHmrcSubmitPartialUrl =
    s"$contactFrontendPartialBaseUrl/contact/contact-hmrc/form?resubmitUrl=${urlEncode(submitUrl)}&renderFormOnly=true"

  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  private val TICKET_ID = "ticketId"

  def onPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck) { implicit request =>
    Ok(
      helpContactView(
        contactHmrcFormPartialUrl,
        None,
        empRef = request.empRef,
        formPartialProvider = formPartialProvider))
  }

  def submitContactHmrcForm: Action[AnyContent] = (authenticate andThen noSessionCheck).async { implicit request =>
    val successRedirect = routes.HelpAndContactController.confirmationContactHmrc()
    val failedValidationResponseContent = (body: Html) =>
      helpContactView(
        contactHmrcFormPartialUrl,
        Some(body),
        empRef = request.empRef,
        formPartialProvider = formPartialProvider)
    request.body.asFormUrlEncoded
      .map { formData =>
        helpAndContactSubmissionService.submitContactHmrc(contactHmrcSubmitPartialUrl, formData).map { resp =>
          resp.status match {
            case OK => Redirect(successRedirect).withSession(request.session + (TICKET_ID -> resp.body))
            case BAD_REQUEST =>
              Logger.warn(
                "[HelpAndContactController][submitContactHmrcForm] Submit contact form: received bad request response")
              BadRequest(failedValidationResponseContent(Html(resp.body)))
            case INTERNAL_SERVER_ERROR => {
              Logger.error(
                s"[HelpAndContactController][submitContactHmrcForm] Submit contact form internal error 500, ${resp.body}")
              InternalServerError(Html(resp.body))
            }
            case status =>
              Logger.error(
                s"[HelpAndContactController][submitContactHmrcForm] Unexpected status code received when submitting contact HMRC form: $status")
              throw new Exception(s"Unexpected status code from contact HMRC form: $status")
          }
        }
      }
      .getOrElse {
        Logger.warn("[HelpAndContactController][submitContactHmrcForm] Trying to submit an empty contact form")
        Future.successful(InternalServerError)
      }
  }

  def confirmationContactHmrc: Action[AnyContent] = (authenticate andThen noSessionCheck) { implicit request =>
    Ok(confirmHelpContactView(empRef = request.empRef))
  }

}

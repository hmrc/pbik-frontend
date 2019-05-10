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

package connectors

import javax.inject.Inject
import play.api.Mode.Mode
import play.api.mvc.{AnyContent, Request, Results}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpReads, HttpResponse, Request => _}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ContactFrontendConnector @Inject()(client: HttpClient,
                                         configuration: Configuration,
                                         environment: Environment) extends ServicesConfig
        with Results {

  val mode: Mode = environment.mode
  val runModeConfiguration: Configuration = configuration

  lazy val serviceBase = s"${baseUrl("contact-frontend")}/contact"

  def getHelpPartial(implicit hc: HeaderCarrier): Future[String] = {

    val url = s"$serviceBase/problem_reports"

    client.GET[String](url) recover {
      case e: BadGatewayException =>
        Logger.error(s"[ContactFrontendConnector] ${e.message}", e)
        ""
    }
  }

  def submitContactHmrc(formUrl: String, formData: Map[String, Seq[String]])(implicit request: Request[AnyContent], ec: ExecutionContext): Future[HttpResponse] = {
      client.POSTForm[HttpResponse](formUrl, formData)(rds = PartialsFormReads.readPartialsForm, hc = partialsReadyHeaderCarrier, ec = ec)
  }

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc1 = PBIKHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    PBIKHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc1)
  }

}

object PartialsFormReads {
  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = response
  }
}

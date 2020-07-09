/*
 * Copyright 2020 HM Revenue & Customs
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

import config.Service
import javax.inject.Inject
import play.api.mvc.{AnyContent, Request}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpReads, HttpResponse, Request => _}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ContactFrontendConnector @Inject()(
  val client: HttpClient,
  configuration: Configuration,
  pBIKHeaderCarrierForPartialsConverter: PBIKHeaderCarrierForPartialsConverter) {

  lazy val serviceBase = s"${configuration.get[Service]("microservice.services.contact-frontend")}/contact"

  def getHelpPartial(implicit hc: HeaderCarrier): Future[String] = {

    val url = s"$serviceBase/problem_reports"

    client.GET[String](url) recover {
      case ex: BadGatewayException =>
        Logger.error(
          s"[ContactFrontendConnector][getHelpPartial] A bad gateway exception occurred when calling $url - ${ex.message}",
          ex)
        ""
    }
  }

  def submitContactHmrc(formUrl: String, formData: Map[String, Seq[String]])(
    implicit request: Request[AnyContent],
    ec: ExecutionContext): Future[HttpResponse] =
    client.POSTForm[HttpResponse](formUrl, formData)(
      rds = PartialsFormReads.readPartialsForm,
      hc = partialsReadyHeaderCarrier,
      ec = ec)

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc1 = pBIKHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    pBIKHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc1)
  }

}

object PartialsFormReads {
  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }
}

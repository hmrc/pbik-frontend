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

package connectors

import models.EmpRef
import models.agent.Client
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentPayeConnector @Inject() (http: HttpClient, servicesConfig: ServicesConfig) extends Logging {

  def getClient(agentCode: Option[String], empRef: EmpRef)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[Client]] =
    if (agentCode.isEmpty) {
      logger.warn("[AgentPayeConnector][getClient] agentCode is empty")
      Future.successful(None)
    } else {
      val url = s"${servicesConfig.baseUrl("agent-paye")}/agent/${agentCode.get}/client/${empRef.encodedEmpRef}"
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK                   =>
              response.json.validate[Client] match {
                case JsSuccess(data, _) => Some(data)
                case JsError(fail)      =>
                  logger.warn(s"[AgentPayeConnector][getClient] Unable to parse response : $fail")
                  None
              }
            case ACCEPTED | NOT_FOUND => None
            case httpStatusCode       =>
              logger.warn(s"[AgentPayeConnector][getClient] GET $url returned $httpStatusCode")
              None
          }
        }
        .recover { case ex: Exception =>
          logger.warn(s"[AgentPayeConnector][getClient] ${ex.getMessage}")
          None
        }
    }
}

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

import config.Service
import models._
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json._
import play.api.mvc.Request
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.Exceptions.GenericServerErrorException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PbikConnector @Inject() (client: HttpClient, configuration: Configuration)(implicit ec: ExecutionContext)
    extends Logging {

  private val maxEmptyBodyLength: Int = 4
  private val baseUrl: String         = s"${configuration.get[Service]("microservice.services.pbik")}/epaye"

  def getRegisteredBiks(
    empRef: EmpRef,
    year: Int
  )(implicit hc: HeaderCarrier): Future[BikResponse] =
    client
      .GET(s"$baseUrl/${empRef.encodedEmpRef}/$year")
      .map(implicit response => BikResponse(responseHeaders, validateResponses("getRegisteredBiks").json.as[List[Bik]]))

  def getAllAvailableBiks(year: Int)(implicit hc: HeaderCarrier): Future[List[Bik]] =
    client
      .GET(s"$baseUrl/$year/getbenefittypes")
      .map(implicit response => validateResponses("getAllAvailableBiks").json.as[List[Bik]])

  def getAllExcludedEiLPersonForBik(iabdString: String, empRef: EmpRef, year: Int)(implicit
    hc: HeaderCarrier
  ): Future[List[EiLPerson]] =
    client
      .GET(s"$baseUrl/${empRef.encodedEmpRef}/$year/${Bik.asNPSTypeValue(iabdString)}/exclusion")
      .map(implicit response => validateResponses("getAllExcludedEiLPersonForBik").json.as[List[EiLPerson]])

  def excludeEiLPersonFromBik(iabdString: String, empRef: EmpRef, year: Int, individual: EiLPerson)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[EiLResponse] =
    client
      .POST(
        s"$baseUrl/${empRef.encodedEmpRef}/$year/${Bik.asNPSTypeValue(iabdString)}/exclusion/update",
        individual,
        createOrCheckForRequiredHeaders
      )
      .map {
        case response if response.body.length <= maxEmptyBodyLength =>
          EiLResponse(validateResponses("excludeEiLPersonFromBik")(response).status, List.empty)
        case response                                               =>
          EiLResponse(response.status, validateResponses("excludeEiLPersonFromBik")(response).json.as[List[EiLPerson]])
      }

  def removeEiLPersonExclusionFromBik(iabdString: String, empRef: EmpRef, year: Int, individualToRemove: EiLPerson)(
    implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Int] =
    client
      .POST(
        s"$baseUrl/${empRef.encodedEmpRef}/$year/${Bik.asNPSTypeValue(iabdString)}/exclusion/remove",
        individualToRemove,
        createOrCheckForRequiredHeaders
      )
      .map { implicit response =>
        validateResponses("removeEiLPersonExclusionFromBik").status
      }

  def updateOrganisationsRegisteredBiks(empRef: EmpRef, year: Int, changes: List[Bik])(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Int] =
    client
      .POST(
        s"$baseUrl/${empRef.encodedEmpRef}/$year/updatebenefittypes",
        changes,
        createOrCheckForRequiredHeaders
      )
      .map { implicit response =>
        validateResponses("updateOrganisationsRegisteredBiks").status
      }

  private def responseHeaders(implicit response: HttpResponse): Map[String, String] =
    Map(
      HeaderTags.ETAG   -> response.header(HeaderTags.ETAG).getOrElse("0"),
      HeaderTags.X_TXID -> response.header(HeaderTags.X_TXID).getOrElse("1")
    )

  private def createOrCheckForRequiredHeaders(implicit request: Request[_]): Seq[(String, String)] = {
    val etagFromSession  = request.session.get(HeaderTags.ETAG).getOrElse("0")
    val xtxidFromSession = request.session.get(HeaderTags.X_TXID).getOrElse("1")
    logger.info(
      "[PbikConnector][createOrCheckForRequiredHeaders] POST etagFromSession: " + etagFromSession + ", xtxidFromSession: " + xtxidFromSession
    )
    Map(HeaderTags.ETAG -> etagFromSession, HeaderTags.X_TXID -> xtxidFromSession).toSeq
  }

  private def validateResponses(fromMethod: String)(implicit response: HttpResponse): HttpResponse =
    if (response.status >= BAD_REQUEST) {
      logger.error(s"[PbikConnector][$fromMethod] An unexpected status was returned: ${response.status}")
      throw new GenericServerErrorException(response.body)
    } else if (response.body.length <= maxEmptyBodyLength) {
      response
    } else {
      response.json.validate[PbikError] match {
        case errorReceived: JsSuccess[PbikError] =>
          logger.error(
            s"[PbikConnector][$fromMethod] a pbik error code was returned. Error Code: ${errorReceived.value.errorCode}"
          )
          throw new GenericServerErrorException(errorReceived.value.errorCode)
        case _: JsError                          => response
      }
    }
}

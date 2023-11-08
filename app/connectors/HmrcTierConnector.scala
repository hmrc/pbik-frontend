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

import models.{EmpRef, HeaderTags, PbikError}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.Exceptions.GenericServerErrorException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HmrcTierConnector @Inject() (client: HttpClient)(implicit ec: ExecutionContext) extends Logging {

  def createGetUrl(baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int): String = {
    val orgIdentifierEncoded = empRef.encodedEmpRef
    orgIdentifierEncoded.trim.length match {
      case 3 => s"$baseUrl/$year/$URIExtension"
      case _ => s"$baseUrl/$orgIdentifierEncoded/$year/$URIExtension"
    }
  }

  def get(baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val request = client.GET(createGetUrl(baseUrl, URIExtension, empRef, year))
    request
  }

  /*
    - New code should be simple and not do too many things
    - We can potentially still keep the logging of headers and validation
    - However, returning the response as is opens up the possibility to transform the data into whatever we want.
    - Old genericGet method was returning a tuple making it annoying to manipulate as you would have to deconstruct the tuple into ._1 or ._2 etc.
   */

  def getV2WithLoggingAndJsonValidation(baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int)(implicit
                                                                            hc: HeaderCarrier
  ): Future[HttpResponse] = {

    val request = client.GET(createGetUrl(baseUrl, URIExtension, empRef, year))
    request.map { response =>
      val headers: Map[String, String] = Map(
        HeaderTags.ETAG -> response.header(HeaderTags.ETAG).getOrElse("0"),
        HeaderTags.X_TXID -> response.header(HeaderTags.X_TXID).getOrElse("1")
      )

      logger.info("[HmrcTierConnector][get] GET etag/xtxid headers: " + headers)

      response.json.validate[PbikError] match {
        case s: JsSuccess[PbikError] =>
          logger.error(s"[HmrcTierConnector][get] a PBIK error code was returned. Error Code: ${s.value.errorCode}")
          throw new GenericServerErrorException(s.value.errorCode)
        case _: JsError => response
      }
    }
  }



// old code
//  def genericGetCall[T](baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int)(implicit
//    hc: HeaderCarrier,
//    formats: Format[T]
//  ): Future[T] = {
//    val resp = client.GET(createGetUrl(baseUrl, URIExtension, empRef, year))
//
//    resp.map { r =>
//      val headers: Map[String, String] = Map(
//        HeaderTags.ETAG   -> r.header(HeaderTags.ETAG).getOrElse("0"),
//        HeaderTags.X_TXID -> r.header(HeaderTags.X_TXID).getOrElse("1")
//      )
//
//      pbikHeaders = headers
//      logger.info("[HmrcTierConnector][genericGetCall] GET etag/xtxid headers: " + pbikHeaders)
//
//      r.json.validate[PbikError] match {
//        case s: JsSuccess[PbikError] =>
//          logger.error(
//            s"[HmrcTierConnector][genericGetCall] a pbik error code was returned. Error Code: ${s.value.errorCode}"
//          )
//          throw new GenericServerErrorException(s.value.errorCode)
//        case _: JsError              => r.json.as[T]
//      }
//
//    }
//  }

  def createPostUrl(baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int): String = {
    val orgIdentifierEncoded = empRef.encodedEmpRef
    s"$baseUrl/$orgIdentifierEncoded/$year/$URIExtension"
  }

  def genericPostCall[T](baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int, data: T)(implicit
    hc: HeaderCarrier,
    request: Request[_],
    formats: Format[T]
  ): Future[HttpResponse] = {

    val etagFromSession  = request.session.get(HeaderTags.ETAG).getOrElse("0")
    val xtxidFromSession = request.session.get(HeaderTags.X_TXID).getOrElse("1")
    val optMapped        = Map(HeaderTags.ETAG -> etagFromSession, HeaderTags.X_TXID -> xtxidFromSession)

    logger.info(
      "[HmrcTierConnector][genericPostCall] POST etagFromSession: " + etagFromSession + ", xtxidFromSession: " + xtxidFromSession
    )
    client.POST(createPostUrl(baseUrl, URIExtension, empRef, year), data, optMapped.toSeq).map {
      response: HttpResponse =>
        processResponse(response)
    }
  }

  def processResponse(response: HttpResponse): HttpResponse =
    response match {
      case _ if response.status >= 400    =>
        logger.error(s"[HmrcTierConnector][processResponse] An unexpected status was returned: ${response.status}")
        throw new GenericServerErrorException(response.body)
      case _ if response.body.length <= 0 => response
      case _                              =>
        response.json.validate[PbikError].asOpt match {
          case Some(pbikError) =>
            logger.error(
              s"[HmrcTierConnector][processResponse] A pbik error code was returned. Error Code: ${pbikError.errorCode}"
            )
            throw new GenericServerErrorException(pbikError.errorCode)
          case _               => response
        }
    }
}

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

import models.{Bik, EmpRef, HeaderTags, PbikError}
import play.api.Logger
import play.api.libs.json
import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.config.ServicesConfig
import utils.URIInformation
import utils.Exceptions.GenericServerErrorException
import java.net.URLEncoder

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

trait TierClient {

}

object HmrcTierConnector extends HmrcTierConnector with ServicesConfig {
  val serviceUrl = baseUrl("government-gateway")
  lazy val http = WSHttp
}

class HmrcTierConnector extends URIInformation with TierClient {
  var pbikHeaders = Map[String, String]()

  def encode(value: String): String = URLEncoder.encode(value, "UTF-8")

  def createGetUrl(baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int): String = {
    val orgIdentifierEncoded = empRef.encodedEmpRef
    orgIdentifierEncoded.trim.length match {
      case 3 => s"$baseUrl/$year/$URIExtension"
      case _ => s"$baseUrl/$orgIdentifierEncoded/$year/$URIExtension"
    }
  }

  def genericGetCall[T](baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int)
                       (implicit hc: HeaderCarrier, request: Request[_],
                        formats: json.Format[T], m: Manifest[T]): Future[T] = {
    val resp = WSHttp.GET(createGetUrl(baseUrl, URIExtension, empRef, year))

    resp.map { r =>
      val headers: Map[String, String] = Map(HeaderTags.ETAG -> r.header(HeaderTags.ETAG).getOrElse("0"), HeaderTags.X_TXID -> r.header(HeaderTags.X_TXID).getOrElse("1"))

      pbikHeaders = headers
      Logger.info("GET etag/xtxid headers: " + pbikHeaders)

      r.json.validate[PbikError] match {
        case s: JsSuccess[PbikError] => throw new GenericServerErrorException(s.value.errorCode)
        case e: JsError => r.json.as[T]
      }

    }
  }

  def createPostUrl(baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int): String = {
    val orgIdentifierEncoded = empRef.encodedEmpRef
    s"$baseUrl/$orgIdentifierEncoded/$year/$URIExtension"
  }


  def genericPostCall[T](baseUrl: String, URIExtension: String, empRef: EmpRef, year: Int, data: T)
                        (implicit hc: HeaderCarrier, request: Request[_],
                         formats: json.Format[T]): Future[HttpResponse] = {

    val etagFromSession = request.session.get(HeaderTags.ETAG).getOrElse("0")
    val xtxidFromSession = request.session.get(HeaderTags.X_TXID).getOrElse("1")
    val optMapped = Map(HeaderTags.ETAG -> etagFromSession, HeaderTags.X_TXID -> xtxidFromSession)

    Logger.info("POST etagFromSession: " + etagFromSession + ", xtxidFromSession: " + xtxidFromSession)

    WSHttp.POST(createPostUrl(baseUrl, URIExtension, empRef, year), data, optMapped.toSeq).map { response: HttpResponse =>
      processResponse(response)
    }
  }

  def processResponse(response: HttpResponse): HttpResponse = {
    response match {
      case _ if (response.status >= 400) => throw new GenericServerErrorException(response.body)
      case _ if (response.body.length <= 0) => response
      case _ => {
        response.json.validate[PbikError].asOpt match {
          case Some(pe) => {
            val error = pe.errorCode
            throw new GenericServerErrorException(error)
          }
          case _ => response
        }
      }
    }
  }
}

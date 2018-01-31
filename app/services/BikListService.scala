/*
 * Copyright 2018 HM Revenue & Customs
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

package services

import config.PbikAppConfig
import connectors.{HmrcTierConnector, TierConnector}
import models.Bik
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllersReferenceData, URIInformation}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object BikListService extends BikListService {
  def pbikAppConfig = PbikAppConfig
  val tierConnector = new HmrcTierConnector
}

trait BikListService extends TierConnector with URIInformation with ControllersReferenceData {
  def pbikHeaders = Map[String, String]()

  def currentYearList(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]): Future[(Map[String, String], List[Bik])] = {
    val response = tierConnector.genericGetCall[List[Bik]](baseUrl, getRegisteredPath,
        ac.principal.accounts.epaye.get.empRef.toString,
        YEAR_RANGE.cyminus1)

    response.map { resultOption: List[Bik] =>
      (tierConnector.pbikHeaders, resultOption.distinct)
    }
  }

  def nextYearList(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]): Future[(Map[String, String], List[Bik])] = {
    val response = tierConnector.genericGetCall[List[Bik]](baseUrl, getRegisteredPath,
        ac.principal.accounts.epaye.get.empRef.toString,
        YEAR_RANGE.cy)

    response.map { resultOption: List[Bik] =>
      (tierConnector.pbikHeaders, resultOption.distinct)
    }
  }

  def registeredBenefitsList(year: Int, orgIdentifier: String)(path: String)(implicit ac: AuthContext, hc: HeaderCarrier, request: Request[_]): Future[List[Bik]] = {
    val newPath = path match {
      case "" => getRegisteredPath
      case _ => path
    }
    val response = tierConnector.genericGetCall[List[Bik]](baseUrl,
      newPath, orgIdentifier, year)
    response
  }
}

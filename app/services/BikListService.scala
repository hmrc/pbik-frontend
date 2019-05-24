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

package services

import config.{AppConfig, PbikAppConfig}
import connectors.HmrcTierConnector
import javax.inject.Inject
import models.{AuthenticatedRequest, Bik, EmpRef}
import play.api.Mode.Mode
import play.api.mvc.Request
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ControllersReferenceData, URIInformation}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BikListService @Inject()(val pbikAppConfig: AppConfig,
                               val tierConnector: HmrcTierConnector,
                               controllersReferenceData: ControllersReferenceData,
                               uriInformation: URIInformation) {

  def pbikHeaders: Map[String, String] = Map[String, String]()

  def currentYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[(Map[String, String], List[Bik])] = {
    val response = tierConnector.genericGetCall[List[Bik]](uriInformation.baseUrl, uriInformation.getRegisteredPath,
      request.empRef, controllersReferenceData.YEAR_RANGE.cyminus1)

    response.map { resultOption: List[Bik] =>
      (tierConnector.pbikHeaders, resultOption.distinct)
    }
  }

  def nextYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[(Map[String, String], List[Bik])] = {
    val response = tierConnector.genericGetCall[List[Bik]](uriInformation.baseUrl, uriInformation.getRegisteredPath,
      request.empRef, controllersReferenceData.YEAR_RANGE.cy)

    response.map { resultOption: List[Bik] =>
      (tierConnector.pbikHeaders, resultOption.distinct)
    }
  }

  def registeredBenefitsList(year: Int, empRef: EmpRef)(path: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[List[Bik]] = {
    val newPath = if (path == "") uriInformation.getRegisteredPath else path
    val response = tierConnector.genericGetCall[List[Bik]](uriInformation.baseUrl, newPath, empRef, year)
    response
  }
}

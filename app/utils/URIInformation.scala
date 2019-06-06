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

package utils

import config.Service
import javax.inject.Inject
import play.api.{Configuration, Logger}
import utils.Exceptions.InvalidBikTypeURIException

class URIInformation @Inject()(configuration: Configuration) extends URIValues {

  val baseUrl: String = configuration.get[Service]("microservice.services.pbik") + "/epaye"

  val urlMappedIABDValues = List(
    ("40", "assets-transferred"),
    ("48", "payments-employee"),
    ("54", "vouchers-credit-cards"),
    ("38", "living-accommodation"),
    ("44", "mileage"),
    ("31", "car"),
    ("29", "car-fuel"),
    ("35", "vans"),
    ("36", "van-fuel"),
    ("37", "interest-free-loans"),
    ("30", "medical"),
    ("50", "qualifying-relocation"),
    ("8", "services"),
    ("39", "assets-disposal"),
    ("47", "other"),
    ("52", "income-tax"),
    ("53", "travelling-subsistence"),
    ("42", "entertainment"),
    ("43", "business-travel"),
    ("32", "telephone"),
    ("45", "non-qualifying-relocation")
  )

  def iabdValueURLMapper(iabd: String): String = {
    val value = urlMappedIABDValues.find(x=> x._1 == iabd)
    value match {
      case Some(_) => value.get._2
      case None => {
        Logger.info("invalid bik passed to map url: " + iabd)
        throw new InvalidBikTypeURIException
      }
    }
  }

  def iabdValueURLDeMapper(iabdMappedURL: String): String = {
    val value = urlMappedIABDValues.find(x=> x._2 == iabdMappedURL)
    value match {
      case Some(_) => value.get._1
      case None =>
        Logger.info("invalid bik passed to de-map url: " + iabdMappedURL)
        throw new InvalidBikTypeURIException
    }
  }
}

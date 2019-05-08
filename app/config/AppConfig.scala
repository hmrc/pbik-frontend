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

package config

import java.util.Collections

import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val assetsPrefix: String
  val reportAProblemPartialUrl: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val analyticsToken: String
  val analyticsHost: String
  val contactFrontendService: String
  val contactFormServiceIdentifier: String
  val cyEnabled:Boolean
  val maximumExclusions:Int
  val biksNotSupported: List[Int]
  val biksNotSupportedCY: List[Int]
  val biksDecommissioned: List[Int]
  val urBannerLink: String
  val serviceSignOut : String

}

object PbikAppConfig extends AppConfig with ServicesConfig with RunModeConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  override lazy val contactFrontendService: String = baseUrl("contact-frontend")
  override lazy val contactFormServiceIdentifier = "PayrollBIK"
  override lazy val maximumExclusions:Int = configuration.getInt("pbik.exclusions.maximum").getOrElse(300)

  private lazy val contactHost = configuration.getString("contact-frontend.host").getOrElse("")

  override lazy val assetsPrefix: String = loadConfig("assets.url") + loadConfig("assets.version")
  override lazy val reportAProblemPartialUrl = s"${baseUrl("contact-frontend")}/contact/problem_reports"

  override lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  override lazy val analyticsToken: String = loadConfig("google-analytics.token")
  override lazy val analyticsHost: String = loadConfig("google-analytics.host")

  override lazy val cyEnabled: Boolean = configuration.getBoolean("pbik.enabled.cy").getOrElse(false)
  override lazy val biksNotSupported:List[Int] = configuration.getIntList("pbik.unsupported.biks.cy1").getOrElse(Collections.emptyList[Integer]()).toArray(new Array[Integer](0)).toList.map(_.intValue())
  override lazy val biksNotSupportedCY:List[Int] = configuration.getIntList("pbik.unsupported.biks.cy").getOrElse(Collections.emptyList[Integer]()).toArray(new Array[Integer](0)).toList.map(_.intValue())
  override lazy val biksDecommissioned:List[Int] = configuration.getIntList("pbik.decommissioned.biks").getOrElse(Collections.emptyList[Integer]()).toArray(new Array[Integer](0)).toList.map(_.intValue())

  lazy val urBannerToggle:Boolean = loadConfig("urBanner.toggle").toBoolean
  override lazy val urBannerLink: String = loadConfig("urBanner.link")
  override lazy val serviceSignOut: String = loadConfig("service-signout.url")

}

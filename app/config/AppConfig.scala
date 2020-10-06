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

package config

import javax.inject.Inject
import play.api.Configuration

trait AppConfig {
  val assetsPrefix: String
  val reportAProblemPartialUrl: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val contactFrontendService: String
  val contactFormServiceIdentifier: String
  val cyEnabled: Boolean
  val maximumExclusions: Int
  val biksNotSupported: Seq[Int]
  val biksNotSupportedCY: Seq[Int]
  val biksDecommissioned: Seq[Int]
  val urBannerLink: String
  val feedbackUrl: String
  val signOut: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val ssoUrl: Option[String]
}

class PbikAppConfig @Inject()(configuration: Configuration) extends AppConfig {

  private lazy val companyAuthHost: String = configuration.get[String]("microservice.auth.company-auth.host")
  override lazy val contactFrontendService: String =
    configuration.get[Service]("microservice.services.contact-frontend")
  override lazy val contactFormServiceIdentifier = "PayrollBIK"
  override lazy val maximumExclusions: Int = configuration.get[Int]("pbik.exclusions.maximum")

  private lazy val contactHost: Service = configuration.get[Service]("microservice.services.contact-frontend")

  override lazy val assetsPrefix: String = configuration.get[String]("assets.url") + configuration.get[String](
    "assets.version")
  override lazy val reportAProblemPartialUrl =
    s"${configuration.get[Service]("microservice.services.contact-frontend")}/contact/problem_reports"

  override lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUnauthenticatedUrl =
    s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

  override lazy val cyEnabled: Boolean = configuration.get[Boolean]("pbik.enabled.cy")
  override lazy val biksNotSupported: Seq[Int] = configuration.get[Seq[Int]]("pbik.unsupported.biks.cy1")
  override lazy val biksNotSupportedCY: Seq[Int] = configuration.get[Seq[Int]]("pbik.unsupported.biks.cy")
  override lazy val biksDecommissioned: Seq[Int] = configuration.get[Seq[Int]]("pbik.decommissioned.biks")
  lazy val urBannerToggle: Boolean = configuration.get[Boolean]("urBanner.toggle")
  override lazy val urBannerLink: String = configuration.get[String]("urBanner.link")
  override lazy val feedbackUrl: String = configuration.get[String]("feedback.url")
  override lazy val signOut = s"$companyAuthHost/gg/sign-out/?continue=$feedbackUrl"

  override val analyticsToken: Option[String] = configuration.getOptional[String]("google-analytics.token")
  override val analyticsHost: String =
    configuration.getOptional[String]("google-analytics.host").getOrElse("service.gov.uk")
  override val ssoUrl: Option[String] = configuration.getOptional[String]("portal.ssoUrl")

  lazy val sessionCacheBaseUri: String = configuration.get[String]("microservice.services.keystore.host")
  lazy val sessionCacheDomain: String =
    configuration.get[String](s"microservice.services.cachable.session-cache.domain")
}

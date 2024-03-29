/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class PbikAppConfig @Inject() (configuration: Configuration) {

  private lazy val basGatewayHost: String = configuration.get[String]("microservice.auth.bas-gateway.host")
  lazy val maximumExclusions: Int         = configuration.get[Int]("pbik.exclusions.maximum")

  lazy val cyEnabled: Boolean                  = configuration.get[Boolean]("pbik.enabled.cy")
  lazy val biksNotSupported: Seq[Int]          = configuration.get[Seq[Int]]("pbik.unsupported.biks.cy1")
  lazy val biksNotSupportedCY: Seq[Int]        = configuration.get[Seq[Int]]("pbik.unsupported.biks.cy")
  lazy val biksDecommissioned: Seq[Int]        = configuration.get[Seq[Int]]("pbik.decommissioned.biks")
  lazy val feedbackUrl: String                 = configuration.get[String]("feedback.url")
  lazy val signOut: String                     = s"$basGatewayHost/bas-gateway/sign-out-without-state/?continue=$feedbackUrl"
  private lazy val timedOutRedirectUrl: String = configuration.get[String]("timedOutUrl")
  lazy val timedOutUrl: String                 =
    s"$basGatewayHost/bas-gateway/sign-out-without-state/?continue=$timedOutRedirectUrl"

  val ssoUrl: Option[String] = configuration.getOptional[String]("portal.ssoUrl")

  lazy val timeout: Int          = configuration.get[Int]("timeout.timeout")
  lazy val timeoutCountdown: Int = configuration.get[Int]("timeout.countdown")

  lazy val loginCallbackUrl: String    = configuration.get[String]("microservice.auth.login-callback.url")
  private lazy val loginPath: String   = configuration.get[String]("microservice.auth.login_path")
  private lazy val signOutPath: String = configuration.get[String]("microservice.auth.signout_path")
  lazy val authSignIn: String          = s"$basGatewayHost/bas-gateway/$loginPath"
  lazy val authSignOut: String         = s"$basGatewayHost/bas-gateway/$signOutPath"

  lazy val mongoTTL: Int = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  private lazy val agentFrontendHost: String = configuration.getOptional[String]("agent-frontend.host").getOrElse("")
  private lazy val agentFrontendPath: String = configuration.get[String]("agent-frontend.clientListPath")

  lazy val agentClientListUrl: String = s"$agentFrontendHost$agentFrontendPath"

  private lazy val btaFrontendHost: String =
    configuration.getOptional[String]("business-tax-account.host").getOrElse("")
  private lazy val btaFrontendPath: String = configuration.get[String]("business-tax-account.url")

  lazy val btaAccountUrl: String = s"$btaFrontendHost$btaFrontendPath"

}

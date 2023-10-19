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

package config

import play.api.Configuration

import javax.inject.{Inject, Singleton}

trait AppConfig {
  val cyEnabled: Boolean
  val maximumExclusions: Int
  val biksNotSupported: Seq[Int]
  val biksNotSupportedCY: Seq[Int]
  val biksDecommissioned: Seq[Int]
  val urBannerLink: String
  val feedbackUrl: String
  val signOut: String
  val ssoUrl: Option[String]
  val timeout: Int
  val timeoutCountdown: Int
  val loginCallbackUrl: String
  val authSignIn: String
  val authSignOut: String
  val timedOutUrl: String
  val mongoTTL: Int
}

@Singleton
class PbikAppConfig @Inject() (configuration: Configuration) extends AppConfig {

  private lazy val basGatewayHost: String  = configuration.get[String]("microservice.auth.bas-gateway.host")
  override lazy val maximumExclusions: Int = configuration.get[Int]("pbik.exclusions.maximum")

  override lazy val cyEnabled: Boolean           = configuration.get[Boolean]("pbik.enabled.cy")
  override lazy val biksNotSupported: Seq[Int]   = configuration.get[Seq[Int]]("pbik.unsupported.biks.cy1")
  override lazy val biksNotSupportedCY: Seq[Int] = configuration.get[Seq[Int]]("pbik.unsupported.biks.cy")
  override lazy val biksDecommissioned: Seq[Int] = configuration.get[Seq[Int]]("pbik.decommissioned.biks")
  override lazy val urBannerLink: String         = configuration.get[String]("urBanner.link")
  override lazy val feedbackUrl: String          = configuration.get[String]("feedback.url")
  override lazy val signOut: String              = s"$basGatewayHost/bas-gateway/sign-out-without-state/?continue=$feedbackUrl"
  private lazy val timedOutRedirectUrl: String   = configuration.get[String]("timedOutUrl")
  override lazy val timedOutUrl: String          =
    s"$basGatewayHost/bas-gateway/sign-out-without-state/?continue=$timedOutRedirectUrl"

  override val ssoUrl: Option[String] = configuration.getOptional[String]("portal.ssoUrl")

  override lazy val timeout: Int          = configuration.get[Int]("timeout.timeout")
  override lazy val timeoutCountdown: Int = configuration.get[Int]("timeout.countdown")

  override lazy val loginCallbackUrl: String = configuration.get[String]("microservice.auth.login-callback.url")
  private lazy val loginPath: String         = configuration.get[String]("microservice.auth.login_path")
  private lazy val signOutPath: String       = configuration.get[String]("microservice.auth.signout_path")
  override lazy val authSignIn: String       = s"$basGatewayHost/bas-gateway/$loginPath"
  override lazy val authSignOut: String      = s"$basGatewayHost/bas-gateway/$signOutPath"

  override lazy val mongoTTL: Int = configuration.get[Int]("mongodb.timeToLiveInSeconds")
}

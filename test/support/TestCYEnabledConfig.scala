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

package support

import config.AppConfig

object TestCYEnabledConfig extends AppConfig {

    override lazy val contactFrontendService: String = ""
    override lazy val contactFormServiceIdentifier: String = ""
    override lazy val assetsPrefix: String = ""
    override lazy val reportAProblemPartialUrl: String = "http://localhost:9250/contact/problem_reports"
    override lazy val betaFeedbackUrl: String = ""
    override lazy val betaFeedbackUnauthenticatedUrl: String = ""
    override lazy val analyticsToken: Option[String] = Some("")
    override lazy val analyticsHost: String = ""
    override lazy val cyEnabled: Boolean = true
    override lazy val biksNotSupported: List[Int] = List.empty[Int]
    override lazy val biksNotSupportedCY: List[Int] = List.empty[Int]
    override lazy val biksDecommissioned: List[Int] = List.empty[Int]
    override lazy val maximumExclusions: Int = 300
    override lazy val urBannerLink: String = ""
    override val serviceSignOut: String = ""
    override val ssoUrl: Option[String] = None
  }

  object TestCYDisabledConfig extends AppConfig {

    override lazy val contactFrontendService: String = ""
    override lazy val contactFormServiceIdentifier: String = ""
    override lazy val assetsPrefix: String = ""
    override lazy val reportAProblemPartialUrl: String = "http://localhost:9250/contact/problem_reports"
    override lazy val betaFeedbackUrl: String = ""
    override lazy val betaFeedbackUnauthenticatedUrl: String = ""
    override lazy val analyticsToken: Option[String] = Some("")
    override lazy val analyticsHost: String = ""
    override lazy val cyEnabled: Boolean = false
    override lazy val biksNotSupported: List[Int] = List.empty[Int]
    override lazy val biksNotSupportedCY: List[Int] = List.empty[Int]
    override lazy val biksDecommissioned: List[Int] = List.empty[Int]
    override lazy val maximumExclusions: Int = 300
    override lazy val urBannerLink: String = ""
    override val serviceSignOut: String = ""
    override val ssoUrl: Option[String] = None
  }
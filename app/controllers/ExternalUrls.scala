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

package controllers

import javax.inject.Inject
import play.api.Configuration

class ExternalUrls @Inject()(configuration: Configuration) {

  val loginCallback: String = configuration.getString("microservice.auth.login-callback.url").getOrElse(routes.HomePageController.onPageLoad().url)
  val companyAuthHost: String = configuration.getString("microservice.auth.company-auth.host").getOrElse("")
  val loginLocalPath: String = configuration.getString("microservice.auth.login_local_path").getOrElse("")
  val loginPath: String = configuration.getString("microservice.auth.login_path").getOrElse("")
  val signOutPath: String = configuration.getString("microservice.auth.signout_path").getOrElse("")

  val continue: String = loginCallback
  val signIn          = s"$companyAuthHost/gg/$loginPath"
  val signInLocal     = s"$companyAuthHost/gg/$loginLocalPath"
  val signOut         = s"$companyAuthHost/gg/$signOutPath"

}

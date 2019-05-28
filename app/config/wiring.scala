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

import connectors.SessionCookieCryptoFilterWrapper
import javax.inject.Inject
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.bootstrap.config.LoadAuditingConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.partials.FormPartialRetriever

class PbikFrontendAuditConnector @Inject()(configuration: Configuration,
                                           environment: Environment) extends Auditing {
  val mode: Mode = environment.mode
  val runModeConfiguration: Configuration = configuration
  override lazy val auditingConfig = LoadAuditingConfig(mode = mode,
                                                        configuration = runModeConfiguration,
                                                        key = s"auditing")
}

class LocalFormPartialRetriever @Inject()(val httpGet: HttpClient, cookieCrypto: SessionCookieCryptoFilterWrapper) extends FormPartialRetriever {
  override val crypto: String => String = cookieCrypto.encryptCookieString
}

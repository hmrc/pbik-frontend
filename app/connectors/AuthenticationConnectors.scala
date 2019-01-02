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

package connectors
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever, HeaderCarrierForPartialsConverter}
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.http._

object FrontendAuditConnector extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}

object WSHttp extends HttpGet with WSGet with HttpPut with WSPut with WSPost with HttpPost with WSDelete with HttpDelete with WSPatch with HttpPatch with AppName with RunMode with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override val auditConnector = FrontendAuditConnector
}


object CachedStaticHtmlPartial extends CachedStaticHtmlPartialRetriever {
  override val httpGet = WSHttp
}

object FormPartialProvider extends FormPartialRetriever with SessionCookieCryptoFilterWrapper {
  override val httpGet = WSHttp
  override val crypto = encryptCookieString _
}

object PBIKHeaderCarrierForPartialsConverter extends HeaderCarrierForPartialsConverter with SessionCookieCryptoFilterWrapper {
  override val crypto = encryptCookieString _
}

trait SessionCookieCryptoFilterWrapper {

  def encryptCookieString(cookie: String) : String = {
    SessionCookieCryptoFilter.encrypt(cookie)
  }
}

object FrontendAuthConnector extends AuthConnector with ServicesConfig {
  val serviceUrl = baseUrl("auth")
  lazy val http = WSHttp
}

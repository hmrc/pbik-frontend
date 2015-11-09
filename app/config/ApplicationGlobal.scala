/*
 * Copyright 2015 HM Revenue & Customs
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

import connectors.FrontendAuditConnector
import play.api.mvc.{Filters, EssentialAction, Request}
import play.filters.headers.{SecurityHeadersParser, SecurityHeadersConfig, DefaultSecurityHeadersConfig, SecurityHeadersFilter}
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.config._
import uk.gov.hmrc.play.frontend.bootstrap._
import play.api._
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.filters._
import play.api.Mode.Mode
import java.io.File
import uk.gov.hmrc.play.frontend.auth.controllers.AuthParamsConfigurationValidator
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

import play.api.mvc.Request
import play.twirl.api.Html
import play.api.{Application, Configuration}
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter

object ApplicationGlobal extends FrontendGlobal {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
    views.html.error_template(pageTitle, heading, message)

}

abstract class FrontendGlobal
  extends DefaultFrontendGlobal
  with RunMode {

  override val auditConnector = FrontendAuditConnector
  override val loggingFilter: FrontendLoggingFilter = LoggingFilter
  override val frontendAuditFilter = PbikAuditFilter

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def doFilter(a: EssentialAction): EssentialAction = {
    val securityFilter = {
      val configuration = play.api.Play.current.configuration
      val securityHeadersConfig:DefaultSecurityHeadersConfig = new SecurityHeadersParser().parse(configuration).asInstanceOf[DefaultSecurityHeadersConfig]
      val sameOriginConfig:SecurityHeadersConfig = securityHeadersConfig.copy(frameOptions = Some("SAMEORIGIN"),None,None,None,None)
      SecurityHeadersFilter(sameOriginConfig)
    }
    Filters(super.doFilter(a), Seq(securityFilter):_*)
  }

  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode): Configuration = {
    AuthParamsConfigurationValidator.validate(config)
    super.onLoadConfig(config, path, classloader, mode)
  }

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig("microservice.metrics")
}


object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object LoggingFilter extends FrontendLoggingFilter {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object PbikAuditFilter extends FrontendAuditFilter with RunMode with AppName {

  import play.api.Play.current

  override lazy val maskedFormFields = Play.configuration.getString("frontend-audit.masked-form-fields").getOrElse("password").split(',').toSeq

  override lazy val applicationPort = None

  override lazy val auditConnector = FrontendAuditConnector

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}
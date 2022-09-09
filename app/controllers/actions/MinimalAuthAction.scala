/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.actions

import com.google.inject.ImplementedBy
import config.AppConfig

import javax.inject.{Inject, Singleton}
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MinimalAuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default,
  config: AppConfig
)(implicit val executionContext: ExecutionContext)
    extends MinimalAuthAction
    with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised(ConfidenceLevel.L50) {
      block(request)
    } recover { case ex: NoActiveSession =>
      Redirect(config.authSignIn, Map("continue" -> Seq(config.loginCallbackUrl), "origin" -> Seq("pbik-frontend")))
    }
  }
}

@ImplementedBy(classOf[MinimalAuthActionImpl])
trait MinimalAuthAction extends ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request]

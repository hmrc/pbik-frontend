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

package controllers.actions

import com.google.inject.ImplementedBy
import config.Service
import controllers.ExternalUrls
import javax.inject.Inject
import models.{AuthenticatedRequest, EmpRef, UserName}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(override val authConnector: AuthConnector,
                               val parser: BodyParsers.Default,
                               externalUrls: ExternalUrls)
  (implicit val executionContext: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L50 and Enrolment("IR-PAYE")).retrieve(Retrievals.authorisedEnrolments and Retrievals.name) {
      case Enrolments(enrolments) ~ name => {
        enrolments.find(_.key == "IR-PAYE").map {
          enrolment =>
            val taxOfficeNumber = enrolment.identifiers.find(id => id.key == "TaxOfficeNumber").map(_.value)
            val taxOfficeReference = enrolment.identifiers.find(id => id.key == "TaxOfficeReference").map(_.value)

            (taxOfficeNumber, taxOfficeReference) match {
              case (Some(number), Some(reference)) => block(AuthenticatedRequest(
                EmpRef(number, reference),
                UserName(name.getOrElse(Name(None, None))),
                request))
              case _ => Future.successful(Results.Redirect(controllers.routes.HomePageController.onPageLoad()))
            }
        }.getOrElse(Future.successful(Results.Redirect(controllers.routes.HomePageController.onPageLoad())))
      }
    } recover {
      case ex: NoActiveSession =>
        Redirect(externalUrls.signIn, Map("continue" -> Seq(externalUrls.loginCallback),
                                             "origin" -> Seq("pbik-frontend")))
      case ex: InsufficientEnrolments =>
        Results.Redirect(controllers.routes.AuthController.notAuthorised())

    }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

class AuthConnector @Inject()(val http: HttpClient,
                              configuration: Configuration) extends PlayAuthConnector {

  override val serviceUrl: String = configuration.get[Service]("microservice.services.auth")

}

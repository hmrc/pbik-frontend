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

package controllers.actions

import com.google.inject.ImplementedBy
import config.{PbikAppConfig, Service}
import connectors.AgentPayeConnector
import models.auth.EpayeSessionKeys
import models.{AuthenticatedRequest, EmpRef, UserName}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default,
  config: PbikAppConfig,
  agentPayeConnector: AgentPayeConnector
)(implicit val executionContext: ExecutionContext)
    extends AuthAction
    with AuthorisedFunctions
    with Logging {

  private def authAsEmployer[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result])(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    authorised(ConfidenceLevel.L50 and Enrolment("IR-PAYE"))
      .retrieve(
        Retrievals.affinityGroup and Retrievals.authorisedEnrolments and Retrievals.name and Retrievals.agentCode
      ) { case _ ~ Enrolments(enrolments) ~ name ~ _ =>
        enrolments
          .find(_.key == "IR-PAYE")
          .map { enrolment =>
            val taxOfficeNumber    = enrolment.identifiers.find(id => id.key == "TaxOfficeNumber").map(_.value)
            val taxOfficeReference = enrolment.identifiers.find(id => id.key == "TaxOfficeReference").map(_.value)

            (taxOfficeNumber, taxOfficeReference) match {
              case (Some(number), Some(reference)) =>
                block(
                  AuthenticatedRequest(
                    EmpRef(number, reference),
                    UserName(name.getOrElse(Name(None, None))),
                    request,
                    None
                  )
                )
              case _                               =>
                logger.warn(
                  "[AuthAction][authAsEmployer] Authentication failed: invalid taxOfficeNumber and/or taxOfficeReference"
                )
                Future.successful(Results.Redirect(controllers.routes.AuthController.notAuthorised))
            }
          }
          .getOrElse {
            logger.warn("[AuthAction][authAsEmployer] Authentication failed - IR-PAYE key not found")
            Future.successful(Results.Redirect(controllers.routes.AuthController.notAuthorised))
          }
      } recover { case ex: InsufficientEnrolments =>
      logger.warn("[AuthAction][authAsEmployer] Insufficient enrolments provided with request")
      Results.Redirect(controllers.routes.AuthController.notAuthorised)
    }

  private def authAsAgent[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result])(implicit
    hc: HeaderCarrier
  ): Future[Result] = {
    val clientEmpRef = request.session.get(EpayeSessionKeys.AGENT_FRONTEND_EMPREF)

    clientEmpRef match {
      case None                         =>
        logger.warn("[AuthFunction][authAsAgent] No client EmpRef found in session")
        Future.successful(Redirect(config.agentClientListUrl))
      case Some(agentEmployerReference) =>
        val empRefs = agentEmployerReference.split("/")

        if (empRefs.size != 2) {
          logger.warn("[AuthFunction][authAsAgent] Invalid client EmpRef found in session")
          Future.successful(Redirect(config.agentClientListUrl))
        } else {

          val empRef = EmpRef(empRefs.head, empRefs.last)

          authorised(
            Enrolment("IR-PAYE")
              .withIdentifier("TaxOfficeNumber", empRef.taxOfficeNumber)
              .withIdentifier("TaxOfficeReference", empRef.taxOfficeReference)
              .withDelegatedAuthRule("lp-paye")
          )
            .retrieve(
              Retrievals.affinityGroup and Retrievals.authorisedEnrolments and Retrievals.name and Retrievals.agentCode
            ) { case _ ~ _ ~ name ~ agentCodeRetrieved =>
              val req: Future[Future[Result]] = for {
                client <- agentPayeConnector.getClient(agentCodeRetrieved, empRef)
              } yield block(
                AuthenticatedRequest(
                  empRef,
                  UserName(name.getOrElse(Name(None, None))),
                  request,
                  client
                )
              )

              req.flatMap(identity)
            }

        }
    }
  }

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()
      .retrieve(
        Retrievals.affinityGroup and Retrievals.authorisedEnrolments and Retrievals.name and Retrievals.agentCode
      ) { case Some(affinityGroup) ~ _ ~ _ ~ _ =>
        affinityGroup match {
          case AffinityGroup.Agent        => authAsAgent(request, block)
          case AffinityGroup.Organisation => authAsEmployer(request, block) //default how it was before
          //Individual
          case _                          =>
            logger.warn(
              s"[AuthAction][invokeBlock] Authentication failed - AffinityGroup not supported: ${affinityGroup.toString}"
            )
            throw new IllegalArgumentException(s"AffinityGroup not supported: ${affinityGroup.toString}")
        }
      }
      .recover {
        case ex: NoActiveSession =>
          logger.warn("[AuthAction][invokeBlock] No Active Session")
          Redirect(
            config.authSignIn,
            Map("continue_url" -> Seq(config.loginCallbackUrl), "origin" -> Seq("pbik-frontend"))
          )

        case ex: InsufficientEnrolments =>
          logger.warn("[AuthAction][authAsEmployer] Insufficient enrolments provided with request")
          Results.Redirect(controllers.routes.AuthController.notAuthorised)
      }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]

class AuthConnector @Inject() (val http: HttpClient, configuration: Configuration) extends PlayAuthConnector {

  override val serviceUrl: String = configuration.get[Service]("microservice.services.auth")

}

/*
 * Copyright 2025 HM Revenue & Customs
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
import config.PbikAppConfig
import connectors.AgentPayeConnector
import models.auth.{AuthenticatedRequest, EpayeSessionKeys}
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
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

  private val enrolmentKey          = "IR-PAYE"
  private val taxOfficeNumberKey    = "TaxOfficeNumber"
  private val taxOfficeReferenceKey = "TaxOfficeReference"

  private def authAsEmployer[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result])(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    authorised(Enrolment(enrolmentKey))
      .retrieve(
        Retrievals.affinityGroup and Retrievals.authorisedEnrolments and Retrievals.internalId and Retrievals.agentCode
      ) { case _ ~ Enrolments(enrolments) ~ internalId ~ _ =>
        enrolments
          .find(_.key == enrolmentKey)
          .map { enrolment =>
            val taxOfficeNumber: Option[String]    =
              enrolment.identifiers.find(id => id.key == taxOfficeNumberKey).map(_.value)
            val taxOfficeReference: Option[String] =
              enrolment.identifiers.find(id => id.key == taxOfficeReferenceKey).map(_.value)

            (taxOfficeNumber, taxOfficeReference) match {
              case (Some(number), Some(reference)) =>
                block(
                  AuthenticatedRequest(
                    EmpRef(number, reference),
                    internalId,
                    request,
                    None
                  )
                )
              case _                               =>
                logger.warn(
                  s"[AuthAction][authAsEmployer] Authentication failed: invalid $taxOfficeNumber and/or $taxOfficeReferenceKey"
                )
                Future.successful(Results.Redirect(controllers.routes.AuthController.notAuthorised))
            }
          }
          .getOrElse {
            logger.warn(s"[AuthAction][authAsEmployer] Authentication failed - $enrolmentKey key not found")
            Future.successful(Results.Redirect(controllers.routes.AuthController.notAuthorised))
          }
      }

  private def authAsAgent[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result])(implicit
    hc: HeaderCarrier
  ): Future[Result] = {
    val clientEmpRef = request.session.get(EpayeSessionKeys.AGENT_FRONTEND_EMPREF)

    clientEmpRef match {
      case None                         =>
        logger.warn("[AuthAction][authAsAgent] No client EmpRef found in session")
        Future.successful(Redirect(config.agentClientListUrl))
      case Some(agentEmployerReference) =>
        val empRefs = agentEmployerReference.split("/")

        if (empRefs.size != 2) {
          logger.warn("[AuthAction][authAsAgent] Invalid client EmpRef found in session")
          Future.successful(Redirect(config.agentClientListUrl))
        } else {

          val empRef = EmpRef(empRefs.head, empRefs.last)

          authorised(
            Enrolment(enrolmentKey)
              .withIdentifier(taxOfficeNumberKey, empRef.taxOfficeNumber)
              .withIdentifier(taxOfficeReferenceKey, empRef.taxOfficeReference)
              .withDelegatedAuthRule("lp-paye")
          )
            .retrieve(
              Retrievals.affinityGroup and Retrievals.authorisedEnrolments and Retrievals.internalId and Retrievals.agentCode
            ) { case _ ~ _ ~ internalId ~ agentCodeRetrieved =>
              val req: Future[Future[Result]] = for {
                client <- agentPayeConnector.getClient(agentCodeRetrieved, empRef)
              } yield block(
                AuthenticatedRequest(
                  empRef,
                  internalId,
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
        Retrievals.affinityGroup and Retrievals.authorisedEnrolments and Retrievals.internalId and Retrievals.agentCode
      ) {
        case Some(affinityGroup) ~ _ ~ _ ~ _ =>
          affinityGroup match {
            case AffinityGroup.Agent        => authAsAgent(request, block)
            case AffinityGroup.Organisation => authAsEmployer(request, block) // default how it was before
            // Individual
            case AffinityGroup.Individual   =>
              logger.warn(
                s"[AuthAction][invokeBlock] Authentication failed - AffinityGroup not supported: ${affinityGroup.toString}"
              )
              Future.successful(Results.Redirect(controllers.routes.AuthController.affinityIndividual))
          }
        case _                               =>
          logger.warn("[AuthAction][invokeBlock] Authentication failed - AffinityGroup not found")
          throw new IllegalArgumentException("AffinityGroup not found")
      }
      .recover {
        case ex: NoActiveSession =>
          logger.warn("[AuthAction][invokeBlock] No Active Session")
          Redirect(
            config.authSignIn,
            Map("continue_url" -> Seq(config.loginCallbackUrl), "origin" -> Seq("pbik-frontend"))
          )

        case ex: InsufficientEnrolments =>
          logger.warn("[AuthAction][invokeBlock] Insufficient enrolments provided with request")
          Results.Redirect(controllers.routes.AuthController.notAuthorised)

      }
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]

class AuthConnector @Inject() (val httpClientV2: HttpClientV2, pbikAppConfig: PbikAppConfig) extends PlayAuthConnector {
  override val serviceUrl: String = pbikAppConfig.authUrl
}

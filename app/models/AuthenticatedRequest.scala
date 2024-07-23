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

package models

import models.agent.Client
import play.api.mvc.{Request, WrappedRequest}

case class AuthenticatedRequest[A](empRef: EmpRef, name: UserName, request: Request[A], client: Option[Client])
    extends WrappedRequest[A](request) {

  val clientName: Option[String] = client.flatMap(_.name)

  /** its an agent logged in if we have a client info in the request
    */
  val isAgent: Boolean        = client.isDefined
  val isOrganisation: Boolean = !isAgent

  val userType: String = if (isAgent) "agent" else "organisation"

  //TODO review this logic maybe to do it somehow clever way in govuk wrapper template
  val showYTABackLink: Boolean = isOrganisation
  val showECLBackLink: Boolean = isAgent

}

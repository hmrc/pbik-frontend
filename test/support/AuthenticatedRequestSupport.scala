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

package support

import models.agent.{AccountsOfficeReference, Client}
import models.auth.AuthenticatedRequest
import play.api.mvc.Request
import uk.gov.hmrc.domain.EmpRef

trait AuthenticatedRequestSupport {

  private val defaultTaxOfficeNumber    = "taxOfficeNumber"
  private val defaultTaxOfficeReference = "taxOfficeReference"

  private val defaultUserId = Some("test_user_id")

  def createEmpRef(
    taxOfficeNumber: String = defaultTaxOfficeNumber,
    taxOfficeReference: String = defaultTaxOfficeReference
  ): EmpRef =
    EmpRef(taxOfficeNumber, taxOfficeReference)

  private val defaultClient: Option[Client] = Some(
    Client(createEmpRef(), AccountsOfficeReference("123", "A", "B", "C"), Some("clientName"), lpAuthorisation = true)
  )

  def createUserId(id: Option[String] = defaultUserId): Option[String] = id

  def createClient(client: Option[Client]): Option[Client] = client

  val organisationClient: Option[Client] = createClient(None)
  val agentClient: Option[Client]        = createClient(defaultClient)

  def createAuthenticatedRequest[A](
    request: Request[A],
    empRef: EmpRef = createEmpRef(),
    userId: Option[String] = createUserId(),
    client: Option[Client] = defaultClient
  ): AuthenticatedRequest[A] =
    AuthenticatedRequest(
      empRef,
      userId,
      request,
      client
    )

}

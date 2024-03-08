/*
 * Copyright 2023 HM Revenue & Customs
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

package models.agent

import play.api.libs.json.{Json, OFormat, Reads, Writes}
import uk.gov.hmrc.domain.EmpRef

import java.time.LocalDateTime

case class Client(
  empRef: EmpRef,
  accountsOfficeReference: AccountsOfficeReference,
  name: Option[String] = None,
  lpAuthorisation: Boolean,
  agentClientRef: Option[String] = None,
  requestedDeletionOn: Option[LocalDateTime] = None
)

object Client {
  implicit val clientReads: Reads[Client]   = Json.reads[Client]
  implicit val clientWrites: Writes[Client] = Json.writes[Client]
}

object AccountsOfficeReference {
  implicit val format: OFormat[AccountsOfficeReference] = Json.format[AccountsOfficeReference]
}

case class AccountsOfficeReference(districtNumber: String, payType: String, checkCode: String, reference: String) {

  lazy val formatted: String = {
    val referenceLength = 8
    val zeros           = "0" * (referenceLength - reference.length)
    s"$districtNumber$payType$checkCode$zeros$reference"
  }
}

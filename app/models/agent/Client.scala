/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText}
import uk.gov.hmrc.domain.EmpRef

import java.time.LocalDateTime

case class Client(
  empRef: EmpRef,
  accountsOfficeReference: AccountsOfficeReference,
  name: Option[String] = None,
  lpAuthorisation: Boolean,
  agentClientRef: Option[String] = None,
  requestedDeletionOn: Option[LocalDateTime] = None
) {
  def encrypt(implicit encrypter: Encrypter): EncryptedClient = Client.encrypt(this)
}

case class EncryptedClient(
  empRef: String,
  accountsOfficeReference: AccountsOfficeReference,
  name: Option[String] = None,
  lpAuthorisation: Boolean,
  agentClientRef: Option[String] = None,
  requestedDeletionOn: Option[LocalDateTime] = None
) {
  def decrypt(implicit decrypter: Decrypter): Client = EncryptedClient.decrypt(this)
}

object Client {
  implicit val clientReads: Reads[Client]   = Json.reads[Client]
  implicit val clientWrites: Writes[Client] = Json.writes[Client]

  def encrypt(client: Client)(implicit encrypter: Encrypter): EncryptedClient =
    EncryptedClient(
      empRef = encrypter.encrypt(PlainText(client.empRef.value)).value,
      accountsOfficeReference = AccountsOfficeReference.encrypt(client.accountsOfficeReference),
      name = client.name.map(name => encrypter.encrypt(PlainText(name)).value),
      lpAuthorisation = client.lpAuthorisation,
      agentClientRef = client.agentClientRef.map(agentClientRef => encrypter.encrypt(PlainText(agentClientRef)).value),
      requestedDeletionOn = client.requestedDeletionOn
    )
}

object EncryptedClient {
  implicit val encryptedClientReads: Reads[EncryptedClient]   = Json.reads[EncryptedClient]
  implicit val encryptedClientWrites: Writes[EncryptedClient] = Json.writes[EncryptedClient]

  def decrypt(eClient: EncryptedClient)(implicit decrypter: Decrypter): Client =
    Client(
      empRef = EmpRef.fromIdentifiers(decrypter.decrypt(Crypted(eClient.empRef)).value),
      accountsOfficeReference = AccountsOfficeReference.decrypt(eClient.accountsOfficeReference),
      name = eClient.name.map(name => decrypter.decrypt(Crypted(name)).value),
      lpAuthorisation = eClient.lpAuthorisation,
      agentClientRef = eClient.agentClientRef.map(agentClientRef => decrypter.decrypt(Crypted(agentClientRef)).value),
      requestedDeletionOn = eClient.requestedDeletionOn
    )
}

case class AccountsOfficeReference(districtNumber: String, payType: String, checkCode: String, reference: String)

object AccountsOfficeReference {
  implicit val format: OFormat[AccountsOfficeReference] = Json.format[AccountsOfficeReference]

  def encrypt(accountsOfficeReference: AccountsOfficeReference)(implicit
    encrypter: Encrypter
  ): AccountsOfficeReference =
    accountsOfficeReference.copy(
      districtNumber = encrypter.encrypt(PlainText(accountsOfficeReference.districtNumber)).value,
      payType = encrypter.encrypt(PlainText(accountsOfficeReference.payType)).value,
      checkCode = encrypter.encrypt(PlainText(accountsOfficeReference.checkCode)).value,
      reference = encrypter.encrypt(PlainText(accountsOfficeReference.reference)).value
    )

  def decrypt(accountsOfficeReference: AccountsOfficeReference)(implicit
    decrypter: Decrypter
  ): AccountsOfficeReference =
    accountsOfficeReference.copy(
      districtNumber = decrypter.decrypt(Crypted(accountsOfficeReference.districtNumber)).value,
      payType = decrypter.decrypt(Crypted(accountsOfficeReference.payType)).value,
      checkCode = decrypter.decrypt(Crypted(accountsOfficeReference.checkCode)).value,
      reference = decrypter.decrypt(Crypted(accountsOfficeReference.reference)).value
    )
}

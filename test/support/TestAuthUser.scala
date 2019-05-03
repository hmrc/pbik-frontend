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

package support

import uk.gov.hmrc.domain.{CtUtr, EmpRef}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}

trait TestAuthUser {

  def createDummyUser(userId : String) : AuthContext = {
    val epayeAccount = Some(EpayeAccount(empRef = EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference ="taxOfficeReference" ), link =""))
    val accounts = Accounts(epaye = epayeAccount)
    val authority = new Authority("", accounts,None,None, CredentialStrength.None, ConfidenceLevel.L50, None, None, None, legacyOid = "testOId")
    val user = LoggedInUser(userId = userId, None, None, None, CredentialStrength.None,  ConfidenceLevel.L50, oid = "testOId")
    val principal = Principal(name = Some("EPaye User"), accounts)
    new AuthContext(user, principal, None, None, None, None)
  }

  def createDummyNonEpayeUser(userId : String) : AuthContext = {
    val ctAccount = Some(CtAccount(utr = CtUtr(utr = ""), link = ""))
    val accounts = Accounts(ct = ctAccount)
    val authority = new Authority("", accounts, None, None, CredentialStrength.None, ConfidenceLevel.L50, None, None, None, legacyOid = "testOId")

    val user = LoggedInUser(userId = userId, None, None, None, CredentialStrength.None,  ConfidenceLevel.L50, oid = "testOId")
    val principal = Principal(name = Some("CT User"), accounts)
    new AuthContext(user, principal, None, None, None, None)
  }

  def createDummyNonGatewayUser(userId : String) : AuthContext = {
    val ctAccount = Some(CtAccount(utr = CtUtr(utr=""), link =""))
    val accounts = Accounts(ct = ctAccount)
    val authority = new Authority("", accounts,None,None, CredentialStrength.None, ConfidenceLevel.L50, None, None, None, legacyOid = "testOId")

    val user = LoggedInUser(userId = userId, None, None, None, CredentialStrength.None, ConfidenceLevel.L50, oid = "testOId")
    val principal = Principal(name = None, accounts)
    new AuthContext(user, principal, None, None, None, None)
  }
}

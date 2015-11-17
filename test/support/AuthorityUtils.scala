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

package support

import uk.gov.hmrc.domain.{CtUtr, Nino, EmpRef, SaUtr}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._


object AuthorityUtils {

  def epayeAuthority(id: String, empRef: String): Authority =
    Authority(s"/auth/oid/$id", Accounts(epaye = Some(EpayeAccount(s"/epaye/$empRef", EmpRef.fromIdentifiers(empRef)))), None, None,
      confidenceLevel = ConfidenceLevel.L50)

  def payeAuthority(id: String, nino: String) =
    Authority(s"/auth/oid/$id", Accounts(paye = Some(PayeAccount(s"/paye/$nino", Nino(nino)))), None, None,
      confidenceLevel = ConfidenceLevel.L50)

  def ctAuthority(id: String, utr: String): Authority =
    Authority(s"/auth/oid/$id", Accounts(ct = Some(CtAccount(s"/ct/$utr", CtUtr(utr)))), None, None,
      confidenceLevel = ConfidenceLevel.L50)
}
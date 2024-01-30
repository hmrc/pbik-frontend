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

/**
  * The Header Tags are used to control a simple optimistic lock feature which prevents race conditions.
  * Each time a call is made for an employer-specific URL, the current value of the current optimistic lock is returned
  * That value should be send back each time we wish to change the employer record ( i.e by updating the registered benefits or
  * excluding an individual ). On receipt by the server, if the Etag value does not match, the update will be rejected, indicating some other source
  * has changed the record, thereby invalidating our changes.
  */
object HeaderTags {
  val ETAG: String   = "ETag"
  val X_TXID: String = "X-TXID"

  val ETAG_DEFAULT_VALUE: String   = "0"
  val X_TXID_DEFAULT_VALUE: String = "1"

  def createResponseHeaders(
    etag: String = ETAG_DEFAULT_VALUE,
    txid: String = X_TXID_DEFAULT_VALUE
  ): Map[String, String] = Map(ETAG -> etag, X_TXID -> txid)

}

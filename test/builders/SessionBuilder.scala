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

package builders

import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys

import java.util.UUID

object SessionBuilder {

  def updateRequestWithSession(fakeRequest: FakeRequest[AnyContentAsJson]): FakeRequest[AnyContentAsJson] = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(SessionKeys.sessionId -> sessionId)
  }

  def buildRequestWithSession(): FakeRequest[AnyContentAsEmpty.type] = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(SessionKeys.sessionId -> sessionId)
  }
}

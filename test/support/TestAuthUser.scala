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

import models.{AuthenticatedRequest, EmpRef, UserName}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.retrieve.Name

trait TestAuthUser {

  def createDummyUser[A](request:Request[A]):AuthenticatedRequest[A] =
  AuthenticatedRequest(EmpRef("taxOfficeNumber", "taxOfficeReference"), UserName(Name(Some("EPaye User"), None)), request)

}

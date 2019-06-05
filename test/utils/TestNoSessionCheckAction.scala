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

package utils

import controllers.actions.NoSessionCheckAction
import javax.inject.Inject
import models.AuthenticatedRequest
import play.api.mvc.{BodyParsers, Result}

import scala.concurrent.{ExecutionContext, Future}

class TestNoSessionCheckAction @Inject()(val parser: BodyParsers.Default)
                                        (implicit val executionContext: ExecutionContext) extends NoSessionCheckAction {
  override def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
      Future.successful(Right(request))
  }
}

/*
 * Copyright 2025 HM Revenue & Customs
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

package models.bindable

import models.v1.IabdType
import models.v1.IabdType._
import play.api.mvc.PathBindable

import scala.util.Try

object Bindables {

  implicit val pathBinder: PathBindable[IabdType] = new PathBindable[IabdType] {
    override def bind(key: String, value: String): Either[String, IabdType] =
      Try(value.toInt).toOption.map(iabdTypeInt => IabdType(iabdTypeInt)) match {
        case Some(iabdType) => Right(iabdType)
        case None           => Left(s"Invalid IabdType $value")
      }
    override def unbind(key: String, iabdType: IabdType): String            = iabdType.id.toString
  }
}

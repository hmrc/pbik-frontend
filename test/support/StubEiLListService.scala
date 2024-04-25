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

import config.PbikAppConfig
import connectors.PbikConnector
import models.{AuthenticatedRequest, EiLPerson}
import services.EiLListService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubEiLListService @Inject() (
  pbikAppConfig: PbikAppConfig,
  tierConnector: PbikConnector
)(implicit ec: ExecutionContext)
    extends EiLListService(pbikAppConfig, tierConnector) {

  private lazy val ListOfPeople: List[EiLPerson] = List(
    EiLPerson("AA111111", "John", Some("Stones"), "Smith", Some("123"), Some("01/01/1980"), Some("male"), Some(status)),
    EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None),
    EiLPerson(
      "AC111111",
      "Humpty",
      Some("Alexander"),
      "Dumpty",
      Some("123"),
      Some("01/01/1980"),
      Some("male"),
      Some(status)
    ),
    EiLPerson("AD111111", "Peter", Some("James"), "Johnson", None, None, None, None),
    EiLPerson(
      "AE111111",
      "Alice",
      Some("In"),
      "Wonderland",
      Some("123"),
      Some("03/02/1978"),
      Some("female"),
      Some(status)
    ),
    EiLPerson(
      "AF111111",
      "Humpty",
      Some("Alexander"),
      "Dumpty",
      Some("123"),
      Some("01/01/1980"),
      Some("male"),
      Some(status)
    )
  )
  val status                                     = 10

  override def currentYearEiL(iabdType: String, year: Int)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[List[EiLPerson]] =
    Future.successful(ListOfPeople)
}

class StubEiLListServiceOneExclusion @Inject() (
  pbikAppConfig: PbikAppConfig,
  tierConnector: PbikConnector
)(implicit ec: ExecutionContext)
    extends StubEiLListService(pbikAppConfig, tierConnector) {

  private lazy val ListOfPeople: List[EiLPerson] = List(
    EiLPerson("AA111111", "John", Some("Stones"), "Smith", Some("123"), Some("01/01/1980"), Some("male"), Some(status)),
    EiLPerson("AB111111", "Adam", None, "Smith", None, Some("01/01/1980"), Some("male"), None),
    EiLPerson(
      "AC111111",
      "Humpty",
      Some("Alexander"),
      "Dumpty",
      Some("123"),
      Some("01/01/1980"),
      Some("male"),
      Some(status)
    ),
    EiLPerson("AD111111", "Peter", Some("James"), "Johnson", None, None, None, None),
    EiLPerson(
      "AE111111",
      "Alice",
      Some("In"),
      "Wonderland",
      Some("123"),
      Some("03/02/1978"),
      Some("female"),
      Some(status)
    ),
    EiLPerson(
      "AF111111",
      "Humpty",
      Some("Alexander"),
      "Dumpty",
      Some("123"),
      Some("01/01/1980"),
      Some("male"),
      Some(status)
    )
  )

  override def currentYearEiL(iabdType: String, year: Int)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[List[EiLPerson]] =
    Future.successful(List(ListOfPeople.head))
}

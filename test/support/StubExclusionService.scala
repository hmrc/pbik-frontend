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

package support

import connectors.PbikConnector
import models.v1.IabdType.IabdType
import models.v1.exclusion.{PbikExclusionPerson, PbikExclusions}
import services.ExclusionService
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubExclusionService @Inject() (tierConnector: PbikConnector)(implicit ec: ExecutionContext)
    extends ExclusionService(tierConnector) {

  override def exclusionListForYear(iabdType: IabdType, year: Int, empRef: EmpRef)(implicit
    hc: HeaderCarrier
  ): Future[PbikExclusions] =
    Future.successful(
      PbikExclusions(
        0,
        Some(
          List(
            PbikExclusionPerson("AB123456A", "John", Some("A"), "Doe", Some("12345"), 11),
            PbikExclusionPerson("AB123456B", "John", Some("A"), "Smith", Some("12345"), 22),
            PbikExclusionPerson("AB123456C", "Victor", Some("A"), "Doe", Some("12345"), 33),
            PbikExclusionPerson("AB123456D", "Victor", Some("A"), "Doe", Some("12345"), 44)
          )
        )
      )
    )
}

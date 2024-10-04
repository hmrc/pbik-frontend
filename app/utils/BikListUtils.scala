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

package utils

import models.v1.BenefitInKindWithCount
import models.v1.IabdType.IabdType
import models.{RegistrationItem, RegistrationList}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Request

import javax.inject.{Inject, Singleton}

@Singleton
class BikListUtils @Inject() (val messagesApi: MessagesApi) extends I18nSupport {

  def sortAlphabeticallyByLabels[A](
    biks: List[BenefitInKindWithCount]
  )(implicit request: Request[A]): List[BenefitInKindWithCount] = {
    val listOfIdLabelPairs: List[(BenefitInKindWithCount, String)]       =
      biks.map(bik => (bik, Messages("BenefitInKind.label." + bik.iabdType.id)))
    val sortedListOfIdLabelPairs: List[(BenefitInKindWithCount, String)] =
      listOfIdLabelPairs.sortWith((bik1: (BenefitInKindWithCount, String), bik2: (BenefitInKindWithCount, String)) =>
        bik1._2 < bik2._2
      )
    sortedListOfIdLabelPairs.map(pair => pair._1)
  }

  def sortRegistrationsAlphabeticallyByLabels[A](
    registeredBiksList: RegistrationList
  )(implicit request: Request[A]): RegistrationList = {
    val listOfIdLabelPairs: List[(IabdType, String)]       =
      registeredBiksList.active.map(bik => (bik.iabdType, Messages("BenefitInKind.label." + bik.iabdType.id)))
    val sortedListOfIdLabelPairs: List[(IabdType, String)] =
      listOfIdLabelPairs.sortWith((bik1: (IabdType, String), bik2: (IabdType, String)) => bik1._2 < bik2._2)
    val registrationItemList: List[RegistrationItem]       =
      sortedListOfIdLabelPairs.map(bik => RegistrationItem(bik._1, active = false, enabled = true))
    RegistrationList(registeredBiksList.selectAll, registrationItemList)
  }

  /** Returns a RegistrationList containing those Biks who do not appear in both lists
    * @param initialList
    *   Normally the superset list of of Biks ( such as the total Biks that can be registered )
    * @param checkedList
    *   Normally the subset of Biks ( such as those already registered )
    * @return
    *   RegistrationList containing the intersection of the Bik Lists with additional attributes set
    */
  def removeMatches(initialList: Set[IabdType], checkedList: Set[IabdType]): RegistrationList = {
    val diff: Set[RegistrationItem] = initialList
      .diff(checkedList)
      .map(x => RegistrationItem(x, active = false, enabled = true))
    RegistrationList(None, diff.toList)

  }

}

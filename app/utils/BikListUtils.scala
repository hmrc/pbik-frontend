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

import models.v1.IabdType.IabdType
import models.v1.PbikAction
import models.{Bik, RegistrationItem, RegistrationList}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Request

import javax.inject.{Inject, Singleton}

@Singleton
class BikListUtils @Inject() (val messagesApi: MessagesApi) extends I18nSupport {

  /** sort the input list according to the labels in the messages files and create a new list based on this order
    * @param biks
    * @return
    */
  def sortAlphabeticallyByLabels[A](biks: List[Bik])(implicit request: Request[A]): List[Bik] = {
    val listOfIdLabelPairs: List[(Bik, String)]       =
      biks.map(bik => (bik, Messages("BenefitInKind.label." + bik.iabdType)))
    val sortedListOfIdLabelPairs: List[(Bik, String)] =
      listOfIdLabelPairs.sortWith((bik1: (Bik, String), bik2: (Bik, String)) => bik1._2 < bik2._2)
    sortedListOfIdLabelPairs.map(pair => pair._1)
  }

  /** sort the input list according to the labels in the messages files and create a new list based on this order
    * @param registeredBiksList
    * @return
    */
  def sortRegistrationsAlphabeticallyByLabels[A](
    registeredBiksList: RegistrationList
  )(implicit request: Request[A]): RegistrationList = {
    val listOfIdLabelPairs: List[(String, String)]       =
      registeredBiksList.active.map(bik => (bik.id, Messages("BenefitInKind.label." + bik.id)))
    val sortedListOfIdLabelPairs: List[(String, String)] =
      listOfIdLabelPairs.sortWith((bik1: (String, String), bik2: (String, String)) => bik1._2 < bik2._2)
    val registrationItemList: List[RegistrationItem]     =
      sortedListOfIdLabelPairs.map(bik => RegistrationItem(bik._1, active = false, enabled = true))
    RegistrationList(registeredBiksList.selectAll, registrationItemList)
  }

  /** normalise the selection list to match the protocol agreed with the NPS backend team
    * @param registeredBiks
    * @param selectedBiks
    * @return
    */
  def normaliseSelectedBenefits(registeredBiks: Set[Bik], selectedBiks: List[Bik]): List[Bik] = {

    val selectedBiksToRemove = selectedBiks.filter(x => x.status == PbikAction.RemovePayrolledBenefitInKind.id)

    val biksToRemove = registeredBiks.foldLeft(List.empty[Bik])((acc, bik) =>
      if (selectedBiksToRemove.contains(bik)) {
        Bik(bik.iabdType, PbikAction.RemovePayrolledBenefitInKind.id, bik.eilCount) :: acc
      } else {
        acc
      }
    )

    val biksToAdd =
      selectedBiks.filter(x => x.status == PbikAction.ReinstatePayrolledBenefitInKind.id && !registeredBiks.contains(x))

    biksToRemove ++ biksToAdd

  }

  /** Merges two lists of Biks. If the Bik exists in both lists, a registrationItem is created which is marked as
    * already active ( checked in a checkbox ) and its enabled flag is set to false ( which would normally indicate it
    * can't be unselected in a checkbox )
    * @param initialList
    *   Normally the superset list of of Biks ( such as the total Biks that can be registered )
    * @param checkedList
    *   Normally the subset of Biks ( such as those already registered )
    * @return
    *   RegistrationList containing the union of the Bik Lists with additional attributes set
    */
  def mergeSelected(initialList: List[Bik], checkedList: List[Bik]): RegistrationList = {

    val items: List[RegistrationItem] = initialList map {
      case a if checkedList.contains(a) => RegistrationItem(a.iabdType, active = true, enabled = false)
      case bik                          => RegistrationItem(bik.iabdType, active = false, enabled = true)
    }
    RegistrationList(None, items)
  }

  /** Returns a RegistrationList containing those Biks who do not appear in both lists
    * @param initialList
    *   Normally the superset list of of Biks ( such as the total Biks that can be registered )
    * @param checkedList
    *   Normally the subset of Biks ( such as those already registered )
    * @return
    *   RegistrationList containing the intersection of the Bik Lists with additional attributes set
    */
  def removeMatches(initialList: Set[IabdType], checkedList: Set[Bik]): RegistrationList = {
    val diff: Set[Int] = initialList.map(_.id).diff(checkedList.map(_.iabdType.toInt))
    RegistrationList(
      None,
      diff.map { x =>
        RegistrationItem(x.toString, active = false, enabled = true)
      }.toList
    )

  }

}

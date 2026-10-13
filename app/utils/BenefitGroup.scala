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

package utils

import models.v1.*

object BenefitGroup {

  private val group1IabdTypes     = Set(
    IabdType.CarBenefit,
    IabdType.CarFuelBenefit,
    IabdType.VanBenefit,
    IabdType.VanFuelBenefit,
    IabdType.MedicalInsurance
  )
  private val group1Ids: Set[Int] = group1IabdTypes.map(_.id)

  private val group2IabdTypes     = Set(
    IabdType.Assets,
    IabdType.AssetTransfer,
    IabdType.Entertaining,
    IabdType.IncomeTaxPaidButNotDeductedFromDirectorRemuneration,
    IabdType.Mileage,
    IabdType.NonQualifyingRelocationExpenses,
    IabdType.Telephone,
    IabdType.PaymentsOnEmployeeBehalf,
    IabdType.QualifyingRelocationExpenses,
    IabdType.EmployerProvidedServices,
    IabdType.TravelAndSubsistence,
    IabdType.VouchersAndCreditCards,
    IabdType.OtherItems
  )
  private val group2Ids: Set[Int] = group2IabdTypes.map(_.id)

  private val group3IabdTypes     = Set(IabdType.Accommodation, IabdType.BeneficialLoan)
  private val group3Ids: Set[Int] = group3IabdTypes.map(_.id)

  def isGroup1(benefitId: Int): Boolean = group1Ids.contains(benefitId)

  def isGroup2(benefitId: Int): Boolean = group2Ids.contains(benefitId)

  def isGroup3(benefitId: Int): Boolean = group3Ids.contains(benefitId)
}

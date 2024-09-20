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

package models.v1

import models.v1.IabdType.IabdType
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IabdTypeSpec extends AnyWordSpec with Matchers {

  private val iabdInUrlParamValues: Set[(String, IabdType)] = Set(
    "Asset-Transfer"                                                -> IabdType.AssetTransfer,
    "Assets"                                                        -> IabdType.Assets,
    "Car-Benefit"                                                   -> IabdType.CarBenefit,
    "Car-Fuel-Benefit"                                              -> IabdType.CarFuelBenefit,
    "Employer-Provided-Services"                                    -> IabdType.EmployerProvidedServices,
    "Entertaining"                                                  -> IabdType.Entertaining,
    "Expenses"                                                      -> IabdType.Expenses,
    "Income-Tax-Paid-but-not-deducted-from-Director's-Remuneration" -> IabdType.IncomeTaxPaidButNotDeductedFromDirectorRemuneration,
    "Medical-Insurance"                                             -> IabdType.MedicalInsurance,
    "Mileage"                                                       -> IabdType.Mileage,
    "Non-qualifying-Relocation-Expenses"                            -> IabdType.NonQualifyingRelocationExpenses,
    "Other-Items"                                                   -> IabdType.OtherItems,
    "Payments-on-Employee's-Behalf"                                 -> IabdType.PaymentsOnEmployeeBehalf,
    "Qualifying-Relocation-Expenses"                                -> IabdType.QualifyingRelocationExpenses,
    "Telephone"                                                     -> IabdType.Telephone,
    "Travel-and-Subsistence"                                        -> IabdType.TravelAndSubsistence,
    "Van-Benefit"                                                   -> IabdType.VanBenefit,
    "Van-Fuel-Benefit"                                              -> IabdType.VanFuelBenefit,
    "Vouchers-and-Credit-Cards"                                     -> IabdType.VouchersAndCreditCards
  )

  "IabdType" when {

    ".fromUrlParam" must {

      "have all mappings for existing iabd types" in {
        iabdInUrlParamValues.map(_._2) mustBe IabdType.values
      }

      iabdInUrlParamValues.foreach { tuple =>
        val (urlParam, iabdType) = tuple
        s"map correct enum value from url param -> $urlParam to -> $iabdType" in {
          iabdType.convertToUrlParam mustBe urlParam
        }
      }

    }

  }
}

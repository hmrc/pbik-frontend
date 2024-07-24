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

import models.v1
import play.api.libs.json.{Format, Json}

object IabdType extends Enumeration {

  type IabdType = Value

  val AssetTransfer: v1.IabdType.Value                                       = Value(40, "Asset Transfer")
  val Assets: v1.IabdType.Value                                              = Value(39, "Assets")
  val CarBenefit: v1.IabdType.Value                                          = Value(31, "Car Benefit")
  val CarFuelBenefit: v1.IabdType.Value                                      = Value(29, "Car Fuel Benefit")
  val EmployerProvidedServices: v1.IabdType.Value                            = Value(8, "Employer Provided Services")
  val Entertaining: v1.IabdType.Value                                        = Value(42, "Entertaining")
  val Expenses: v1.IabdType.Value                                            = Value(43, "Expenses")
  val IncomeTaxPaidButNotDeductedFromDirectorRemuneration: v1.IabdType.Value =
    Value(52, "Income Tax Paid but not deducted from Director's Remuneration")
  val MedicalInsurance: v1.IabdType.Value                                    = Value(30, "Medical Insurance")
  val Mileage: v1.IabdType.Value                                             = Value(44, "Mileage")
  val NonQualifyingRelocationExpenses: v1.IabdType.Value                     = Value(45, "Non-qualifying Relocation Expenses")
  val OtherItems: v1.IabdType.Value                                          = Value(47, "Other Items")
  val PaymentsOnEmployeeBehalf: v1.IabdType.Value                            = Value(48, "Payments on Employee's Behalf")
  val QualifyingRelocationExpenses: v1.IabdType.Value                        = Value(50, "Qualifying Relocation Expenses")
  val Telephone: v1.IabdType.Value                                           = Value(32, "Telephone")
  val TravelAndSubsistence: v1.IabdType.Value                                = Value(53, "Travel and Subsistence")
  val VanBenefit: v1.IabdType.Value                                          = Value(35, "Van Benefit")
  val VanFuelBenefit: v1.IabdType.Value                                      = Value(36, "Van Fuel Benefit")
  val VouchersAndCreditCards: v1.IabdType.Value                              = Value(54, "Vouchers and Credit Cards")

  implicit val formats: Format[IabdType] = Json.formatEnum(this)

}

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

import models._
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import utils.FormMappings
import views.html._
import views.html.exclusion._
import views.html.registration._

class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {

  private val forms: FormMappings = app.injector.instanceOf[FormMappings]

  private val (cyMinus1, cy, cyPlus1): (Int, Int, Int) = (2019, 2020, 2021)

  private val (status1, status2): (Int, Int) = (10, 12)

  private def listOfBik(iabdType1: String, iabdType2: String): List[Bik] = List(
    Bik(
      iabdType = iabdType1,
      status = status1
    ),
    Bik(
      iabdType = iabdType2,
      status = status2
    )
  )

  private val listOfEiLPerson: List[EiLPerson] = List(
    EiLPerson(
      nino = "AB123456C",
      firstForename = "John",
      secondForename = Some("Smith"),
      surname = "Smith",
      worksPayrollNumber = Some("123/AB123456C"),
      dateOfBirth = None,
      gender = None,
      status = None,
      perOptLock = 1
    )
  )

  private val registrationList: RegistrationList = RegistrationList(
    active = List(
      RegistrationItem(
        id = "30",
        active = true,
        enabled = true
      )
    )
  )

  implicit val arbObjSelectedForm: Arbitrary[Form[RegistrationList]] = fixed(forms.objSelectedForm.fill(registrationList))
  implicit val arbIndividualSelectionForm: Arbitrary[Form[ExclusionNino]] = fixed(forms.individualSelectionForm)
  implicit val arbOtherReasonForm: Arbitrary[Form[OtherReason]] = fixed(forms.removalOtherReasonForm)
  implicit val arbRemovalReasonForm: Arbitrary[Form[BinaryRadioButtonWithDesc]] = fixed(forms.removalReasonForm)
  implicit val arbBinaryRadioButtonForm: Arbitrary[Form[MandatoryRadioButton]] = fixed(forms.binaryRadioButton)

  override implicit val arbAsciiString: Arbitrary[String] = fixed("assets-transferred")
  implicit val arbRegistrationList: Arbitrary[RegistrationList] = fixed(registrationList)
  implicit val arbListOfEiLPerson: Arbitrary[List[EiLPerson]] = fixed(listOfEiLPerson)
  implicit val arbEilPersonList: Arbitrary[EiLPersonList] = fixed(EiLPersonList(listOfEiLPerson))
  implicit val arbTaxYearRange: Arbitrary[TaxYearRange] = fixed(TaxYearRange(cyMinus1, cy, cyPlus1))
  implicit val arbEmpRef: Arbitrary[EmpRef] = fixed(EmpRef(taxOfficeNumber = "123", taxOfficeReference = "4567890"))

  override def renderViewByClass: PartialFunction[Any, Html] = {
    case enrol: Enrol                                                               => render(enrol)
    case errorPage: ErrorPage                                                       => render(errorPage)
    case errorTemplate: ErrorTemplate                                               => render(errorTemplate)
    case maintenancePage: MaintenancePage                                           => render(maintenancePage)
    case signedOut: SignedOut                                                       => render(signedOut)
    case startPage: StartPage                                                       => render(startPage)
    case summary: Summary                                                           => render(summary)(
      fixed(true), arbTaxYearRange, fixed(listOfBik("40", "48")), fixed(listOfBik("54", "38")),
      fixed(2), fixed(2), fixed("false"), arbEmpRef, arbRequest, arbMessages
    )
    case exclusionNinoOrNoNinoForm: ExclusionNinoOrNoNinoForm                       => render(exclusionNinoOrNoNinoForm)
    case exclusionOverview: ExclusionOverview                                       => render(exclusionOverview)
    case ninoExclusionSearchForm: NinoExclusionSearchForm                           =>
      implicit val exclusionSearchFormWithNino: Arbitrary[Form[EiLPerson]] = fixed(forms.exclusionSearchFormWithNino(fakeRequest))
      render(ninoExclusionSearchForm)
    case noNinoExclusionSearchForm: NoNinoExclusionSearchForm                       =>
      implicit val exclusionSearchFormWithoutNino: Arbitrary[Form[EiLPerson]] = fixed(forms.exclusionSearchFormWithoutNino(fakeRequest))
      render(noNinoExclusionSearchForm)
    case removalConfirmation: RemovalConfirmation                                   =>
      implicit val arbAsciiString: Arbitrary[String] = fixed("assets-transferred")
      render(removalConfirmation)
    case searchResults: SearchResults                                               => render(searchResults)
    case whatNextExclusion: WhatNextExclusion                                       => render(whatNextExclusion)
    case whatNextRescind: WhatNextRescind                                           =>
      implicit val arbAsciiString: Arbitrary[String] = fixed("assets-transferred")
      render(whatNextRescind)
    case page_not_found_template: page_not_found_template                           => render(page_not_found_template)
    case addBenefitConfirmationNextTaxYear: AddBenefitConfirmationNextTaxYear       => render(addBenefitConfirmationNextTaxYear)
    case confirmAddCurrentTaxYear: ConfirmAddCurrentTaxYear                         => render(confirmAddCurrentTaxYear)
    case confirmUpdateNextTaxYear: ConfirmUpdateNextTaxYear                         => render(confirmUpdateNextTaxYear)
    case currentTaxYear: CurrentTaxYear                                             => render(currentTaxYear)
    case nextTaxYear: NextTaxYear                                                   => render(nextTaxYear)
    case removeBenefitConfirmationNextTaxYear: RemoveBenefitConfirmationNextTaxYear =>
      implicit val arbAsciiString: Arbitrary[String] = fixed("assets-transferred")
      render(removeBenefitConfirmationNextTaxYear)
    case removeBenefitNextTaxYear: RemoveBenefitNextTaxYear                         =>
      implicit val arbRegistrationList: Arbitrary[RegistrationList] = fixed(RegistrationList(
        active = registrationList.active.map(_.copy(id = "assets-transferred"))
      ))
      implicit val arbAsciiString: Arbitrary[String] = fixed("assets-transferred")
      render(removeBenefitNextTaxYear)
    case removeBenefitOtherReason: RemoveBenefitOtherReason                         =>
      implicit val arbAsciiString: Arbitrary[String] = fixed("assets-transferred")
      render(removeBenefitOtherReason)
  }

  override def viewPackageName: String = "views.html"

  override def layoutClasses: Seq[Class[GovukLayoutWrapper]] = Seq(classOf[GovukLayoutWrapper])

  runAccessibilityTests()
}

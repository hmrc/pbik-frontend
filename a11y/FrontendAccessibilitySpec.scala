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
import models.agent.{AccountsOfficeReference, Client}
import models.auth.AuthenticatedRequest
import models.form._
import models.v1.IabdType.IabdType
import models.v1.exclusion.PbikExclusionPerson
import models.v1.{BenefitInKindWithCount, IabdType}
import org.scalacheck.{Arbitrary, Gen}
import play.api.data.Form
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import utils.FormMappings
import views.html._
import views.html.exclusion._
import views.html.registration._

import scala.util.Random

class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {

  private val forms: FormMappings = app.injector.instanceOf[FormMappings]

  private val (cyMinus1, cy, cyPlus1): (Int, Int, Int) = (2019, 2020, 2021)

  private def bikSize = new Random().between(0, IabdType.values.size)

  implicit val arbIabdType: Arbitrary[IabdType] = Arbitrary(Gen.oneOf(IabdType.values))

  implicit val arbBik: Arbitrary[BenefitInKindWithCount] =
    Arbitrary {
      for {
        iabd <- Gen.oneOf(IabdType.values)
      } yield BenefitInKindWithCount(iabd, 2)
    }

  implicit val arbListOfBiks: Arbitrary[List[BenefitInKindWithCount]] = Arbitrary {
    for {
      biks <- Gen.listOfN(bikSize, arbBik.arbitrary).map(_.distinct)
    } yield biks
  }

  private val exclusionPerson: PbikExclusionPerson =
    PbikExclusionPerson("AB123456C", "John", Some("A"), "Doe", Some("12345"), 22)

  private val registrationItem = RegistrationItem(iabdType = IabdType.MedicalInsurance, active = true, enabled = true)

  private val registrationList: RegistrationList = RegistrationList(active = List(registrationItem))

  val empRef: EmpRef = EmpRef(taxOfficeNumber = "123", taxOfficeReference = "4567890")

  val agentClient: Option[Client] = Some(
    Client(
      uk.gov.hmrc.domain.EmpRef(empRef.taxOfficeNumber, empRef.taxOfficeReference),
      AccountsOfficeReference("123AB12345678", "123", "A", "12345678"),
      Some("client test name"),
      lpAuthorisation = false,
      None,
      None
    )
  )

  val authenticatedRequest: AuthenticatedRequest[_] = AuthenticatedRequest(
    empRef,
    None,
    fakeRequest,
    agentClient
  )

  implicit val arbRequestHeader: Arbitrary[RequestHeader]                       = fixed(fakeRequest)
  implicit val arbObjSelectedForm: Arbitrary[Form[RegistrationList]]            = fixed(
    forms.objSelectedForm.fill(registrationList)
  )
  implicit val arbIndividualSelectionForm: Arbitrary[Form[ExclusionNino]]       = fixed(forms.individualSelectionForm)
  implicit val arbOtherReasonForm: Arbitrary[Form[OtherReason]]                 = fixed(forms.removalOtherReasonForm)
  implicit val arbRemovalReasonForm: Arbitrary[Form[BinaryRadioButtonWithDesc]] = fixed(forms.removalReasonForm)
  implicit val arbBinaryRadioButtonForm: Arbitrary[Form[MandatoryRadioButton]]  = fixed(forms.binaryRadioButton)
  implicit val arbSelectYearForm: Arbitrary[Form[SelectYear]]                   = fixed(forms.selectYearForm)

  implicit val arbRegistrationList: Arbitrary[RegistrationList]             = fixed(registrationList)
  implicit val arbExclusionPersonList: Arbitrary[List[PbikExclusionPerson]] = fixed(List(exclusionPerson))
  implicit val arbTaxYearRange: Arbitrary[TaxYearRange]                     = fixed(TaxYearRange(cyMinus1, cy, cyPlus1))
  implicit val arbEmpRef: Arbitrary[EmpRef]                                 = fixed(empRef)

  override def renderViewByClass: PartialFunction[Any, Html] = {
    case enrol: Enrol                                                               => render(enrol)
    case errorPage: ErrorPage                                                       => render(errorPage)
    case errorTemplate: ErrorTemplate                                               => render(errorTemplate)
    case maintenancePage: MaintenancePage                                           => render(maintenancePage)
    case signedOut: SignedOut                                                       => render(signedOut)
    case startPage: StartPage                                                       =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(startPage)
    case summary: Summary                                                           =>
      render(summary)(
        fixed(true),
        arbTaxYearRange,
        arbListOfBiks,
        arbListOfBiks,
        fixed(2),
        fixed(2),
        fixed(true),
        fixed(authenticatedRequest),
        arbMessages
      )
    case exclusionNinoOrNoNinoForm: ExclusionNinoOrNoNinoForm                       => render(exclusionNinoOrNoNinoForm)
    case exclusionOverview: ExclusionOverview                                       =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(exclusionOverview)
    case ninoExclusionSearchForm: NinoExclusionSearchForm                           =>
      implicit val exclusionSearchFormWithNino: Arbitrary[Form[NinoForm]] = fixed(
        forms.exclusionSearchFormWithNino(fakeRequest)
      )
      render(ninoExclusionSearchForm)
    case noNinoExclusionSearchForm: NoNinoExclusionSearchForm                       =>
      implicit val exclusionSearchFormWithoutNino: Arbitrary[Form[NoNinoForm]] = fixed(
        forms.exclusionSearchFormWithoutNino(fakeRequest)
      )
      render(noNinoExclusionSearchForm)
    case removalConfirmation: RemovalConfirmation                                   =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(removalConfirmation)
    case searchResults: SearchResults                                               => render(searchResults)
    case whatNextExclusion: WhatNextExclusion                                       =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(whatNextExclusion)
    case whatNextRescind: WhatNextRescind                                           =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(whatNextRescind)
    case page_not_found_template: page_not_found_template                           => render(page_not_found_template)
    case addBenefitConfirmationNextTaxYear: AddBenefitConfirmationNextTaxYear       =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(addBenefitConfirmationNextTaxYear)
    case confirmAddCurrentTaxYear: ConfirmAddCurrentTaxYear                         => render(confirmAddCurrentTaxYear)
    case confirmUpdateNextTaxYear: ConfirmUpdateNextTaxYear                         =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(confirmUpdateNextTaxYear)
    case currentTaxYear: CurrentTaxYear                                             => render(currentTaxYear)
    case nextTaxYear: NextTaxYear                                                   =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(nextTaxYear)
    case removeBenefitConfirmationNextTaxYear: RemoveBenefitConfirmationNextTaxYear =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(removeBenefitConfirmationNextTaxYear)
    case removeBenefitNextTaxYear: RemoveBenefitNextTaxYear                         =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]]   = fixed(authenticatedRequest)
      render(removeBenefitNextTaxYear)
    case removeBenefitOtherReason: RemoveBenefitOtherReason                         =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(removeBenefitOtherReason)
    case confirmRemoveNextTaxYear: ConfirmRemoveNextTaxYear                         =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(confirmRemoveNextTaxYear)
    case selectYearPage: SelectYearPage                                             =>
      implicit val arbRequest: Arbitrary[AuthenticatedRequest[_]] = fixed(authenticatedRequest)
      render(selectYearPage)
  }

  override def viewPackageName: String = "views.html"

  override def layoutClasses: Seq[Class[GovukLayoutWrapper]] = Seq(classOf[GovukLayoutWrapper])

  runAccessibilityTests()

}

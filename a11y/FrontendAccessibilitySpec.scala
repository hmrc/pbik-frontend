import models._
import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import utils.FormMappings
import views.html._
import views.html.exclusion._
import views.html.registration._

class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {

  private val forms: FormMappings = app.injector.instanceOf[FormMappings]

  implicit val objSelectedForm: Typeclass[Form[RegistrationList]] = fixed(forms.objSelectedForm)

  override def renderViewByClass: PartialFunction[Any, Html] = {
    case enrol: Enrol                                                               => render(enrol)
    case errorPage: ErrorPage                                                       =>
      errorPage.render(
        errorMessage = "Error message",
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        isCurrentTaxYear = true.toString,
        code = 100,
        pageHeading = "Page heading",
        backLink = "Back link",
        empRef = None,
        iabdType = "40",
        request = fakeRequest,
        messages = messages
      )
    case errorTemplate: ErrorTemplate                                               => render(errorTemplate)
    case maintenancePage: MaintenancePage                                           => render(maintenancePage)
    case signedOut: SignedOut                                                       => render(signedOut)
    case startPage: StartPage                                                       => render(startPage)
    case summary: Summary                                                           =>
      summary.render(
        cyAllowed = true,
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        registeredBenefitsCurrentYear = List(Bik("40", 10), Bik("48", 12)),
        registeredBenefitsNextYear = List(Bik("54", 10), Bik("38", 12)),
        serviceBiksCountCY = 2,
        serviceBiksCountCYP1 = 2,
        fromYTA = false.toString,
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case exclusionNinoOrNoNinoForm: ExclusionNinoOrNoNinoForm                       =>
      exclusionNinoOrNoNinoForm.render(
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        isCurrentTaxYear = true.toString,
        iabdType = "40",
        previousSelection = "nino",
        form = forms.binaryRadioButton,
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case exclusionOverview: ExclusionOverview                                       =>
      exclusionOverview.render(
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        isCurrentTaxYear = true.toString,
        iabdType = "40",
        current =
          List(EiLPerson("AB123456C", "John", Some("Smith"), "Smith", Some("123/AB123456C"), None, None, None, 1)),
        empRef = EmpRef("123", "4567890"),
        form = forms.binaryRadioButton,
        request = fakeRequest,
        messages = messages
      )
    case ninoExclusionSearchForm: NinoExclusionSearchForm                           =>
      ninoExclusionSearchForm.render(
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        isCurrentTaxYear = true.toString,
        iabdType = "40",
        searchForm = forms.exclusionSearchFormWithNino(fakeRequest),
        alreadyExists = false,
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case noNinoExclusionSearchForm: NoNinoExclusionSearchForm                       =>
      noNinoExclusionSearchForm.render(
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        isCurrentTaxYear = true.toString,
        iabdType = "40",
        searchForm = forms.exclusionSearchFormWithoutNino(fakeRequest),
        alreadyExists = false,
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case removalConfirmation: RemovalConfirmation                                   =>
      removalConfirmation.render(
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        year = 2020.toString,
        iabdType = "assets-transferred",
        individualsToRemove = EiLPersonList(
          List(EiLPerson("AB123456C", "John", Some("Smith"), "Smith", Some("123/AB123456C"), None, None, None, 1))
        ),
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case searchResults: SearchResults                                               =>
      searchResults.render(
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        year = 2020.toString,
        iabdType = "40",
        listOfMatches = EiLPersonList(
          List(EiLPerson("AB123456C", "John", Some("Smith"), "Smith", Some("123/AB123456C"), None, None, None, 1))
        ),
        listOfMatchesForm = forms.individualSelectionForm,
        formType = "Assets transferred",
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case whatNextExclusion: WhatNextExclusion                                       =>
      whatNextExclusion.render(
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        year = 2020.toString,
        iabdType = "40",
        name = "Assets transferred",
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case whatNextRescind: WhatNextRescind                                           =>
      whatNextRescind.render(
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        year = 2020.toString,
        iabdType = "assets-transferred",
        name = "Assets transferred",
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case page_not_found_template: page_not_found_template                           => render(page_not_found_template)
    case addBenefitConfirmationNextTaxYear: AddBenefitConfirmationNextTaxYear       =>
      render(addBenefitConfirmationNextTaxYear)
    case confirmAddCurrentTaxYear: ConfirmAddCurrentTaxYear                         =>
      confirmAddCurrentTaxYear.render(
        bikForm = forms.objSelectedForm.fill(
          RegistrationList(None, List(RegistrationItem("30", active = true, enabled = true)))
        ),
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case confirmUpdateNextTaxYear: ConfirmUpdateNextTaxYear                         =>
      confirmUpdateNextTaxYear.render(
        bikList = RegistrationList(active = List(RegistrationItem("30", active = true, enabled = true))),
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case currentTaxYear: CurrentTaxYear                                             => render(currentTaxYear)
    case nextTaxYear: NextTaxYear                                                   => render(nextTaxYear)
    case removeBenefitConfirmationNextTaxYear: RemoveBenefitConfirmationNextTaxYear =>
      removeBenefitConfirmationNextTaxYear.render(
        isCurrentYear = false,
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        bikList = RegistrationList(active = List(RegistrationItem("30", active = true, enabled = true))),
        iabdType = "assets-transferred",
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case removeBenefitNextTaxYear: RemoveBenefitNextTaxYear                         =>
      removeBenefitNextTaxYear.render(
        bikList = RegistrationList(active =
          List(
            RegistrationItem("assets-transferred", active = true, enabled = true),
            RegistrationItem("payments-employee", active = true, enabled = true)
          )
        ),
        removalBik = Some(RegistrationItem("assets-transferred", active = true, enabled = true)),
        taxYearRange = TaxYearRange(2019, 2020, 2021),
        form = forms.removalReasonForm,
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
    case removeBenefitOtherReason: RemoveBenefitOtherReason                         =>
      removeBenefitOtherReason.render(
        form = forms.removalOtherReasonForm,
        iabdType = "assets-transferred",
        empRef = EmpRef("123", "4567890"),
        request = fakeRequest,
        messages = messages
      )
  }

  override def viewPackageName: String = "views.html"

  override def layoutClasses: Seq[Class[_]] = Seq(classOf[GovukLayoutWrapper])

  runAccessibilityTests()
}

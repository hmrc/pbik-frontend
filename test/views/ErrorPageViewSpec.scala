/*
 * Copyright 2025 HM Revenue & Customs
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

package views

import models.auth.AuthenticatedRequest
import models.v1.IabdType
import models.v1.IabdType.IabdType
import org.jsoup.Jsoup
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.ErrorPage

class ErrorPageViewSpec extends PBIKViewSpec {

  val errorPageView: ErrorPage = injected[ErrorPage]

  private def view(
    code: Int = 0,
    isCurrentTaxYear: String = utils.FormMappingsConstants.CY,
    iabdType: Option[IabdType] = None
  )(implicit request: AuthenticatedRequest[?]): Html =
    errorPageView(
      errorMessage = "Some error occurred",
      taxYearRange = taxYearRange,
      code = code,
      isCurrentTaxYear = isCurrentTaxYear,
      iabdType = iabdType
    )

  "ServiceMessage view" must {

    "display the correct h1 for a given code" in {
      val doc = Jsoup.parse(view(63091)(organisationRequest).toString)
      doc.select("h1#title").text must include(messages("ServiceMessage.63091.h1"))
    }

    "display the correct h1 for CY with code < 0" in {
      val doc =
        Jsoup.parse(view(code = -1, isCurrentTaxYear = utils.FormMappingsConstants.CY)(organisationRequest).toString)
      doc.select("h1#title").text must include(
        messages("An error has occurred", taxYearRange.cyminus1.toString, taxYearRange.cy.toString)
      )
    }

    "display the correct h1 for CYP1 with code < 0" in {
      val doc =
        Jsoup.parse(view(code = -1, isCurrentTaxYear = utils.FormMappingsConstants.CYP1)(organisationRequest).toString)
      doc.select("h1#title").text must include(
        messages("An error has occurred", taxYearRange.cy.toString, taxYearRange.cyplus1.toString)
      )
    }

    "display the correct body text for code with extra script" in {
      val doc = Jsoup.parse(view(63091)(organisationRequest).toString)
      doc.select("p.govuk-body-l").html must include(messages("ServiceMessage.63091"))
      doc.select("script").size()       must be > 0
    }

    "display back link for iabdType if provided" in {
      val doc  = Jsoup.parse(view(iabdType = Some(IabdType.MedicalInsurance))(organisationRequest).toString)
      val link = doc.select("a#link-exclusion-back")
      link.text must include(messages("Service.back.excluded"))
    }

    "display the correct h1 for fallback case _" in {
      val doc = Jsoup.parse(view(code = -1, isCurrentTaxYear = "unknown")(organisationRequest).toString)
      doc.select("h1#title").text must include("Some error occurred")
    }

    "always display summary back link" in {
      val doc  = Jsoup.parse(view()(organisationRequest).toString)
      val link = doc.select("a#link-back-summary")
      link.text must include(messages("Service.back.overview"))
    }

  }

}

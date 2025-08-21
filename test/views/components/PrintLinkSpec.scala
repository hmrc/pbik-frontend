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

package views.components

import models.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.components.PrintLink

class PrintLinkSpec extends PBIKViewSpec {

  private val printLinkView = injected[PrintLink]

  def view(text: String)(implicit request: AuthenticatedRequest[?]): Html =
    printLinkView(text)

  "printLink view - organisation" must {
    implicit val html: Html = view("Print this page")(organisationRequest)

    "render the link with correct text" in {
      val doc = Jsoup.parse(html.toString)
      doc.select("a.govuk-link.hmrc-\\!-js-visible").text() mustBe "Print this page"
    }

    "have the correct govuk-body class" in {
      val doc = Jsoup.parse(html.toString)
      doc.select("p.govuk-body.govuk-\\!-display-none-print").size() mustBe 1
    }

    "have the hmrc print-link data-module" in {
      val doc = Jsoup.parse(html.toString)
      doc.select("a[data-module=hmrc-print-link]").size() mustBe 1
    }
  }

  "printLink view - agent" must {
    implicit val html: Html = view("Print")(agentRequest)

    "render the link with correct text for agent" in {
      val doc = Jsoup.parse(html.toString)
      doc.select("a.govuk-link.hmrc-\\!-js-visible").text() mustBe "Print"
    }
  }
}

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

package views.components

import models.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.components.PageTitleHeading

class PageTitleHeadingSpec extends PBIKViewSpec {

  private val headingView: PageTitleHeading = injected[PageTitleHeading]

  val testOrganisationRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      empRef = organisationRequest.empRef,
      client = organisationRequest.client,
      request = FakeRequest(),
      userId = Some("tester")
    )
  val testAgentRequest: AuthenticatedRequest[AnyContentAsEmpty.type]        =
    AuthenticatedRequest(
      empRef = agentRequest.empRef,
      client = agentRequest.client,
      request = FakeRequest(),
      userId = Some("tester")
    )

  def view(title: String)(implicit request: AuthenticatedRequest[AnyContentAsEmpty.type]): Html =
    headingView(title)

  "heading view - organisation" must {
    implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = testOrganisationRequest
    val html                                                           = view("Test Title")
    val doc: Document                                                  = Jsoup.parse(html.toString)

    "render the client name if defined" in
      testOrganisationRequest.client.flatMap(_.name).foreach { clientName =>
        doc.select("span#client-name").text() mustBe clientName
      }

    "render the empRef with govuk-caption-l class" in {
      doc.select("span#empref.govuk-caption-l").text() must include(testOrganisationRequest.empRef.toString)
    }

    "render the title with govuk-heading-xl class" in {
      doc.select("h1#title.govuk-heading-xl").text() mustBe "Test Title"
    }
  }

  "heading view - agent" must {
    implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = testAgentRequest
    val html                                                           = view("Test Title")
    val doc: Document                                                  = Jsoup.parse(html.toString)

    "render the client name if defined" in
      testAgentRequest.client.flatMap(_.name).foreach { clientName =>
        doc.select("span#client-name").text() mustBe clientName
      }

    "render the empRef with govuk-caption-l class" in {
      doc.select("span#empref.govuk-caption-l").text() must include(testAgentRequest.empRef.toString)
    }

    "render the title with govuk-heading-xl class" in {
      doc.select("h1#title.govuk-heading-xl").text() mustBe "Test Title"
    }
  }
}

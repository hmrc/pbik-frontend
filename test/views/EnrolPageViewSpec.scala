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
import play.twirl.api.Html
import views.helper.PBIKViewSpec
import views.html.{Enrol, StartPage}

class EnrolPageViewSpec extends PBIKViewSpec {

  val enrolPageView: Enrol = injected[Enrol]

  def view()(implicit request: AuthenticatedRequest[_]): Html = enrolPageView()

  "enrolPage - organisation" must {
    implicit val html: Html = view()(organisationRequest)

    behave like pageWithTitle(messages("Service.notEnrolled.title"))
    behave like pageWithHeader(messages("Service.notEnrolled.title"))

    behave like pageWithIdAndText(messages("Service.notEnrolled.text1"), "text1")
    behave like pageWithIdAndText(messages("Service.notEnrolled.text2"), "text2")

    behave like pageWithIdAndText(messages("Service.enrollmentLoop.list1"), "list1")
    behave like pageWithIdAndText(messages("Service.enrollmentLoop.list2"), "list2")

    behave like pageWithIdAndText(messages("Service.notEnrolled.text3"), "text3")
    behave like pageWithIdAndText(messages("Service.notEnrolled.text4"), "text4")

    behave like pageWithLink(
      messages("Service.notEnrolled.button"),
      "https://www.gov.uk/paye-online/enrol"
    )
  }

  "enrolPage - agent" must {
    implicit val html: Html = view()(agentRequest)

    behave like pageWithTitle(messages("Service.notEnrolled.title"))
    behave like pageWithHeader(messages("Service.notEnrolled.title"))

    behave like pageWithIdAndText(messages("Service.notEnrolled.text1"), "text1")
    behave like pageWithIdAndText(messages("Service.notEnrolled.text2"), "text2")

    behave like pageWithIdAndText(messages("Service.enrollmentLoop.list1"), "list1")
    behave like pageWithIdAndText(messages("Service.enrollmentLoop.list2"), "list2")

    behave like pageWithIdAndText(messages("Service.notEnrolled.text3"), "text3")
    behave like pageWithIdAndText(messages("Service.notEnrolled.text4"), "text4")

    behave like pageWithLink(
      messages("Service.notEnrolled.button"),
      "https://www.gov.uk/paye-online/enrol"
    )
  }

}

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

package views.templatemodels

import views.helper.PBIKViewSpec

class PageTitleSpec extends PBIKViewSpec {

  val postTitleText = s"- ${messages("Service.title")} - GOV.UK"
  val title         = "testTitle"

  "fullTitle" should {
    "return a title with post text when no form errors are present" in {
      val pageTitle = PageTitle(title)
      pageTitle.fullTitle() mustBe s"$title $postTitleText"
    }

    "return a title with error pre text and title post text when form errors are present" in {
      val pageTitle = PageTitle(title, formErrorsExist = true)
      pageTitle.fullTitle() mustBe s"${messages("Service.error")}: $title $postTitleText"
    }
  }

}

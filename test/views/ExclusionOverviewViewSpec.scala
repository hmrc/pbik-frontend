/*
 * Copyright 2019 HM Revenue & Customs
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

import models.{EmpRef, TaxYearRange}
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.FormMappings
import views.helper.PBIKViewSpec
import views.html.exclusion.ExclusionOverview

class ExclusionOverviewViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val formMappings = app.injector.instanceOf[FormMappings]
  val exclusionOverviewView = app.injector.instanceOf[ExclusionOverview]

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  override def view: Html = exclusionOverviewView(taxYearRange, "cyp1", "30", List(),EmpRef("", ""))

  "exclusionOverview" must {
    behave like pageWithTitle(messages("ExclusionOverview.title"))
    behave like pageWithHeader(messages("ExclusionOverview.title"))
    behave like pageWithLink(messages("Service.excludeanemployee"), "/payrollbik/cyp1/medical/exclude-employee-search")

  }
}

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

import config.{AppConfig, LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import controllers.ExternalUrls
import models.{EmpRef, TaxYearRange}
import play.twirl.api.Html
import utils.{TaxDateUtils, URIInformation}
import views.helper.PBIKViewSpec

class HomePageViewSpec extends PBIKViewSpec {

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  implicit val context: PbikContext = app.injector.instanceOf[PbikContext]
  implicit val taxDateUtils: TaxDateUtils = app.injector.instanceOf[TaxDateUtils]
  implicit val pbikAppConfig: PbikAppConfig = app.injector.instanceOf[PbikAppConfig]
  implicit val uriInformation: URIInformation = app.injector.instanceOf[URIInformation]
  implicit val externalURLs: ExternalUrls = app.injector.instanceOf[ExternalUrls]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val localFormPartialRetriever: LocalFormPartialRetriever = app.injector.instanceOf[LocalFormPartialRetriever]

  override def view: Html = views.html.overview(cyAllowed = true, taxYearRange, List(), List(), 200, 0, "", EmpRef("", ""))

  "overview" must {

    behave like pageWithTitle(messages("Overview.heading"))
    behave like pageWithHeader(messages("Overview.heading"))
    behave like pageWithHeaderH2(messages("Overview.next.heading", taxYearRange.cy.toString, taxYearRange.cyplus1.toString))
    behave like pageWithIdAndText(messages("Overview.next.lead.empty"), "no-benefits")
    behave like pageWithLink("Register a benefit or expense", "/payrollbik/cy/choose-benefit-expense")

  }

}

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

import config.{AppConfig, LocalFormPartialRetriever}
import controllers.ExternalUrls
import models.{EmpRef, TaxYearRange}
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.{FormMappings, URIInformation}
import views.helper.PBIKViewSpec
import views.html.exclusion.WhatNextExclusion


class WhatNextExclusionViewSpec extends PBIKViewSpec {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val formMappings: FormMappings = app.injector.instanceOf[FormMappings]
  val whatNextExclusionView: WhatNextExclusion = app.injector.instanceOf[WhatNextExclusion]

  def taxYearRange = TaxYearRange(2018, 2019, 2020)

  implicit val uriInformation: URIInformation = app.injector.instanceOf[URIInformation]
  implicit val externalURLs: ExternalUrls = app.injector.instanceOf[ExternalUrls]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val localFormPartialRetriever: LocalFormPartialRetriever = app.injector.instanceOf[LocalFormPartialRetriever]

  override def view: Html = whatNextExclusionView(taxYearRange, "cyp1", "30", "",  EmpRef("", ""))

  "whatNextAddRemove" must {
    behave like pageWithTitle(messages("whatNext.exclude.heading"))
    behave like pageWithHeader(messages("whatNext.exclude.heading"))
    behave like pageWithLink(messages("Service.back.overview.whatNext"), "/payrollbik/payrolled-benefits-expenses")
    behave like pageWithLink(messages("Service.finish.excluded"), "/payrollbik/cyp1/medical/excluded-employees")

  }

}

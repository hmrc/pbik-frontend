/*
 * Copyright 2021 HM Revenue & Customs
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

package support

import config.PbikAppConfig
import connectors.HmrcTierConnector
import models._
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import play.twirl.api.HtmlFormat
import services.{BikListService, RegistrationService}
import uk.gov.hmrc.http.HeaderCarrier
import utils._
import views.html.ErrorPage
import views.html.registration.{CurrentTaxYear, NextTaxYear}

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class StubbedRegistrationService @Inject()(
  bikListUtils: BikListUtils,
  formMappings: FormMappings,
  pbikAppConfig: PbikAppConfig,
  tierConnector: HmrcTierConnector,
  bikListService: BikListService,
  taxDateUtils: TaxDateUtils,
  controllersReferenceData: ControllersReferenceData,
  uRIInformation: URIInformation,
  override val messagesApi: MessagesApi,
  errorPageView: ErrorPage,
  currentTaxYearView: CurrentTaxYear,
  nextTaxYearView: NextTaxYear)
    extends RegistrationService(
      messagesApi,
      bikListUtils,
      formMappings,
      tierConnector,
      bikListService,
      taxDateUtils,
      controllersReferenceData,
      uRIInformation,
      pbikAppConfig,
      errorPageView
    ) with I18nSupport {

  val dateRange: TaxYearRange = taxDateUtils.getTaxYearRange()
  lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))
  lazy val CYRegistrationItems: List[RegistrationItem] =
    List.tabulate(21)(n => RegistrationItem("" + (n + 1), active = true, enabled = true))
  val registeredListOption = List.empty[Bik]
  val allRegisteredListOption: List[Bik] = CYCache
  val mockRegistrationItemList = List.empty[RegistrationItem]
  val mockFormRegistrationList: Form[RegistrationList] =
    formMappings.objSelectedForm.fill(RegistrationList(None, CYRegistrationItems))

  //TODO: Why is this test returning different views to the real code?
  override def generateViewForBikRegistrationSelection(
    year: Int,
    cachingSuffix: String,
    generateViewBasedOnFormItems: (
      Form[RegistrationList],
      Seq[RegistrationItem],
      Seq[Bik],
      Seq[Int],
      Seq[Int],
      Option[Int]) => HtmlFormat.Appendable)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[AnyContent]): Future[Result] =
    year match {
      case dateRange.cyminus1 => {
        Future.successful(
          Ok(
            currentTaxYearView(
              mockFormRegistrationList,
              dateRange,
              mockRegistrationItemList,
              allRegisteredListOption,
              nonLegislationBiks = List(0),
              decommissionedBiks = List(0),
              biksAvailableCount = Some(17),
              empRef = request.empRef
            )))
      }
      case _ => {
        Future.successful(
          Ok(
            nextTaxYearView(
              mockFormRegistrationList,
              additive = true,
              dateRange,
              mockRegistrationItemList,
              registeredListOption,
              nonLegislationBiks = List(0),
              decommissionedBiks = List(0),
              biksAvailableCount = Some(17),
              empRef = request.empRef
            )))
      }
    }

}

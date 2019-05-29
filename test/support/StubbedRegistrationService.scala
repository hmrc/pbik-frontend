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

package support

import config.{LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import connectors.HmrcTierConnector
import controllers.ExternalUrls
import javax.inject.Inject
import models._
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Result}
import play.api.{Configuration, Environment}
import play.twirl.api.HtmlFormat
import services.{BikListService, RegistrationService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ControllersReferenceData, FormMappings, TaxDateUtils, URIInformation}

import scala.concurrent.Future

class StubbedRegistrationService @Inject()(pbikAppConfig: PbikAppConfig,
                                           tierConnector: HmrcTierConnector,
                                           bikListService: BikListService,
                                           runModeConfiguration: Configuration,
                                           environment: Environment,
                                           taxDateUtils: TaxDateUtils,
                                           context: PbikContext,
                                           controllersReferenceData: ControllersReferenceData,
                                           uRIInformation: URIInformation,
                                           externalURLs: ExternalUrls,
                                           localFormPartialRetriever: LocalFormPartialRetriever,
                                           val messagesApi: MessagesApi) extends RegistrationService(

  tierConnector,
  bikListService,
  runModeConfiguration,
  environment,
  taxDateUtils,
  controllersReferenceData,
  uRIInformation)(
  pbikAppConfig,
  context,
  externalURLs,
  localFormPartialRetriever
) with FormMappings with I18nSupport {

  val dateRange: TaxYearRange = taxDateUtils.getTaxYearRange()
  lazy val CYCache: List[Bik] = List.tabulate(21)(n => Bik("" + (n + 1), 10))
  lazy val CYRegistrationItems: List[RegistrationItem] = List.tabulate(21)(n => RegistrationItem("" + (n + 1), active = true, enabled = true))
  val registeredListOption = List.empty[Bik]
  val allRegisteredListOption: List[Bik] = CYCache
  val mockRegistrationItemList = List.empty[RegistrationItem]
  val mockFormRegistrationList: Form[RegistrationList] = objSelectedForm.fill(RegistrationList(None, CYRegistrationItems))

  override def generateViewForBikRegistrationSelection(year: Int, cachingSuffix: String,
                                                       generateViewBasedOnFormItems: (Form[RegistrationList],
                                                         List[RegistrationItem], List[Bik], List[Int], List[Int], Option[Int]) => HtmlFormat.Appendable)
                                                      (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]):
  Future[Result] = {
    year match {
      case dateRange.cyminus1 => {
        Future.successful(Ok(views.html.registration.currentTaxYear(mockFormRegistrationList,
          dateRange,
          mockRegistrationItemList,
          allRegisteredListOption,
          nonLegislationBiks = List(0),
          decommissionedBiks = List(0),
          biksAvailableCount = Some(17),
          empRef = request.empRef)(implicitly, context,
          implicitly,
          externalURLs,
          pbikAppConfig,
          localFormPartialRetriever)))
      }
      case _ => {
        Future.successful(Ok(views.html.registration.nextTaxYear(mockFormRegistrationList,
          additive = true,
          dateRange,
          mockRegistrationItemList,
          registeredListOption,
          nonLegislationBiks = List(0),
          decommissionedBiks = List(0),
          biksAvailableCount = Some(17),
          empRef = request.empRef)(implicitly, context,
          implicitly,
          externalURLs,
          pbikAppConfig,
          localFormPartialRetriever))
        )
      }
    }
  }

}

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

package services

import config.{LocalFormPartialRetriever, PbikAppConfig, PbikContext}
import connectors.HmrcTierConnector
import controllers.ExternalUrls
import javax.inject.Inject
import models._
import play.api.Mode.Mode
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Result}
import play.api.{Configuration, Environment}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ControllersReferenceData, URIInformation, _}

import scala.concurrent.Future

class RegistrationService @Inject()(
                                    tierConnector: HmrcTierConnector,
                                    val bikListService: BikListService,
                                    val runModeConfiguration: Configuration,
                                    environment: Environment,
                                    taxDateUtils: TaxDateUtils,
                                    controllersReferenceData: ControllersReferenceData,
                                    uriInformation: URIInformation)(
                                    implicit val pbikAppConfig: PbikAppConfig,
                                    implicit val context: PbikContext,
                                    implicit val externalURLs: ExternalUrls,
                                    implicit val localFormPartialRetriever: LocalFormPartialRetriever
) extends FrontendController {
  val mode: Mode = environment.mode

  def generateViewForBikRegistrationSelection(year: Int, cachingSuffix: String,
                                              generateViewBasedOnFormItems: (Form[RegistrationList],
                                                List[RegistrationItem], List[Bik], List[Int], List[Int], Option[Int]) => HtmlFormat.Appendable)
                                             (implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]): Future[Result] = {

    val decommissionedBikIds: List[Int] = pbikAppConfig.biksDecommissioned
    val nonLegislationBiks: List[Int] = if (taxDateUtils.isCurrentTaxYear(year)) {
      pbikAppConfig.biksNotSupportedCY
    } else {
      pbikAppConfig.biksNotSupported
    }

    val isCurrentYear: String = {
      if (taxDateUtils.isCurrentTaxYear(year))
        FormMappingsConstants.CY
      else
        FormMappingsConstants.CYP1
    }

    for {
      biksListOption: List[Bik] <- bikListService.registeredBenefitsList(year, EmpRef.empty)(uriInformation.getBenefitTypesPath)
      registeredListOption <- tierConnector.genericGetCall[List[Bik]](uriInformation.baseUrl,
        uriInformation.getRegisteredPath,
        request.empRef,
        year)
      nonLegislationList = nonLegislationBiks.map { x =>
        Bik("" + x, 30, 0)
      }
      decommissionedBikList = decommissionedBikIds.map { x =>
        Bik("" + x, 30, 0)
      }

      // During transition, we have to ensure we handle the existing decommissioned IABDs (e.g 47 ) being sent by the server
      // and after the NPS R38 config release, when it wont be. Therefore, aas this is a list, we remove the
      // decommissioned values ( if they exist ) and then add them back in
      hybridList = biksListOption.filterNot(y => decommissionedBikIds.contains(y.iabdType.toInt)) ::: nonLegislationList ::: decommissionedBikList

    } yield {
      val pbikHeaders = bikListService.pbikHeaders
      val fetchFromCacheMapBiksValue = List.empty[RegistrationItem]

      val mergedData: RegistrationList = utils.BikListUtils.removeMatches(hybridList, registeredListOption)
      val sortedMegedData: RegistrationList = utils.BikListUtils.sortRegistrationsAlphabeticallyByLabels(mergedData)

      if (sortedMegedData.active.isEmpty) {
        Ok(views.html.errorPage(ControllersReferenceDataCodes.NO_MORE_BENEFITS_TO_ADD,
          controllersReferenceData.YEAR_RANGE,
          isCurrentYear,
          code = -1,
          pageHeading = ControllersReferenceDataCodes.NO_MORE_BENEFITS_TO_ADD_HEADING,
          empRef = Some(request.empRef)))
      } else {
        Ok(generateViewBasedOnFormItems(uriInformation.objSelectedForm.fill(sortedMegedData),
          fetchFromCacheMapBiksValue, registeredListOption, nonLegislationBiks, pbikAppConfig.biksDecommissioned, Some(biksListOption.size)))
      }
    }
  }

}

/*
 * Copyright 2024 HM Revenue & Customs
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

import config.PbikAppConfig
import connectors.PbikConnector
import models._
import models.v1.IabdType.IabdType
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import utils._
import views.html.ErrorPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject() (
  override val messagesApi: MessagesApi,
  bikListUtils: BikListUtils,
  formMappings: FormMappings,
  val tierConnector: PbikConnector,
  val bikListService: BikListService,
  taxDateUtils: TaxDateUtils,
  controllersReferenceData: ControllersReferenceData,
  pbikAppConfig: PbikAppConfig,
  errorPageView: ErrorPage
)(implicit val executionContext: ExecutionContext)
    extends I18nSupport {

  def generateViewForBikRegistrationSelection(
    year: Int,
    cachingSuffix: String,
    generateViewBasedOnFormItems: (
      Form[RegistrationList],
      Seq[RegistrationItem],
      Seq[Bik],
      Seq[Int],
      Seq[Int],
      Option[Int]
    ) => HtmlFormat.Appendable
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val decommissionedBikIds: Set[IabdType] = pbikAppConfig.biksDecommissioned
    val nonLegislationBiks: Set[IabdType]   = if (taxDateUtils.isCurrentTaxYear(year)) {
      pbikAppConfig.biksNotSupportedCY
    } else {
      pbikAppConfig.biksNotSupported
    }
    val isCurrentYear: String = {
      if (taxDateUtils.isCurrentTaxYear(year)) {
        FormMappingsConstants.CY
      } else {
        FormMappingsConstants.CYP1
      }
    }

    for {
      biksListOption       <- bikListService.getAllBenefitsForYear(year)
      registeredListOption <- tierConnector.getRegisteredBiks(request.empRef, year).map(_.bikList)
      nonLegislationList    = nonLegislationBiks
      decommissionedBikList = decommissionedBikIds

      // During transition, we have to ensure we handle the existing decommissioned IABDs (e.g 47 ) being sent by the server
      // and after the NPS R38 config release, when it wont be. Therefore, aas this is a list, we remove the
      // decommissioned values ( if they exist ) and then add them back in
      hybridList = biksListOption.diff(decommissionedBikIds) ++ nonLegislationList ++ decommissionedBikList

    } yield result(
      hybridList,
      registeredListOption.toList,
      nonLegislationBiks,
      biksListOption,
      isCurrentYear,
      generateViewBasedOnFormItems
    )
  }

  private def result(
    hybridList: Set[IabdType],
    registeredListOption: List[Bik],
    nonLegislationBiks: Set[IabdType],
    biksListOption: Set[IabdType],
    isCurrentYear: String,
    generateViewBasedOnFormItems: (
      Form[RegistrationList],
      Seq[RegistrationItem],
      Seq[Bik],
      Seq[Int],
      Seq[Int],
      Option[Int]
    ) => HtmlFormat.Appendable
  )(implicit request: AuthenticatedRequest[AnyContent]): Result = {
    val fetchFromCacheMapBiksValue = List.empty[RegistrationItem]

    val mergedData: RegistrationList      = bikListUtils.removeMatches(hybridList, registeredListOption.toSet)
    val sortedMegedData: RegistrationList = bikListUtils.sortRegistrationsAlphabeticallyByLabels(mergedData)

    if (sortedMegedData.active.isEmpty) {
      Ok(
        errorPageView(
          ControllersReferenceDataCodes.NO_MORE_BENEFITS_TO_ADD,
          controllersReferenceData.yearRange,
          isCurrentYear,
          code = -1,
          pageHeading = ControllersReferenceDataCodes.NO_MORE_BENEFITS_TO_ADD_HEADING
        )
      )
    } else {
      Ok(
        generateViewBasedOnFormItems(
          formMappings.objSelectedForm.fill(sortedMegedData),
          fetchFromCacheMapBiksValue,
          registeredListOption,
          nonLegislationBiks.map(_.id).toList, //TODO change view to Set instead of list it must be unique list
          pbikAppConfig.biksDecommissioned
            .map(_.id)
            .toList, //TODO change view to Set instead of list it must be unique list
          Some(biksListOption.size)
        )
      )
    }
  }

}

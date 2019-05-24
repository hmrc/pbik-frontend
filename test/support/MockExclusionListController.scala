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
import controllers.actions.{AuthAction, NoSessionCheckAction}
import controllers.{ExclusionListController, ExternalUrls}
import javax.inject.Inject
import models.{Bik, EiLPerson, EmpRef}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.concurrent.Futures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.{Configuration, Environment}
import services.{BikListService, EiLListService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{ControllersReferenceData, SplunkLogger, TaxDateUtils, URIInformation}

import scala.concurrent.Future

class MockExclusionListController @Inject()(pbikAppConfig: PbikAppConfig,
                                            authenticate: AuthAction,
                                            noSessionCheck: NoSessionCheckAction,
                                            eiLListService: EiLListService,
                                            bikListService: BikListService,
                                            tierConnector: HmrcTierConnector,
                                            runModeConfiguration: Configuration,
                                            environment:Environment,
                                            context: PbikContext,
                                            taxDateUtils: TaxDateUtils,
                                            splunkLogger: SplunkLogger,
                                            controllersReferenceData: ControllersReferenceData,
                                            uriInformation: URIInformation,
                                            externalURLs: ExternalUrls,
                                            localFormPartialRetriever: LocalFormPartialRetriever)
  extends ExclusionListController(
    authenticate,
    noSessionCheck,
    eiLListService,
    bikListService,
    tierConnector,
    runModeConfiguration,
    environment,
    taxDateUtils,
    splunkLogger,
    controllersReferenceData)(
    pbikAppConfig,
    context,
    uriInformation,
    externalURLs,
    localFormPartialRetriever) with Futures {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  def logSplunkEvent(dataEvent: DataEvent)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    Future.successful(AuditResult.Success)
  }

  when(tierConnector.genericPostCall[EiLPerson](anyString, Matchers.eq("31/exclusion/update"),
    any[EmpRef], anyInt, any[EiLPerson])(any[HeaderCarrier], any[Request[_]],
    any[json.Format[EiLPerson]])).thenReturn(Future.successful(new FakeResponse()))

  when(tierConnector.genericPostCall[EiLPerson](anyString, Matchers.eq("31/exclusion/remove"),
    any[EmpRef], anyInt, any[EiLPerson])(any[HeaderCarrier], any[Request[_]],
    any[json.Format[EiLPerson]])).thenReturn(Future.successful(new FakeResponse()))

  when(tierConnector.genericPostCall(anyString, Matchers.eq(uriInformation.updateBenefitTypesPath),
    any[EmpRef], anyInt, any[List[Bik]])(any[HeaderCarrier], any[Request[_]],
    any[json.Format[List[Bik]]])).thenReturn(Future.successful(new FakeResponse()))

  override lazy val exclusionsAllowed = true
}

class FakeResponse extends HttpResponse {
  override def status = 200

  override val json: JsValue = Json.parse(
    """[
                 {
                     "nino": "AB111111",
                     "firstForename": "Adam",
                    "surname": "Smith",
                     "worksPayrollNumber": "ABC123",
                     "dateOfBirth": "01/01/1980",
                     "gender": "male",
                     "status": 0,
                     "perOptLock": 0
                 }
             ]""")
}
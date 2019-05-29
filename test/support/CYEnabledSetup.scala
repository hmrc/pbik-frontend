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

import com.google.inject.AbstractModule
import config.AppConfig
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import org.mockito.Mockito.mock
import org.specs2.specification.Scope
import services.BikListService
import utils.{SplunkLogger, TestAuthAction, TestNoSessionCheckAction}

trait CYEnabledSetup extends Scope {

  object GuiceTestModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[HmrcTierConnector]).toInstance(mock(classOf[HmrcTierConnector]))
      bind(classOf[BikListService]).to(classOf[StubbedBikListService])
      bind(classOf[SplunkLogger]).to(classOf[TestSplunkLogger])
      bind(classOf[AppConfig]).toInstance(TestCYEnabledConfig)
      bind(classOf[AuthAction]).to(classOf[TestAuthAction])
      bind(classOf[NoSessionCheckAction]).to(classOf[TestNoSessionCheckAction])
    }
  }

}

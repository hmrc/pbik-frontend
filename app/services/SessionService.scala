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

package services

import config.PbikSessionCache

import javax.inject.{Inject, Singleton}
import models.{Bik, EiLPerson, PbikSession, RegistrationItem, RegistrationList}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SessionService @Inject()(val http: DefaultHttpClient, val sessionCache: PbikSessionCache) extends Logging {

  private object CacheKeys extends Enumeration {
    val RegistrationList, BikRemoved, ListOfMatches, EiLPerson, CurrentExclusions, CYRegisteredBiks, NYRegisteredBiks =
      Value
  }

  val PBIK_SESSION_KEY: String = "pbik_session"
  val cleanRegistrationList: Option[RegistrationList] = Some(RegistrationList(None, List.empty[RegistrationItem], None))
  val cleanBikRemoved: Option[RegistrationItem] = Some(RegistrationItem("", false, false))
  val cleanEiLPersonList: Option[List[EiLPerson]] = Some(List.empty[EiLPerson])
  val cleanEiLPerson: Option[EiLPerson] = Some(EiLPerson("", "", None, "", None, None, None, None))
  val cleanBikList: Option[List[Bik]] = Some(List.empty[Bik])
  val cleanSession: PbikSession = {
    PbikSession(
      cleanRegistrationList,
      cleanBikRemoved,
      cleanEiLPersonList,
      cleanEiLPerson,
      cleanEiLPersonList,
      cleanBikList,
      cleanBikList)
  }

  def fetchPbikSession()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    sessionCache
      .fetchAndGetEntry[PbikSession](PBIK_SESSION_KEY)
      .map {
        case Some(session) => Some(session)
        case None          => Some(cleanSession)
      }
      .recover {
        case ex: Exception =>
          logger.warn(s"[SessionService][fetchPbikSession] Fetch failed due to: $ex")
          Some(cleanSession)
      }

  def cacheRegistrationList(value: RegistrationList)(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.RegistrationList, Some(value))

  def cacheBikRemoved(value: RegistrationItem)(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.BikRemoved, Some(value))

  def cacheListOfMatches(value: List[EiLPerson])(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.ListOfMatches, Some(value))

  def cacheEiLPerson(value: EiLPerson)(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.EiLPerson, Some(value))

  def cacheCurrentExclusions(value: List[EiLPerson])(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.CurrentExclusions, Some(value))

  def cacheCYRegisteredBiks(value: List[Bik])(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.CYRegisteredBiks, Some(value))

  def cacheNYRegisteredBiks(value: List[Bik])(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.NYRegisteredBiks, Some(value))

  def resetRegistrationList()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.RegistrationList, cleanRegistrationList)

  def resetBikRemoved()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.BikRemoved, cleanBikRemoved)

  def resetListOfMatches()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.ListOfMatches, cleanEiLPersonList)

  def resetEiLPerson()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.EiLPerson, cleanEiLPerson)

  def resetCurrentExclusions()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.CurrentExclusions, cleanEiLPersonList)

  def resetCYRegisteredBiks()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.CYRegisteredBiks, cleanBikList)

  def resetNYRegisteredBiks()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.NYRegisteredBiks, cleanBikList)

  def resetAll()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] = {
    resetBikRemoved()
    resetEiLPerson()
    resetRegistrationList()
    resetListOfMatches()
    resetCurrentExclusions()
    resetCYRegisteredBiks()
    resetNYRegisteredBiks()
  }

  private def cache[T](key: CacheKeys.Value, value: Option[T] = None)(
    implicit hc: HeaderCarrier): Future[Option[PbikSession]] = {
    def selectKeysToCache(session: PbikSession): PbikSession =
      key match {
        case CacheKeys.RegistrationList => session.copy(registrations = Some(value.get.asInstanceOf[RegistrationList]))
        case CacheKeys.BikRemoved       => session.copy(bikRemoved = Some(value.get.asInstanceOf[RegistrationItem]))
        case CacheKeys.ListOfMatches    => session.copy(listOfMatches = Some(value.get.asInstanceOf[List[EiLPerson]]))
        case CacheKeys.EiLPerson        => session.copy(eiLPerson = Some(value.get.asInstanceOf[EiLPerson]))
        case CacheKeys.CurrentExclusions =>
          session.copy(currentExclusions = Some(value.get.asInstanceOf[List[EiLPerson]]))
        case CacheKeys.CYRegisteredBiks => session.copy(cyRegisteredBiks = Some(value.get.asInstanceOf[List[Bik]]))
        case CacheKeys.NYRegisteredBiks => session.copy(nyRegisteredBiks = Some(value.get.asInstanceOf[List[Bik]]))
        case _ =>
          logger.warn(s"[SessionService][cache] No matching keys found - returning clean session")
          cleanSession
      }
    for {
      currentSession <- fetchPbikSession
      session = currentSession.getOrElse(cleanSession)
      cacheMap <- sessionCache.cache[PbikSession](PBIK_SESSION_KEY, selectKeysToCache(session))

    } yield {
      cacheMap.getEntry[PbikSession](PBIK_SESSION_KEY)
    }
  }

}

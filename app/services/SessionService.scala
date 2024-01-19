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

import models._
import models.cache.MissingSessionIdException
import play.api.Logging
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionService @Inject() (val sessionRepository: SessionRepository)(implicit
  ec: ExecutionContext
) extends Logging {

  private object CacheKeys extends Enumeration {
    val RegistrationList, BikRemoved, ListOfMatches, EiLPerson, CurrentExclusions, CYRegisteredBiks, NYRegisteredBiks =
      Value
  }

  //TODO investigate if we can remove all this default object creation
  private val cleanRegistrationList: Option[RegistrationList] = Some(
    RegistrationList(None, List.empty[RegistrationItem], None)
  )
  private val cleanBikRemoved: Option[RegistrationItem]       = Some(RegistrationItem("", active = false, enabled = false))
  private val cleanEiLPersonList: Option[List[EiLPerson]]     = Some(List.empty[EiLPerson])
  private val cleanEiLPerson: Option[EiLPerson]               = Some(EiLPerson("", "", None, "", None, None, None, None))
  private val cleanBikList: Option[List[Bik]]                 = Some(List.empty[Bik])
  private def cleanSession(sessionId: String): PbikSession    =
    PbikSession(
      sessionId,
      cleanRegistrationList,
      cleanBikRemoved,
      cleanEiLPersonList,
      cleanEiLPerson,
      cleanEiLPersonList,
      cleanBikList,
      cleanBikList
    )

  private def getSessionFromHeaderCarrier(hc: HeaderCarrier): Either[Exception, String] =
    hc.sessionId match {
      case Some(sessionId) =>
        Right(sessionId.value)
      case _               =>
        logger.warn("[SessionService][getSessionFromHeaderCarrier] No session Id present at header carrier")
        Left(new MissingSessionIdException("Unable to retrieve session ID"))
    }

  def fetchPbikSession()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    getSessionFromHeaderCarrier(hc) match {
      case Left(_)          => Future.successful(None)
      case Right(sessionId) => sessionRepository.get(sessionId)
    }

  def storeRegistrationList(value: RegistrationList)(implicit hc: HeaderCarrier): Future[PbikSession] =
    storeSession(CacheKeys.RegistrationList, value)

  def storeBikRemoved(value: RegistrationItem)(implicit hc: HeaderCarrier): Future[PbikSession] =
    storeSession(CacheKeys.BikRemoved, value)

  def storeListOfMatches(value: List[EiLPerson])(implicit hc: HeaderCarrier): Future[PbikSession] =
    storeSession(CacheKeys.ListOfMatches, value)

  def storeEiLPerson(value: EiLPerson)(implicit hc: HeaderCarrier): Future[PbikSession] =
    storeSession(CacheKeys.EiLPerson, value)

  def storeCurrentExclusions(value: List[EiLPerson])(implicit hc: HeaderCarrier): Future[PbikSession] =
    storeSession(CacheKeys.CurrentExclusions, value)

  def storeCYRegisteredBiks(value: List[Bik])(implicit hc: HeaderCarrier): Future[PbikSession] =
    storeSession(CacheKeys.CYRegisteredBiks, value)

  def storeNYRegisteredBiks(value: List[Bik])(implicit hc: HeaderCarrier): Future[PbikSession] =
    storeSession(CacheKeys.NYRegisteredBiks, value)

  def resetAll()(implicit hc: HeaderCarrier): Future[Boolean] =
    getSessionFromHeaderCarrier(hc) match {
      case Left(_)   =>
        logger.info("[SessionService][resetAll] No session to reset")
        Future.successful(false)
      case Right(id) => sessionRepository.remove(id)
    }

  private def storeSession[T](key: CacheKeys.Value, value: T)(implicit
    hc: HeaderCarrier
  ): Future[PbikSession] = {
    def selectKeysToCache(session: PbikSession): PbikSession =
      key match {
        case CacheKeys.RegistrationList  => session.copy(registrations = Some(value.asInstanceOf[RegistrationList]))
        case CacheKeys.BikRemoved        => session.copy(bikRemoved = Some(value.asInstanceOf[RegistrationItem]))
        case CacheKeys.ListOfMatches     => session.copy(listOfMatches = Some(value.asInstanceOf[List[EiLPerson]]))
        case CacheKeys.EiLPerson         => session.copy(eiLPerson = Some(value.asInstanceOf[EiLPerson]))
        case CacheKeys.CurrentExclusions =>
          session.copy(currentExclusions = Some(value.asInstanceOf[List[EiLPerson]]))
        case CacheKeys.CYRegisteredBiks  => session.copy(cyRegisteredBiks = Some(value.asInstanceOf[List[Bik]]))
        case CacheKeys.NYRegisteredBiks  => session.copy(nyRegisteredBiks = Some(value.asInstanceOf[List[Bik]]))
        case _                           =>
          logger.warn(s"[SessionService][storeSession] No matching keys found - returning current session")
          session
      }

    val sessionId = getSessionFromHeaderCarrier(hc) match {
      case Left(e)          => throw e
      case Right(sessionId) => sessionId
    }

    for {
      currentSession <- fetchPbikSession()
      session         = currentSession.getOrElse(cleanSession(sessionId))
      updatedSession <- sessionRepository.upsert(selectKeysToCache(session))
    } yield updatedSession
  }

}

package models

case class EiLPerson(nino: String, firstForename: String, secondForename: Option[String], surname: String, worksPayrollNumber: Option[String],
                     dateOfBirth: Option[String], gender: Option[String], status: Option[Int], perOptLock: Int = 0) {

  override def equals(obj: Any):Boolean = obj match {
    case EiLPerson(nino,_,_,_,_,_,_,_,_) => this.nino == nino
    case _                               => false
  }

  override def hashCode:Int = nino.hashCode
}

object EiLPerson {

  val defaultStringArgumentValue = ""
  val defaultIntArgumentValue = -1
  val defaultNino = defaultStringArgumentValue
  val defaultFirstName = defaultStringArgumentValue
  val defaultSecondName = Some(defaultStringArgumentValue)
  val defaultSurname = defaultStringArgumentValue
  val defaultWorksPayrollNumber = Some(defaultStringArgumentValue)
  val defaultDateOfBirth = None
  val defaultGender = Some(defaultStringArgumentValue)
  val defaultStatus = Some(defaultIntArgumentValue)
  val defaultPerOptLock = defaultIntArgumentValue

  def secondaryComparison(x: EiLPerson, y: EiLPerson): Boolean = {
    x.firstForename == y.firstForename &&
      x.surname == y.surname &&
      x.dateOfBirth.getOrElse("") == y.dateOfBirth.getOrElse("") &&
      x.gender.getOrElse("") == y.gender.getOrElse("")
  }

  def defaultEiLPerson(): EiLPerson = {
    EiLPerson(defaultNino, defaultFirstName, defaultSecondName, defaultSurname, defaultWorksPayrollNumber, defaultDateOfBirth, defaultGender, defaultStatus, defaultPerOptLock )
  }
}

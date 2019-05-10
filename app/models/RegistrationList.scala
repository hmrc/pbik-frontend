package models
import models.BinaryRadioButtonWithDesc

case class RegistrationList(selectAll: Option[String] = None, active: List[RegistrationItem], reason: Option[BinaryRadioButtonWithDesc] = None)
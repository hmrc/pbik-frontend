package models

import play.api.libs.json.{Json, OFormat}

case class Bik(iabdType: String, status: Int, eilCount: Int = 0) {
  override def equals(obj: Any):Boolean = obj match {
    case Bik(iabdType,_,_) => this.iabdType == iabdType
    case _                => false
  }

  override def hashCode:Int = iabdType.hashCode
}

object Bik {

  implicit val bikFormats: OFormat[Bik] = Json.format[Bik]
}

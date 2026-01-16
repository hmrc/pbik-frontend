/*
 * Copyright 2026 HM Revenue & Customs
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

package views.templatemodels

object CheckboxId {
  def first(formData: Map[String, String]): String = {
    val idsFromForm   = formData.keys.map(extractIdFromKey)
    val otherCategory = formData.filter(_._2 == "47")

    if (idsFromForm.isEmpty) {
      ""
    } else if (idsFromForm.size > 1 && otherCategory.nonEmpty) {
      val otherCategoryKey = extractIdFromKey(otherCategory.head._1)
      val id               = formData(firstFieldString(idsFromForm, otherCategoryKey))
      s"checkbox-$id"
    } else {
      val id = formData(firstFieldString(idsFromForm))
      s"checkbox-$id"
    }
  }

  private def extractIdFromKey(key: String): Int = {
    val str = key.slice(8, key.length - 5)
    if (str.isEmpty) { -1 }
    else { str.toInt }
  }

  private def firstFieldString(keys: Iterable[Int]): String =
    s"actives[${keys.filter(_ >= 0).min}].uid"

  private def firstFieldString(keys: Iterable[Int], ignoreKey: Int): String =
    s"actives[${keys.filter(key => key >= 0 && key != ignoreKey).min}].uid"

}

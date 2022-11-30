/*
 * Copyright 2022 HM Revenue & Customs
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

package utils

import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

object DateUtils {

  def npsDateConversionFormat(dateAsString: String): String = {
    val sourceFormat: DateFormat = new SimpleDateFormat("dd/MM/yyyy")
    val date: Date               = sourceFormat.parse(dateAsString)

    val outputFormat: String               = "d MMMMM yyyy"
    val simpleDateFormat: SimpleDateFormat = new SimpleDateFormat(outputFormat)
    simpleDateFormat.format(date)
  }

}

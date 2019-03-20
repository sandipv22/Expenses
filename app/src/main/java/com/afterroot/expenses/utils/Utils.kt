/*
 * Copyright 2018-2019 Sandip Vaghela
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.afterroot.expenses.utils

import android.content.Context
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.*


/**
 * Created by Sandip on 04-12-2017.
 */
object Utils {
    fun getDateDiff(fromDate: Date, toDate: Date = Calendar.getInstance().time): String {
        val fromCal = Calendar.getInstance()
        fromCal.time = fromDate

        val toCal = Calendar.getInstance()
        toCal.time = toDate

        val fromMS = fromCal.timeInMillis
        val toMS = toCal.timeInMillis
        val diff: Long = toMS - fromMS

        return if (diff < 0) {
            "Just now"
        } else {
            val diffInDays = (diff / (1000 * 60 * 60 * 24)).toInt()
            val diffInHours = (diff / (1000 * 60 * 60) % 24).toInt()
            val diffInMinutes = (diff % (1000 * 60 * 60) / (1000 * 60)).toInt()
            val diffInSec = (diff / 1000 % 60).toInt()

            val builder = StringBuilder()
            when {
                diffInDays > 1 -> builder.append("$diffInDays days ago")
                diffInDays == 1 -> builder.append("$diffInDays day ago")
                diffInHours > 1 -> builder.append("$diffInHours hours ago")
                diffInHours == 1 -> builder.append("$diffInHours hour ago")
                diffInMinutes > 1 -> builder.append("$diffInMinutes minutes ago")
                diffInMinutes == 1 -> builder.append("$diffInMinutes minute ago")
                diffInSec > 1 -> builder.append("$diffInSec seconds ago")
                else -> builder.append("$diffInSec second ago")
            }
            builder.toString()
        }
    }

    fun formatNames(map: HashMap<String, String>): String {
        val builder = StringBuilder()
        var i = 0
        map.forEach {
            i++
            when (i) {
                map.size -> builder.append(it.value)
                map.size - 1 -> builder.append(it.value + " and ")
                else -> builder.append(it.value + ", ")
            }
        }
        return builder.toString()
    }

    fun formatPhone(context: Context, phone: String): String {
        val phoneUtil = PhoneNumberUtil.createInstance(context)
        val number = phoneUtil.parse(phone, "IN")
        val test = number.nationalNumber.toString()
        return test.replace("[\\D]", "")
    }
}


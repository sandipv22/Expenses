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

package com.afterroot.expenses.model

import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*

data class Group(
        var group_name: String,
        @ServerTimestamp var date: Date?,
        var members: HashMap<String?, Int>?,
        @ServerTimestamp var lastEntry: Date? = Date(),
        var lastEntryText: String?
) : Serializable, Expense {
    override fun getType(): Int {
        return Expense.TYPE_GROUP
    }

    constructor() : this("", null, null, null, "")
}
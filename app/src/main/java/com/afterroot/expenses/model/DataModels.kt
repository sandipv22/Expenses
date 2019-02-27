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

/*data class ExpenseItem(var amount: Long, var category: String, @ServerTimestamp var date: Date?, var note: String, var paidBy: String, var with: List<String>?) : Serializable {
    constructor() : this(0, "", null, "", "", null)
}*/

data class ExpenseItem(
        var amount: Long,
        var category: String,
        @ServerTimestamp var date: Date?,
        var note: String,
        var paidBy: String,
        var with: HashMap<String, String>?
) : Serializable, Expense {
    override fun getType(): Int {
        return Expense.TYPE_EXPENSE
    }

    constructor() : this(0, "", null, "", "", null)
}

data class Group(
        var group_name: String,
        @ServerTimestamp var date: Date?,
        var members: HashMap<String?, Int>?
) : Serializable, Expense {
    override fun getType(): Int {
        return Expense.TYPE_GROUP
    }

    constructor() : this("", null, null)
}

data class User(var name: String, var email: String, var uid: String, var phone: String?) : Serializable {
    constructor() : this("", "", "", "")
}

data class Category(var name: String) : Serializable {
    constructor() : this("")
}

interface Expense {

    fun getType(): Int

    companion object {
        const val TYPE_GROUP = 1
        const val TYPE_EXPENSE = 2
    }
}

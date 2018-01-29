package com.afterroot.expenses.model

import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*

data class ExpenseItem(var amount: Long, var category: String, @ServerTimestamp var date: Date?, var note: String, var paidBy: String, var with: List<String>?) : Serializable {
    constructor() : this(0, "", null, "", "", null)
}

data class Group(var group_name: String, @ServerTimestamp var date: Date?, var members: HashMap<String?, Int>?) : Serializable {
    constructor() : this("", null, null)
}

data class User(var name: String, var email: String, var uid: String, var phone: String) : Serializable {
    constructor() : this("", "", "", "")
}

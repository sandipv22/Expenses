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

package com.afterroot.expenses.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.afterroot.expenses.database.DBConstants
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.model.ExpenseItem
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot


class ExpensesViewModel : ViewModel() {
    var snapshot: MutableLiveData<QuerySnapshot> = MutableLiveData()
    var expenses: MutableLiveData<List<ExpenseItem>> = MutableLiveData()

    fun getSnapshot(groupId: String): LiveData<QuerySnapshot> {
        if (snapshot.value == null) {
            Log.d("ExpensesViewModel", "getGroupSnapshot: ")
            Database.getInstance()
                    .collection(DBConstants.GROUPS)
                    .document(groupId)
                    .collection(DBConstants.EXPENSES)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener { querySnapshot, _ ->
                        if (querySnapshot != null) {
                            snapshot.value = querySnapshot
                        }
                    }
        }
        return snapshot
    }

    fun getExpenses(groupId: String): LiveData<List<ExpenseItem>> {
        if (expenses.value == null) {
            expenses.value = getSnapshot(groupId).value?.toObjects(ExpenseItem::class.java)
        }
        return expenses
    }
}

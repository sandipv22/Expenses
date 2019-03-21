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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.afterroot.expenses.database.DBConstants
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.model.ExpenseItem
import com.google.firebase.firestore.Query


class ExpensesViewModel : ViewModel() {
    var snapshot: MutableLiveData<ViewModelState> = MutableLiveData()
    var expenses: MutableLiveData<List<ExpenseItem>> = MutableLiveData()

    fun getSnapshot(groupId: String): LiveData<ViewModelState> {
        if (snapshot.value == null) {
            snapshot.postValue(ViewModelState.Loading)
            Database.getInstance()
                    .collection(DBConstants.GROUPS)
                    .document(groupId)
                    .collection(DBConstants.EXPENSES)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener { querySnapshot, _ ->
                        if (querySnapshot != null) {
                            snapshot.postValue(ViewModelState.Loaded(querySnapshot))
                        }
                    }
        }
        return snapshot
    }
}

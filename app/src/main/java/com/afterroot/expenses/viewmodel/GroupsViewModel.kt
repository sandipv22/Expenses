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

class GroupsViewModel : ViewModel() {
    var groupSnapshot: MutableLiveData<ViewModelState> = MutableLiveData()

    fun getGroupSnapshot(userId: String): LiveData<ViewModelState> {
        if (groupSnapshot.value == null) {
            groupSnapshot.postValue(ViewModelState.Loading)
            Database.getInstance().collection(DBConstants.GROUPS).whereGreaterThanOrEqualTo(
                    "${DBConstants.FIELD_GROUP_MEMBERS}.$userId",
                    DBConstants.TYPE_MEMBER
            ).addSnapshotListener { querySnapshot, _ ->
                if (querySnapshot != null) {
                    groupSnapshot.postValue(ViewModelState.Loaded(querySnapshot))
                }
            }
        }
        return groupSnapshot
    }
}
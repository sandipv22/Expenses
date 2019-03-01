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

import android.view.MenuItem
import com.google.firebase.firestore.DocumentSnapshot

interface ListClickCallbacks<in T> {
    fun onListItemClick(item: T?, docId: String, position: Int)
    fun onListItemLongClick(item: T?, docId: String, position: Int)
}

interface NavigationItemClickCallback {
    fun onClick(item: MenuItem)
}

interface Callbacks<in T> {
    fun onSuccess(value: T)
    fun onFailed(message: String)
    fun onSnapshot(snapshot: DocumentSnapshot)
}

interface DeleteListener {
    fun onDeleteSuccess()
    fun onDeleteFailed()
}
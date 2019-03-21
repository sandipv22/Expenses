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

package com.afterroot.expenses.database

import com.afterroot.expenses.firebase.DeleteListener
import com.afterroot.expenses.firebase.QueryCallback
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.model.User
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.util.*

object Database {

    fun getInstance() = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
    }

    fun getUserByID(uid: String, queryCallback: QueryCallback<User>) {
        getInstance().collection(DBConstants.USERS).document(uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        queryCallback.onSuccess(documentSnapshot.toObject(User::class.java)!!)
                    } else {
                        queryCallback.onFailed("User Not Exists")
                    }
                }.addOnFailureListener { exception ->
                    queryCallback.onFailed(exception.message!!)
                }
    }

    inline fun <reified T> getByID(ref: DocumentReference, queryCallback: QueryCallback<T>) {
        ref.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                queryCallback.onSuccess(documentSnapshot.toObject(T::class.java)!!)
                queryCallback.onSnapshot(documentSnapshot)
            } else {
                queryCallback.onFailed("User Not Exists")
            }
        }.addOnFailureListener { exception ->
            queryCallback.onFailed(exception.message!!)
        }
    }

    fun getGroupMembers(groupId: String, queryCallback: QueryCallback<HashMap<String, User>>) {
        getInstance().collection(DBConstants.GROUPS).document(groupId).get().addOnSuccessListener { groupSnapshot ->
            val group = groupSnapshot.toObject(Group::class.java)
            val user: HashMap<String, User>? = null
            var i = group!!.members!!.size
            group.members?.forEach {
                getInstance().collection(DBConstants.USERS).document(it.key!!).get().addOnSuccessListener { userSnapshot ->
                    i--
                    user!!.put(it.key!!, userSnapshot.toObject(User::class.java)!!)
                }
                if (i == 0) {
                    queryCallback.onSuccess(user!!)
                }
            }
        }
    }

    fun delete(ref: DocumentReference, callbacks: DeleteListener) {
        ref.delete().addOnSuccessListener {
            callbacks.onDeleteSuccess()
        }.addOnFailureListener { callbacks.onDeleteFailed() }
    }
}
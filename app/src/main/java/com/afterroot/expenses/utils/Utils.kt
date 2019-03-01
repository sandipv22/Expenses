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

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.afterroot.expenses.BuildConfig
import com.afterroot.expenses.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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

fun Activity.getDrawableExt(id: Int, tint: Int? = null): Drawable {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val drawable = resources.getDrawable(id, theme)
        if (tint != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                drawable.setTint(resources.getColor(tint, theme))
            } else {
                drawable.setTint(resources.getColor(tint))
            }
        }
        return drawable
    }
    return resources.getDrawable(id)
}

fun View.visible(value: Boolean) {
    visibility = if (value) {
        View.VISIBLE
    } else {
        View.INVISIBLE
    }
}

class PermissionChecker(private val mContext: Context) {

    fun isPermissionsNeeded(vararg permissions: String): Boolean {
        return permissions.any { checkPermission(it) }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_DENIED
    }
}

object Constants {
    const val RC_SIGN_IN = 479
    const val RC_PERMISSIONS = 8649
    const val RC_CONTACTS_PICKER = 461
    const val PREF_KEY_FIRST_START = "${BuildConfig.APPLICATION_ID}.PREF_KEY_FIRST_START"
    const val KEY_EXPENSE_SERIALIZE = "EXPENSE_ITEM_SERIALIZE"
}

object DBConstants {
    const val USERS = "users"
    const val EXPENSES = "expenses"
    const val GROUPS = "groups"
    const val CATEGORIES = "categories"

    const val FIELD_NAME = "name"
    const val FIELD_EMAIL = "email"
    const val FIELD_UID = "uid"
    const val FIELD_PHONE = "phone"

    const val FIELD_ADMIN_ID = "admin_id"
    const val FIELD_GROUP_NAME = "group_name"
    const val FIELD_GROUP_MEMBERS = "members"

    const val TYPE_MEMBER = 0
    const val TYPE_ADMIN = 1
}

object FirebaseUtils {
    var auth: FirebaseAuth? = FirebaseAuth.getInstance()
        get() {
            Log.d("FirebaseUtils", "FirebaseUtils.auth: initializing Auth")
            return field ?: FirebaseAuth.getInstance()
        }

    val firebaseUser: FirebaseUser? = auth!!.currentUser
        get() {
            Log.d("FirebaseUtils", "FirebaseUtils.getFirebaseUser: getting firebaseUser")
            return field ?: auth!!.currentUser
        }

    val isUserSignedIn: Boolean
        get() {
            if (firebaseUser == null) {
                return false
            }
            return true
        }
}

object Database {

    fun getInstance() = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
    }

    fun getUserByID(uid: String, callbacks: Callbacks<User>) {
        getInstance().collection(DBConstants.USERS).document(uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        Log.d("FirebaseUser", "DocumentSnapshot data: " + documentSnapshot.data)
                        callbacks.onSuccess(documentSnapshot.toObject(User::class.java)!!)
                    } else {
                        callbacks.onFailed("User Not Exists")
                    }
                }.addOnFailureListener { exception ->
                    Log.d("FirebaseUser", "getUserByID: Error :${exception.message}")
                    callbacks.onFailed(exception.message!!)
                }
    }

    inline fun <reified T> getByID(ref: DocumentReference, callbacks: Callbacks<T>) {
        ref.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                Log.d("FirebaseUser", "DocumentSnapshot data: " + documentSnapshot.data)
                callbacks.onSuccess(documentSnapshot.toObject(T::class.java)!!)
                callbacks.onSnapshot(documentSnapshot)
            } else {
                callbacks.onFailed("User Not Exists")
            }
        }.addOnFailureListener { exception ->
            Log.d("FirebaseUser", "getUserByID: Error :${exception.message}")
            callbacks.onFailed(exception.message!!)
        }
    }

    fun delete(ref: DocumentReference, callbacks: DeleteListener) {
        ref.delete().addOnSuccessListener {
            callbacks.onDeleteSuccess()
        }.addOnFailureListener { callbacks.onDeleteFailed() }
    }
}
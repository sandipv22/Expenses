package com.afterroot.expenses.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.afterroot.expenses.BuildConfig
import com.afterroot.expenses.model.User
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.*


/**
 * Created by Sandip on 04-12-2017.
 */
object Utils {
    private var providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build())
    /*     AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
         AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build())*/

    fun startFirebaseUI(activity: Activity, requestCode: Int) {
        activity.startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        .build(), requestCode)
    }

    fun getDateDiff(fromDate: Date, toDate: Date = Calendar.getInstance().time): String {
        val fromCal = Calendar.getInstance()
        fromCal.time = fromDate

        val toCal = Calendar.getInstance()
        toCal.time = toDate

        val fromMS = fromCal.timeInMillis
        val toMS = toCal.timeInMillis
        val diff: Long = toMS - fromMS

        Log.d("Expenses", "getDateDiff: fromMillis $fromMS, toMillis $toMS, diff $diff")

        return if (diff < 0) {
            "Just now"
        } else {
            val diffInDays = (diff / (1000 * 60 * 60 * 24)).toInt()
            val diffInHours = (diff / (1000 * 60 * 60) % 24).toInt()
            val diffInMinutes = (diff % (1000 * 60 * 60) / (1000 * 60)).toInt()
            val diffInSec = (diff / 1000 % 60).toInt()

            val builder = StringBuilder()
            when {
                diffInDays > 0 -> builder.append("$diffInDays days ago")
                diffInHours > 0 -> builder.append("$diffInHours hours ago")
                diffInMinutes > 0 -> builder.append("$diffInMinutes minutes ago")
                else -> builder.append("$diffInSec seconds ago")
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

class PermissionChecker(private val mContext: Context) {

    fun isPermissionsNeeded(vararg permissions: String): Boolean {
        return permissions.any { checkPermission(it) }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_DENIED
    }
}

object Constants {
    val RC_SIGN_IN = 479
    val RC_PERMISSIONS = 8649
    val RC_CONTACTS_PICKER = 461
    val PREF_KEY_FIRST_START = "${BuildConfig.APPLICATION_ID}.PREF_KEY_FIRST_START"
    val KEY_EXPENSE_SERIALIZE = "EXPENSE_ITEM_SERIALIZE"
}

object DBConstants {
    val USERS = "users"
    val EXPENSES = "expenses"
    val GROUPS = "groups"
    val CATEGORIES = "categories"

    val FIELD_NAME = "name"
    val FIELD_EMAIL = "email"
    val FIELD_UID = "uid"
    val FIELD_PHONE = "phone"

    val FIELD_ADMIN_ID = "admin_id"
    val FIELD_GROUP_NAME = "group_name"
    val FIELD_GROUP_MEMBERS = "members"

    val TYPE_MEMBER = 0
    val TYPE_ADMIN = 1
}

object FirebaseUtils {
    var auth: FirebaseAuth? = null
        get() {
            Log.d("FirebaseUtils", "FirebaseUtils.auth: initializing Auth")
            return field ?: FirebaseAuth.getInstance()
        }

    val firebaseUser: FirebaseUser? = null
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
    val EMAIL = auth?.currentUser?.email
    val UID = auth?.currentUser?.uid
    val NAME = auth?.currentUser?.displayName
    val PHONE = auth?.currentUser?.phoneNumber

    interface Callbacks<in T> {
        fun onSuccess(value: T)
        fun onFailed(message: String)
    }

    fun getUserByID(uid: String, callbacks: Callbacks<User>) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection(DBConstants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid)
        userRef.get().addOnSuccessListener { documentSnapshot ->
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

    inline fun <reified T> getByID(ref: DocumentReference, callbacks: FirebaseUtils.Callbacks<T>) {
        ref.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                Log.d("FirebaseUser", "DocumentSnapshot data: " + documentSnapshot.data)
                callbacks.onSuccess(documentSnapshot.toObject(T::class.java)!!)
            } else {
                callbacks.onFailed("User Not Exists")
            }
        }.addOnFailureListener { exception ->
            Log.d("FirebaseUser", "getUserByID: Error :${exception.message}")
            callbacks.onFailed(exception.message!!)
        }
    }

}
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

package com.afterroot.expenses.ui

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.MenuItem
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.R
import com.afterroot.expenses.fragment.BottomNavigationDrawerFragment
import com.afterroot.expenses.model.User
import com.afterroot.expenses.utils.*
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import org.jetbrains.anko.design.snackbar


class HomeActivity : AppCompatActivity() {

    private val _tag = "HomeActivity"
    private val homeFragmentId = R.id.groupsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(bottom_appbar)

        checkPermissions(permissions)
    }

    override fun onResume() {
        super.onResume()
        checkPermissions(permissions)
    }

    private val handler = Handler()
    lateinit var mFab: FloatingActionButton
    private fun setUpNavigation() {
        val drawerArrowDrawable = DrawerArrowDrawable(this)
        bottom_appbar.navigationIcon = drawerArrowDrawable
        mFab = fab

        handler.postDelayed({
            host_nav_fragment.findNavController().addOnDestinationChangedListener { controller, destination, _ ->
                Log.d(_tag, "onDestinationChange: ${destination.label}")
                action_title.text = destination.label
                with(mFab) {
                    hide()
                    setImageDrawable(getDrawableExt(R.drawable.ic_add, R.color.icon_fill))
                }
                when (destination.id) {
                    R.id.groupsFragment -> {
                        bottom_appbar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
                        mFab.show()
                    }
                    R.id.expenseListFragment -> {
                        bottom_appbar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
                        mFab.show()
                    }
                    R.id.newGroupFragment -> {
                        handler.postDelayed({
                            with(mFab) {
                                show()
                                setImageDrawable(getDrawableExt(R.drawable.ic_save, R.color.icon_fill))
                            }
                        }, 150)
                    }
                    R.id.addExpenseFragment -> {
                        with(mFab) {
                            setImageDrawable(getDrawableExt(R.drawable.ic_done, R.color.icon_fill))
                            show()
                        }
                    }
                    R.id.expenseDetailFragment -> {
                    }
                    R.id.editProfileFragment -> {
                        handler.postDelayed({
                            with(mFab) {
                                show()
                                setImageDrawable(getDrawableExt(R.drawable.ic_save, R.color.icon_fill))
                            }
                        }, 150)
                    }
                }
                val anim: ValueAnimator = when {
                    destination.id != homeFragmentId -> {
                        bottom_appbar.setNavigationOnClickListener { controller.navigateUp() }
                        ValueAnimator.ofFloat(0F, 1F) //Set as Arrow
                    }
                    else -> {
                        setUpBottomNavDrawer()
                        ValueAnimator.ofFloat(1F, 0F) //Set as Hamburger
                    }
                }
                handler.postDelayed({
                    anim.apply {
                        addUpdateListener {
                            drawerArrowDrawable.progress = it.animatedValue as Float
                        }
                        interpolator = DecelerateInterpolator()
                        duration = 200
                        start()
                    }
                }, 100)
            }
        }, 100)

    }

    private fun setUpBottomNavDrawer() {
        bottom_appbar.setNavigationOnClickListener {
            val fragment = BottomNavigationDrawerFragment.with(object : NavigationItemClickCallback {
                override fun onClick(item: MenuItem) {
                    when (item.itemId) {
                        R.id.action_settings -> {
                            Toast.makeText(this@HomeActivity, "Clicked", Toast.LENGTH_SHORT).show()
                        }
                        R.id.sign_out -> {
                            signOutDialog().show()
                        }
                        R.id.action_edit_profile -> {
                            host_nav_fragment.findNavController().navigate(R.id.toEditProfile)
                        }
                    }
                }

            })
            fragment.show(this.supportFragmentManager, fragment.tag)
        }
    }

    private val permissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun checkPermissions(permissions: Array<out String>) {
        if (PermissionChecker(this).isPermissionsNeeded(*permissions)) {
            Log.d(_tag, "checkPermissions: Requesting Permissions")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, Constants.RC_PERMISSIONS)
            }
        } else {
            Log.d(_tag, "checkPermissions: Permissions Granted. Now initializing")
            if (FirebaseUtils.isUserSignedIn) {
                addUserInfoInDB()
                setUpNavigation()
                setUpBottomNavDrawer()
            }
        }
    }

    override fun onSupportNavigateUp() = host_nav_fragment.findNavController().navigateUp()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.RC_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && !grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                    Log.d(_tag, "onRequestPermissionsResult: Permission Granted")
                    Handler().postDelayed({
                        when {
                            PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_KEY_FIRST_START, true)
                                    or !FirebaseUtils.isUserSignedIn -> signInDialog().show()
                            else -> {
                                addUserInfoInDB()
                                setUpNavigation()
                                setUpBottomNavDrawer()
                            }
                        }
                    }, 1000)
                } else {
                    Log.d(_tag, "onRequestPermissionsResult: Permissions not granted")
                    root_layout.snackbar(getString(R.string.msg_grant_permission_request), getString(R.string.text_action_grant)) {
                        checkPermissions(permissions)
                    }
                    //TODO MainInfoFragment
                }
            }
        }
    }

    private fun signInDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(this)
                .setTitle(getString(R.string.text_sign_in))
                .setMessage(getString(R.string.msg_dialog_sign_in))
                .setPositiveButton(R.string.text_sign_in) { _, _ ->
                    startActivityForResult(AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(true)
                            .setTheme(R.style.AppTheme)
                            .setAvailableProviders(arrayListOf(AuthUI.IdpConfig.PhoneBuilder().build(),
                                    AuthUI.IdpConfig.GoogleBuilder().build()))
                            .build(), Constants.RC_SIGN_IN)
                }
                .setNegativeButton(getString(R.string.text_cancel)) { _, _ ->
                    finish()
                }.setCancelable(false)
    }

    private fun signOutDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(this)
                .setTitle(getString(R.string.text_sign_out))
                .setMessage(getString(R.string.msg_dialog_sign_out))
                .setPositiveButton(R.string.text_sign_out) { _, _ ->
                    AuthUI.getInstance().signOut(this).addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.text_sign_out_success), Toast.LENGTH_SHORT).show()
                        signInDialog().show()
                    }
                }
                .setNegativeButton(R.string.text_cancel) { _, _ ->

                }.setCancelable(true)
    }

    private fun addUserInfoInDB() {
        val auth = FirebaseAuth.getInstance()
        val curUser = auth.currentUser
        /*if (curUser?.displayName == null || curUser.email == null || curUser.phoneNumber == null) {
            createdView.findNavController().navigate(R.id.edit_profile)
            return
        }*/
        val userRef = FirebaseFirestore.getInstance().collection(DBConstants.USERS).document(curUser!!.uid)
        Log.d(_tag, "addUserInfoInDB: Started")
        userRef.get().addOnCompleteListener { getUserTask ->
            when {
                getUserTask.isSuccessful -> if (!getUserTask.result!!.exists()) {
                    val dialog = MaterialDialog.Builder(this)
                            .progress(true, 1)
                            .content("Creating User...")
                            .show()
                    Log.d(_tag, "User not available. Creating User..")
                    val phone = curUser.phoneNumber
                    val user = User(curUser.displayName!!,
                            curUser.email!!,
                            curUser.uid,
                            phone)
                    //TODO add dialog to add phone number
                    userRef.set(user).addOnCompleteListener { setUserTask ->
                        when {
                            setUserTask.isSuccessful -> {
                                Log.d(_tag, "User Created")
                                Log.d(_tag, "DocumentSnapshot data: " + setUserTask.result)
                            }
                            else -> Log.e(_tag, "Can't create firebaseUser", setUserTask.exception)
                        }
                        dialog.dismiss()
                    }
                }
                else -> Log.e(_tag, "Unknown Error", getUserTask.exception)
            }
            //initFirebaseDb()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.RC_SIGN_IN -> {
                Log.d(_tag, "onActivityResult: Request Code Received")
                val response = IdpResponse.fromResultIntent(data)
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d(_tag, "onActivityResult: Result was ok")
                        val user = FirebaseAuth.getInstance().currentUser
                        when {
                            user != null -> {
                                PreferenceManager.getDefaultSharedPreferences(this).edit()
                                        .putBoolean(Constants.PREF_KEY_FIRST_START, false)
                                        .apply()
                                checkPermissions(permissions)
                                Log.d(_tag, "onActivityResult: Everything is Ok. Navigating to Groups")
                            }
                        }
                    }
                    else -> {
                        when {
                            response == null -> {
                                // User pressed back button
                                Toast.makeText(this, "Sign In Cancelled", Toast.LENGTH_SHORT).show()
                            }
                            response.error!!.errorCode == ErrorCodes.NO_NETWORK -> {
                                Toast.makeText(this, "No network", Toast.LENGTH_SHORT).show()
                            }
                            response.error!!.errorCode == ErrorCodes.UNKNOWN_ERROR -> {
                                Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show()
                            }
                            else -> Toast.makeText(this, "Sign In failed. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }


}

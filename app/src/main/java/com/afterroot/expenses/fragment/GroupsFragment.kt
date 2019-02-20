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

package com.afterroot.expenses.fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.R
import com.afterroot.expenses.model.GroupAdapter
import com.afterroot.expenses.model.GroupsViewModel
import com.afterroot.expenses.model.User
import com.afterroot.expenses.utils.*
import com.afterroot.expenses.utils.Constants.PREF_KEY_FIRST_START
import com.afterroot.expenses.utils.Constants.RC_SIGN_IN
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.fragment_groups.*
import org.jetbrains.anko.design.snackbar

class GroupsFragment : Fragment() {
    private var groupsAdapter: GroupAdapter? = null
    lateinit var db: FirebaseFirestore
    private val _tag = "GroupsFragment"
    private lateinit var _context: Context
    private lateinit var createdView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity!!.progress.visibility = View.VISIBLE
        createdView = view
        _context = createdView.context
        val dbSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        db = FirebaseFirestore.getInstance().apply { firestoreSettings = dbSettings }
        when {
            PreferenceManager.getDefaultSharedPreferences(view.context).getBoolean(Constants.PREF_KEY_FIRST_START, true)
                    or !FirebaseUtils.isUserSignedIn -> signInDialog().show()
            else -> checkPermissions(permissions)
        }

        activity!!.apply {
            fab.apply {
                setImageDrawable(getDrawableExt(R.drawable.ic_add))
                setOnClickListener {
                    view.findNavController().navigate(R.id.newGroupFragment)
                }
            }

            bottom_appbar.setNavigationOnClickListener {
                val fragment = BottomNavigationDrawerFragment.with(object : NavigationItemClickCallback {
                    override fun onClick(item: MenuItem) {
                        when (item.itemId) {
                            R.id.action_settings -> {
                                Toast.makeText(_context, "Clicked", Toast.LENGTH_SHORT).show()
                            }
                            R.id.sign_out -> {
                                AuthUI.getInstance().signOut(_context).addOnSuccessListener {
                                    Toast.makeText(_context, "Signed Out", Toast.LENGTH_SHORT).show()
                                    signInDialog().show()
                                }
                            }
                            R.id.edit_profile -> {
                                findNavController().navigate(R.id.edit_profile)
                            }
                        }

                    }

                })
                fragment.show(supportFragmentManager, fragment.tag)
            }
        }
    }

    private val permissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun checkPermissions(permissions: Array<out String>) {
        if (PermissionChecker(_context).isPermissionsNeeded(*permissions)) {
            Log.d(_tag, "checkPermissions: Requesting Permissions")
            requestPermissions(permissions, Constants.RC_PERMISSIONS)
        } else {
            Log.d(_tag, "checkPermissions: Permissions Granted. Now initializing")
            addUserInfoInDB()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.RC_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && !grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                    Log.d(_tag, "onRequestPermissionsResult: Permission Granted")
                    Handler().postDelayed({
                        addUserInfoInDB()
                    }, 1000)
                } else {
                    Log.d(_tag, "onRequestPermissionsResult: Permissions not granted")
                    activity!!.root_layout.snackbar("Please grant permissions", "GRANT") {
                        checkPermissions(permissions)
                    }
                    //TODO MainInfoFragment
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_SIGN_IN -> {
                Log.d(_tag, "onActivityResult: Request Code Received")
                val response = IdpResponse.fromResultIntent(data)
                when (resultCode) {
                    RESULT_OK -> {
                        Log.d(_tag, "onActivityResult: Result was ok")
                        val user = FirebaseAuth.getInstance().currentUser
                        when {
                            user != null -> {
                                PreferenceManager.getDefaultSharedPreferences(_context).edit()
                                        .putBoolean(PREF_KEY_FIRST_START, false)
                                        .apply()
                                val handler = Handler()
                                handler.postDelayed({ checkPermissions(permissions) }, 500)
                            }
                        }
                    }
                    else -> {
                        when {
                            response == null -> {
                                // User pressed back button
                                Toast.makeText(_context, "Sign In Cancelled", Toast.LENGTH_SHORT).show()
                            }
                            response.error!!.errorCode == ErrorCodes.NO_NETWORK -> {
                                Toast.makeText(_context, "No network", Toast.LENGTH_SHORT).show()
                            }
                            response.error!!.errorCode == ErrorCodes.UNKNOWN_ERROR -> {
                                Toast.makeText(_context, "Unknown Error", Toast.LENGTH_SHORT).show()
                            }
                            else -> Toast.makeText(_context, "Sign In failed. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                        activity!!.finish()
                    }
                }
            }
        }
    }

    private fun addUserInfoInDB() {
        val auth = FirebaseAuth.getInstance()
        val curUser = auth.currentUser
        /*if (curUser?.displayName == null || curUser.email == null || curUser.phoneNumber == null) {
            createdView.findNavController().navigate(R.id.action_groupsFragment_to_editProfileFragment)
            return
        }*/
        val userRef = db.collection(DBConstants.USERS).document(curUser!!.uid)
        Log.d(_tag, "addUserInfoInDB: Started")
        userRef.get().addOnCompleteListener { getUserTask ->
            when {
                getUserTask.isSuccessful -> if (!getUserTask.result!!.exists()) {
                    val dialog = MaterialDialog.Builder(_context).progress(true, 1).content("Creating User...").show()
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
            initFirebaseDb()
        }
    }

    private fun signInDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(_context)
                .setTitle("Sign In")
                .setMessage("Please Sign In to Continue")
                .setPositiveButton("Sign In") { _, _ ->
                    startActivityForResult(AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(false)
                            .setAvailableProviders(arrayListOf(AuthUI.IdpConfig.PhoneBuilder().build(),
                                    AuthUI.IdpConfig.GoogleBuilder().build()))
                            .build(), Constants.RC_SIGN_IN)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    activity!!.finish()
                }.setCancelable(false)
    }


    private fun initFirebaseDb() {
        Log.d(_tag, "initFirebaseDb: Started")
        groupsAdapter = GroupAdapter(object : ListClickCallbacks<QuerySnapshot> {
            override fun onListItemClick(item: QuerySnapshot?, docId: String) {
                val action = GroupsFragmentDirections
                        .toExpenseList(docId)
                activity!!.host_nav_fragment.findNavController().navigate(action)
            }

            override fun onListItemLongClick(item: QuerySnapshot?, docId: String) {

            }

        })
        list?.apply {
            val lm = LinearLayoutManager(this.context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
        }

        val groupsViewModel = ViewModelProviders.of(this).get(GroupsViewModel::class.java)
        groupsViewModel.getGroupSnapshots(FirebaseAuth.getInstance().uid!!).observe(this, Observer<QuerySnapshot> { snapshot ->
            groupsAdapter!!.setSnapshots(snapshot)
            list.adapter = groupsAdapter
            activity?.apply {
                progress?.visibility = View.GONE
                fab.show()
            }
        })
        Log.d(_tag, "initFirebaseDb: Ended")
    }
}
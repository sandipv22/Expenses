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
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.model.User
import com.afterroot.expenses.utils.*
import com.afterroot.expenses.utils.Constants.PREF_KEY_FIRST_START
import com.afterroot.expenses.utils.Constants.RC_SIGN_IN
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_groups.*
import kotlinx.android.synthetic.main.list_item_group.view.*
import org.jetbrains.anko.design.snackbar
import java.util.*

class GroupsFragment : Fragment() {
    private var groupsAdapter: FirestoreRecyclerAdapter<Group, ViewHolder>? = null
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
        progress.visibility = View.VISIBLE
        createdView = view
        _context = createdView.context
        val dbSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        db = FirebaseFirestore.getInstance().apply { firestoreSettings = dbSettings }
        when {
            PreferenceManager.getDefaultSharedPreferences(view.context).getBoolean(Constants.PREF_KEY_FIRST_START, true)
                    or !FirebaseUtils.isUserSignedIn -> signInDialog().show()
            else -> checkPermissions(permissions)
        }

        activity!!.bottom_appbar.setNavigationOnClickListener {
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
            fragment.show(fragmentManager, fragment.tag)
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
        val uid = FirebaseAuth.getInstance().uid
        val query = db.collection(DBConstants.GROUPS)
                .whereGreaterThanOrEqualTo(
                        "${DBConstants.FIELD_GROUP_MEMBERS}.$uid",
                        DBConstants.TYPE_MEMBER
                )
        val options = FirestoreRecyclerOptions.Builder<Group>()
                .setQuery(query, Group::class.java)
                .setLifecycleOwner(this)
                .build()
        groupsAdapter = object : FirestoreRecyclerAdapter<Group, ViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val holderView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_group, parent, false)
                return ViewHolder(holderView)
            }

            override fun onBindViewHolder(holder: GroupsFragment.ViewHolder, position: Int, model: Group) {
                holder.apply {
                    itemName.text = model.group_name
                    itemDate.text = Utils.getDateDiff(model.date!!, Calendar.getInstance().time)
                }

                with(holder.itemView) {
                    tag = model
                    setOnClickListener {
                        //callbacks!!.onListItemClick(tag as Group, id)
                        val action = GroupsFragmentDirections
                                .actionGroupsFragment2ToExpenseListFragment(snapshots.getSnapshot(holder.adapterPosition).id)
                        it.findNavController().navigate(action)
                    }
                    setOnLongClickListener {
                        return@setOnLongClickListener true
                    }
                }
            }

        }
        Log.d(_tag, "initFirebaseDb: Ended")
        //progress?.visibility = View.GONE
        list?.apply {
            val lm = LinearLayoutManager(this.context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            adapter = groupsAdapter
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: AppCompatTextView = view.item_name
        val itemEmail: AppCompatTextView = view.item_email
        val itemDate: AppCompatTextView = view.item_time
    }
}
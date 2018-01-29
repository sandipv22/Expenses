package com.afterroot.expenses.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v13.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatTextView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.R
import com.afterroot.expenses.fragment.*
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.model.User
import com.afterroot.expenses.utils.*
import com.afterroot.expenses.utils.Constants.PREF_KEY_FIRST_START
import com.afterroot.expenses.utils.Constants.RC_PERMISSIONS
import com.afterroot.expenses.utils.Constants.RC_SIGN_IN
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnFragmentInteractionListener,
        OnSaveButtonClick {

    var db = FirebaseFirestore.getInstance()
    val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val dbSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        db.firestoreSettings = dbSettings

        when {
            PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREF_KEY_FIRST_START, true)
                    or !FirebaseUtils.isUserSignedIn -> signInDialog().show()
            else -> checkPermissions(permissions)
        }

        toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle!!)
        toggle!!.syncState()

        fab.hide()

        nav_view.setNavigationItemSelectedListener(this)

        toolbar.setNavigationOnClickListener {
            navigate()
        }
    }

    private fun updateHeader(view: View) {
        view.apply {
            header_username.text = FirebaseUtils.NAME
            header_email.text = FirebaseUtils.EMAIL

        }
    }

    private fun navigate() {
        when {
            supportFragmentManager.backStackEntryCount > 1 -> supportFragmentManager.popBackStack()
            supportFragmentManager.backStackEntryCount == 1 -> {
                supportFragmentManager.popBackStack()
                animateArrow(0f)
                fab.show()
            }
            supportFragmentManager.backStackEntryCount == 0 -> drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private val expenseItemListCallback = object : ListClickCallbacks<ExpenseItem> {
        override fun onListItemClick(item: ExpenseItem?) {
            replaceFragment(ExpenseDetailFragment.newInstance(item!!), R.id.fragment_container) {
                addToBackStack("expense")
            }
            animateArrow(1f)
            fab.show()
        }

        override fun onListItemLongClick(item: ExpenseItem?) {

        }

    }

    fun showProgress(value: Boolean) {
        progress.visibility = when (value) {
            true -> View.VISIBLE
            false -> View.INVISIBLE
        }
    }

    private val groupItemCallbacks = object : ListClickCallbacks<Group> {
        override fun onListItemClick(item: Group?) {
            showProgress(true)
            val query = db.collection(DBConstants.GROUPS).whereEqualTo(DBConstants.FIELD_GROUP_NAME, item?.group_name)
            query.get().addOnCompleteListener { task ->
                Log.d(TAG, "onListItemClick: ID ${task.result.documents[0].id}")
                ObjectAnimator.ofFloat(toggle!!.drawerArrowDrawable, "progress", 1f).start()
                toolbar.title = item!!.group_name
                replaceFragment(ExpenseListFragment.with(task.result.documents[0].id, expenseItemListCallback), R.id.fragment_container) {
                    addToBackStack("group")
                }
                fab.setOnClickListener {
                    fab.hide()
                    replaceFragment(AddExpenseFragment.newInstance(task.result.documents[0].id), R.id.fragment_container) {
                        addToBackStack("expense")
                    }
                }
                showProgress(false)
            }
        }

        override fun onListItemLongClick(item: Group?) {
            Toast.makeText(this@MainActivity, "Long Clicked: ${item?.group_name}", Toast.LENGTH_SHORT).show()
            val query = db.collection(DBConstants.GROUPS).whereEqualTo(DBConstants.FIELD_GROUP_NAME, item?.group_name)
            AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete ${item!!.group_name}")
                    .setMessage("Do you want to delete this group?")
                    .setPositiveButton("Delete", { dialogInterface, i ->
                        dialogInterface.dismiss()
                        val progress = MaterialDialog.Builder(this@MainActivity).progress(true, 1).content("Deleting").show()
                        query.get().addOnCompleteListener { task ->
                            db.collection(DBConstants.GROUPS).document(task.result.documents[0].id).delete().addOnSuccessListener {
                                progress.dismiss()
                            }
                        }
                    }).show()
        }

    }

    private var groupsFragment: GroupsFragment? = null
    private fun init() {
        try {
            updateHeader(nav_view.getHeaderView(0))
            groupsFragment = GroupsFragment.with(groupItemCallbacks)
            addFragment(groupsFragment!!, R.id.fragment_container)
            addUserInfoInDB()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val handler = Handler()
    private val runnable = Runnable {
        signInDialog().show()
    }

    private fun signInDialog(): AlertDialog.Builder {
        try {
            handler.removeCallbacks(runnable)
        } catch (e: Exception) {

        }
        return AlertDialog.Builder(this)
                .setTitle("Sign In")
                .setMessage("Please Sign In to Continue")
                .setPositiveButton("Sign In", { _, _ ->
                    Utils.startFirebaseUI(this, RC_SIGN_IN)
                })
                .setNegativeButton("Cancel", { _, _ ->
                    finish()
                }).setCancelable(false)
    }

    private fun addUserInfoInDB() {
        val auth = FirebaseAuth.getInstance()
        val curUser = auth.currentUser
        if (curUser?.displayName == null || curUser.email == null) {
            replaceFragment(EditProfileFragment.newInstance(), R.id.fragment_container) {
                addToBackStack("profile")
            }
            return
        }
        val userRef = db.collection(DBConstants.USERS).document(curUser.uid)
        Log.d(TAG, "addUserInfoInDB: Started")
        userRef.get().addOnCompleteListener { getUserTask ->
            if (getUserTask.isSuccessful) {
                val uidDocSnapshot = getUserTask.result
                when {
                    uidDocSnapshot.exists() -> Log.d("MainActivity", "DocumentSnapshot data: " + uidDocSnapshot.data)
                    else -> {
                        val dialog = MaterialDialog.Builder(this).progress(true, 1).content("Creating User...").show()
                        Log.d(TAG, "User not available. Creating User..")
                        val user = User(curUser.displayName!!,
                                curUser.email!!,
                                curUser.uid,
                                curUser.phoneNumber!!)

                        userRef.set(user).addOnCompleteListener { setUserTask ->
                            if (setUserTask.isSuccessful) {
                                Log.d(TAG, "User Created")
                                Log.d("MainActivity", "DocumentSnapshot data: " + setUserTask.result)
                            } else Log.e(TAG, "Can't create user", setUserTask.exception)
                            dialog.dismiss()
                        }
                    }
                }
            } else Log.e(TAG, "Unknown Error", getUserTask.exception)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                return true
            }
            R.id.sign_out -> {
                FirebaseUtils.auth!!.signOut()
                handler.postDelayed(runnable, 100)
                return true
            }
            R.id.reset_start_code -> {
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit().putBoolean(PREF_KEY_FIRST_START, true).apply()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }

        when (item.title) {
            getString(R.string.menu_new_group) -> {
                replaceFragment(NewGroupFragment(), R.id.fragment_container) {
                    addToBackStack("groups")
                }
                return true
            }
        }
        return true
    }

    private val permissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun checkPermissions(permissions: Array<out String>) {
        if (PermissionChecker(this).isPermissionsNeeded(*permissions)) {
            Log.d(TAG, "checkPermissions: Requesting Permissions")
            ActivityCompat.requestPermissions(this, permissions, Constants.RC_PERMISSIONS)
        } else {
            Log.d(TAG, "checkPermissions: Permissions Granted. Now initializing")
            init()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RC_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && !grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                    Log.d(TAG, "onRequestPermissionsResult: Permission Granted")
                    resetInfoMessage(main_info_message)
                    Handler().postDelayed({
                        init()
                    }, 1000)
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: Permissions not granted")
                    Snackbar.make(main_content, "Please grant permissions", Snackbar.LENGTH_INDEFINITE).setAction("GRANT", {
                        checkPermissions(permissions)
                    }).show()
                    setInfoMessage(main_info_message, "Please Grant Permissions")
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_profile -> {
                replaceFragment(EditProfileFragment.newInstance(), R.id.fragment_container) {
                    addToBackStack("profile")
                }
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_SIGN_IN -> {
                Log.d(TAG, "onActivityResult: Request Code Received")
                val response = IdpResponse.fromResultIntent(data)
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d(TAG, "onActivityResult: Result was ok")
                        val user = FirebaseAuth.getInstance().currentUser
                        when {
                            user != null -> {
                                PreferenceManager.getDefaultSharedPreferences(this).edit()
                                        .putBoolean(PREF_KEY_FIRST_START, false)
                                        .apply()
                                Snackbar.make(main_content, "Welcome ${user.displayName}", Snackbar.LENGTH_SHORT).show()
                                val handler = Handler()
                                handler.postDelayed({ addUserInfoInDB() }, 500)
                            }
                        }
                    }
                    else -> when {
                        response == null -> {
                            // User pressed back button
                            Toast.makeText(this, "Sign In Cancelled", Toast.LENGTH_SHORT).show()
                            return
                        }
                        response.errorCode == ErrorCodes.NO_NETWORK -> {
                            Toast.makeText(this, "No network", Toast.LENGTH_SHORT).show()
                            return
                        }
                        response.errorCode == ErrorCodes.UNKNOWN_ERROR -> {
                            Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show()
                            return
                        }
                        else -> Toast.makeText(this, "Sign In failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onSaveButtonClicked() {

    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    /**
     * @see <a href="https://medium.com/thoughts-overflow/how-to-add-a-fragment-in-kotlin-way-73203c5a450b">Source: How to Add a Fragment the Kotlin way</a></p>
     */
    private inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right).func().commit()
        resetInfoMessage(main_info_message)
    }

    private fun AppCompatActivity.addFragment(fragment: Fragment, frameId: Int, func: (FragmentTransaction.() -> FragmentTransaction)? = null) {
        when {
            func != null -> supportFragmentManager.inTransaction { add(frameId, fragment).func() }
            else -> supportFragmentManager.inTransaction { add(frameId, fragment) }
        }
    }

    private fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int, func: (FragmentTransaction.() -> FragmentTransaction)? = null) {
        when {
            func != null -> supportFragmentManager.inTransaction { replace(frameId, fragment).func() }
            else -> supportFragmentManager.inTransaction { replace(frameId, fragment) }
        }
    }

    companion object {
        private var toggle: ActionBarDrawerToggle? = null
        fun setInfoMessage(textView: AppCompatTextView, message: String) {
            with(textView) {
                visibility = View.VISIBLE
                text = message
            }
        }

        fun resetInfoMessage(textView: AppCompatTextView) {
            with(textView) {
                visibility = View.INVISIBLE
                text = null
            }
        }

        fun animateArrow(progress: Float) {
            ObjectAnimator.ofFloat(toggle!!.drawerArrowDrawable, "progress", progress).start()
        }

        fun getProgress(): Float = toggle!!.drawerArrowDrawable.progress
    }
}

package com.afterroot.expenses.ui

/*
class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnSaveButtonClick {

    var db = FirebaseFirestore.getInstance()
    val TAG = this.javaClass.simpleName
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
            FirebaseAuth.getInstance().currentUser.let {
                if (it != null) {
                    header_username.text = it.displayName
                    header_email.text = it.email
                }
            }
        }
    }

    private fun navigate() {
        supportFragmentManager.let {
            when {
                supportFragmentManager.backStackEntryCount > 1 -> supportFragmentManager.popBackStack()
                supportFragmentManager.backStackEntryCount == 1 -> {
                    supportFragmentManager.popBackStack()
                    fab.show()
                }
                supportFragmentManager.backStackEntryCount == 0 -> drawer_layout.openDrawer(GravityCompat.START)
            }
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
        override fun onListItemClick(item: ExpenseItem?, docId: String) {
            val fragment = ExpenseDetailFragment.newInstance(item!!)
            replaceFragment(fragment, R.id.fragment_container) {
                addToBackStack("expense")
            }
            fab.show()
        }

        override fun onListItemLongClick(item: ExpenseItem?, docId: String) {

        }

    }

    fun showProgress(value: Boolean) {
        progress.visibility = when (value) {
            true -> View.VISIBLE
            false -> View.INVISIBLE
        }
    }

    private val groupItemCallbacks = object : ListClickCallbacks<Group> {
        override fun onListItemClick(item: Group?, docId: String) {
            showProgress(true)
            val fragment = AddExpenseFragment.newInstance(docId)
            ObjectAnimator.ofFloat(toggle!!.drawerArrowDrawable, "progress", 1f).start()
            val expenseListFragment = ExpenseListFragment.with(expenseItemListCallback)
            expenseListFragment.arguments = Bundle().apply {
                putString("GROUP_NAME", item!!.group_name)
            }
            replaceFragment(expenseListFragment, R.id.fragment_container) {
                addToBackStack("group")
            }
            fab.setOnClickListener {
                replaceFragment(fragment, R.id.fragment_container) {
                    addToBackStack("expense")
                }
                fab.hide()
            }
            showProgress(false)
        }

        override fun onListItemLongClick(item: Group?, docId: String) {
            AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete ${item!!.group_name}")
                    .setMessage("Do you want to delete this group?")
                    .setPositiveButton("Delete") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                        val progress = MaterialDialog.Builder(this@MainActivity).progress(true, 1).content("Deleting").show()
                        db.collection(DBConstants.GROUPS).document(docId).delete().addOnSuccessListener {
                            progress.dismiss()
                        }
                    }.show()
        }

    }

    private var groupsFragment: GroupsFragment? = null
    private fun init() {
        try {
            updateHeader(nav_view.getHeaderView(0))
            groupsFragment = GroupsFragment.with(groupItemCallbacks)
            addFragment(groupsFragment!!, R.id.fragment_container)
            addUserInfoInDB()

            val token = FirebaseInstanceId.getInstance().token

            Log.d(TAG, "init: FCM token: $token")
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
                .setPositiveButton("Sign In") { _, _ ->
                    Utils.startFirebaseUI(this, RC_SIGN_IN)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    finish()
                }.setCancelable(false)
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
            when {
                getUserTask.isSuccessful -> if (!getUserTask.result!!.exists()) {
                    val dialog = MaterialDialog.Builder(this).progress(true, 1).content("Creating User...").show()
                    Log.d(TAG, "User not available. Creating User..")
                    val firebaseUser = User(curUser.displayName!!,
                            curUser.email!!,
                            curUser.uid,
                            curUser.phoneNumber!!)

                    userRef.set(firebaseUser).addOnCompleteListener { setUserTask ->
                        when {
                            setUserTask.isSuccessful -> {
                                Log.d(TAG, "User Created")
                                Log.d(TAG, "DocumentSnapshot data: " + setUserTask.result)
                            }
                            else -> Log.e(TAG, "Can't create firebaseUser", setUserTask.exception)
                        }
                        dialog.dismiss()
                    }
                }
                else -> Log.e(TAG, "Unknown Error", getUserTask.exception)
            }
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
            androidx.legacy.app.ActivityCompat.requestPermissions(this, permissions, Constants.RC_PERMISSIONS)
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
                    com.google.android.material.snackbar.Snackbar.make(main_content, "Please grant permissions", com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE).setAction("GRANT", {
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
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        when {
                            firebaseUser != null -> {
                                PreferenceManager.getDefaultSharedPreferences(this).edit()
                                        .putBoolean(PREF_KEY_FIRST_START, false)
                                        .apply()
                                com.google.android.material.snackbar.Snackbar.make(main_content, "Welcome ${firebaseUser.displayName}", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                                val handler = Handler()
                                handler.postDelayed({ checkPermissions(permissions) }, 500)
                            }
                        }
                    }
                    else -> when {
                        response == null -> {
                            // User pressed back button
                            Toast.makeText(this, "Sign In Cancelled", Toast.LENGTH_SHORT).show()
                            return
                        }
                        response.error!!.errorCode == ErrorCodes.NO_NETWORK -> {
                            Toast.makeText(this, "No network", Toast.LENGTH_SHORT).show()
                            return
                        }
                        response.error!!.errorCode == ErrorCodes.UNKNOWN_ERROR -> {
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

    */
/**
     * @see <a href="https://medium.com/thoughts-overflow/how-to-add-a-fragment-in-kotlin-way-73203c5a450b">Source: How to Add a Fragment the Kotlin way</a></p>
 *//*

    private inline fun androidx.fragment.app.FragmentManager.inTransaction(func: androidx.fragment.app.FragmentTransaction.() -> androidx.fragment.app.FragmentTransaction) {
        beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right).func().commit()
        resetInfoMessage(main_info_message)
    }

    private fun AppCompatActivity.addFragment(fragment: androidx.fragment.app.Fragment, frameId: Int, func: (androidx.fragment.app.FragmentTransaction.() -> androidx.fragment.app.FragmentTransaction)? = null) {
        when {
            func != null -> supportFragmentManager.inTransaction { add(frameId, fragment).func() }
            else -> supportFragmentManager.inTransaction { add(frameId, fragment) }
        }
    }

    private fun AppCompatActivity.replaceFragment(fragment: androidx.fragment.app.Fragment, frameId: Int, func: (androidx.fragment.app.FragmentTransaction.() -> androidx.fragment.app.FragmentTransaction)? = null) {
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
    }
}
*/

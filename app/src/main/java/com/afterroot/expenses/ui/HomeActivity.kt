package com.afterroot.expenses.ui

import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.afterroot.expenses.R
import com.afterroot.expenses.fragment.BottomNavigationDrawerFragment
import com.afterroot.expenses.utils.Constants.PREF_KEY_FIRST_START
import com.afterroot.expenses.utils.FirebaseUtils
import com.google.android.material.bottomappbar.BottomAppBar
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    private val _tag = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(bottom_appbar)

        host_nav_fragment.findNavController().addOnDestinationChangedListener { controller, destination, arguments ->
            Log.d(_tag, "onDestinationChange: ${destination.label}")
            when (destination.id) {
                R.id.groupsFragment -> {
                    bottom_appbar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
                    fab.setOnClickListener {
                        controller.navigate(R.id.newGroupFragment)
                    }
                }
                R.id.expenseListFragment -> {
                    bottom_appbar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
                    fab.setOnClickListener {
                        controller.navigate(R.id.addExpenseFragment)
                    }
                }
            }
        }

        bottom_appbar.setNavigationOnClickListener {
            val fragment = BottomNavigationDrawerFragment()
            fragment.show(supportFragmentManager, fragment.tag)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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
                return true
            }
            R.id.reset_start_code -> {
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit().putBoolean(PREF_KEY_FIRST_START, true).apply()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
        return super.onOptionsItemSelected(item)
    }
}

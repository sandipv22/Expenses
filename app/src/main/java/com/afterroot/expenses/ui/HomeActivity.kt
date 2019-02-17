package com.afterroot.expenses.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.afterroot.expenses.R
import com.google.android.material.bottomappbar.BottomAppBar
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*


class HomeActivity : AppCompatActivity() {

    private val _tag = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(bottom_appbar)

        host_nav_fragment.findNavController().addOnDestinationChangedListener { controller, destination, arguments ->
            Log.d(_tag, "onDestinationChange: ${destination.label}")
            action_title.text = destination.label
            when (destination.id) {
                com.afterroot.expenses.R.id.groupsFragment -> {
                    fab.show()
                    bottom_appbar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
                    fab.setOnClickListener {
                        controller.navigate(com.afterroot.expenses.R.id.newGroupFragment)
                        fab.hide()
                    }
                }
                com.afterroot.expenses.R.id.expenseListFragment -> {
                    fab.show()
                    bottom_appbar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
                }
                com.afterroot.expenses.R.id.newGroupFragment -> {
                    fab.hide()
                }
                com.afterroot.expenses.R.id.addExpenseFragment -> {
                    fab.hide()
                }
            }
        }
    }
}

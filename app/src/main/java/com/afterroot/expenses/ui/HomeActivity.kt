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

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
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
        val drawerArrowDrawable = DrawerArrowDrawable(this)
        bottom_appbar.navigationIcon = drawerArrowDrawable
        val handler = Handler()

        host_nav_fragment.findNavController().addOnDestinationChangedListener { controller, destination, arguments ->
            Log.d(_tag, "onDestinationChange: ${destination.label}")
            action_title.text = destination.label
            when (destination.id) {
                R.id.groupsFragment -> {
                    handler.postDelayed({
                        bottom_appbar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
                        val anim = ValueAnimator.ofFloat(1F, 0F)
                        anim.apply {
                            addUpdateListener {
                                drawerArrowDrawable.progress = it.animatedValue as Float
                            }
                            interpolator = DecelerateInterpolator()
                            duration = 200
                            start()
                        }
                    }, 100)
                    return@addOnDestinationChangedListener
                }
                R.id.expenseListFragment -> {
                    handler.postDelayed({
                        bottom_appbar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
                    }, 100)
                }
                R.id.newGroupFragment -> {

                }
                R.id.addExpenseFragment -> {
                    fab.hide()
                }
                R.id.expenseDetailFragment -> {
                    fab.hide()
                }
            }
            handler.postDelayed({
                val anim = ValueAnimator.ofFloat(0F, 1F)
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
    }
}

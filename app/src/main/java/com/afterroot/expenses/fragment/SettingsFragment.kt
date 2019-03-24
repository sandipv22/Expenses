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

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Expense
import com.afterroot.expenses.ui.ListClickCallbacks
import com.afterroot.expenses.viewmodel.GroupsViewModel
import com.afterroot.expenses.viewmodel.ViewModelState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.QuerySnapshot

class SettingsFragment : PreferenceFragmentCompat() {
    private var querySnapshot: QuerySnapshot? = null
    private lateinit var sharedPref: SharedPreferences
    private val groupsViewModel by lazy {
        ViewModelProviders.of(this).get(GroupsViewModel::class.java)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)
    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = PreferenceManager.getDefaultSharedPreferences(context)

        groupsViewModel.getGroupSnapshot(FirebaseAuth.getInstance().uid!!).observe(this, Observer<ViewModelState> { state ->
            when (state) {
                is ViewModelState.Loading -> {
                }

                is ViewModelState.Loaded<*> -> {
                    querySnapshot = state.data as QuerySnapshot
                }
            }
        })
    }

    override fun onPreferenceTreeClick(preference: androidx.preference.Preference?): Boolean {
        when (preference!!.key) {
            getString(R.string.pref_main_screen) -> {
                ListBottomSheetDialog.also {
                    it.with(
                            querySnapshot!!,
                            Expense.TYPE_GROUP_ALT,
                            "Select Group",
                            object : ListClickCallbacks<QuerySnapshot> {
                                override fun onListItemClick(item: QuerySnapshot?, docId: String, position: Int, view: View?) {
                                    it.dismiss()
                                    sharedPref.edit(true) {
                                        putString(getString(R.string.pref_main_screen), docId)
                                    }
                                    Toast.makeText(this@SettingsFragment.activity, "Clicked with id: $docId", Toast.LENGTH_SHORT).show()
                                }

                                override fun onListItemLongClick(item: QuerySnapshot?, docId: String, position: Int) {
                                }

                            }
                    ).show(activity!!.supportFragmentManager, "group-select")
                }
                return true
            }
        }
        return false
    }
}

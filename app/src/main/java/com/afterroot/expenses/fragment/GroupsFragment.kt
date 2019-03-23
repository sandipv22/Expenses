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

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afterroot.expenses.R
import com.afterroot.expenses.adapter.ExpenseAdapterDelegate
import com.afterroot.expenses.adapter.callback.ItemSelectedCallback
import com.afterroot.expenses.database.DBConstants
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.firebase.DeleteListener
import com.afterroot.expenses.firebase.FirebaseUtils
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.viewmodel.GroupsViewModel
import com.afterroot.expenses.viewmodel.ViewModelState
import com.afterroot.expenses.visible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.context_group.*
import kotlinx.android.synthetic.main.fragment_groups.*
import org.jetbrains.anko.design.snackbar

class GroupsFragment : Fragment(), ItemSelectedCallback {
    private var groupsAdapter: ExpenseAdapterDelegate? = null
    lateinit var db: FirebaseFirestore
    private val _tag = "GroupsFragment"
    private lateinit var _context: Context
    private lateinit var createdView: View
    private lateinit var sharedPreferences: SharedPreferences
    private val groupsViewModel: GroupsViewModel by lazy {
        ViewModelProviders.of(this).get(GroupsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createdView = view
        _context = createdView.context
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        db = Database.getInstance()
        activity!!.apply {
            fab.apply {
                setOnClickListener {
                    view.findNavController().navigate(R.id.newGroupFragment)
                }
            }
        }
        if (FirebaseUtils.isUserSignedIn) {
            initFirebaseDb()
        }
    }

    private var mSnapshot: QuerySnapshot? = null
    private fun initFirebaseDb() {
        activity!!.progress.visibility = View.VISIBLE
        groupsAdapter = ExpenseAdapterDelegate(this)
        list?.apply {
            val lm = LinearLayoutManager(this.context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            this.adapter = groupsAdapter
        }

        groupsViewModel.getGroupSnapshot(FirebaseAuth.getInstance().uid!!).observe(this, Observer<ViewModelState> { state ->
            when (state) {
                is ViewModelState.Loading -> {
                    activity!!.progress.visible(true)
                }

                is ViewModelState.Loaded<*> -> {
                    mSnapshot = state.data as QuerySnapshot
                    groupsAdapter!!.add(mSnapshot!!.toObjects(Group::class.java) as List<Group>)
                    activity?.apply {
                        progress?.visible(false)
                        fab.show()
                        text_no_groups.visible(mSnapshot!!.documents.size == 0)
                    }
                }
            }
        })
    }

    override fun onClick(position: Int, view: View?) {
        val docId = mSnapshot!!.documents[position].id
        val action = GroupsFragmentDirections.toExpenseList(docId)
        activity!!.host_nav_fragment.findNavController().navigate(action)
    }

    override fun onLongClick(position: Int) {
        val docId = mSnapshot!!.documents[position].id
        val bottomSheetDialog = BottomSheetDialog(activity!!)
        val categoryReference = db.collection(DBConstants.GROUPS).document(docId).collection(DBConstants.CATEGORIES)
        val expensesReference = db.collection(DBConstants.GROUPS)
                .document(docId).collection(DBConstants.EXPENSES)
        with(bottomSheetDialog) {
            setContentView(R.layout.context_group)
            show()
            item_edit.setOnClickListener {
                dismiss()
            }
            item_delete.setOnClickListener {
                dismiss()
                activity!!.progress.visible(true)
                categoryReference.get().addOnSuccessListener { categories ->
                    if (categories.documents.isNotEmpty()) { //check group has categories
                        categories.documents.forEach { category ->
                            Database.delete(category.reference, object : DeleteListener {
                                override fun onDeleteSuccess() {
                                    expensesReference.get().addOnSuccessListener { expenses ->
                                        if (expenses.documents.isNotEmpty()) {
                                            expenses.documents.forEach { expense ->
                                                Database.delete(expense.reference, object : DeleteListener {
                                                    override fun onDeleteSuccess() {
                                                        db.collection(DBConstants.GROUPS).document(docId).delete().addOnSuccessListener {
                                                            activity!!.apply {
                                                                root_layout.snackbar("Group Deleted.")
                                                                progress.visible(false)
                                                            }
                                                        }
                                                    }

                                                    override fun onDeleteFailed() {
                                                        activity!!.progress.visible(false)
                                                    }

                                                })
                                            }
                                        } else {
                                            db.collection(DBConstants.GROUPS).document(docId).delete().addOnSuccessListener {
                                                activity!!.root_layout.snackbar("Group Deleted.")
                                                activity!!.progress.visible(true)
                                            }
                                        }

                                    }
                                }

                                override fun onDeleteFailed() {
                                    activity!!.progress.visible(false)
                                }

                            })
                        }
                    } else { //no categories, direct delete expenses
                        db.collection(DBConstants.GROUPS).document(docId).collection(DBConstants.EXPENSES).get().addOnSuccessListener {
                            if (it.documents.isNotEmpty()) {
                                it.documents.forEach { documentSnapshot ->
                                    documentSnapshot.reference.delete().addOnSuccessListener {
                                        db.collection(DBConstants.GROUPS).document(docId).delete().addOnSuccessListener {
                                            activity!!.apply {
                                                root_layout.snackbar("Group Deleted.")
                                                progress.visible(false)
                                            }
                                        }
                                    }
                                }
                            } else {
                                db.collection(DBConstants.GROUPS).document(docId).delete().addOnSuccessListener {
                                    activity!!.apply {
                                        root_layout.snackbar("Group Deleted.")
                                        progress.visible(false)
                                    }
                                }
                            }

                        }
                    }

                }
            }
        }
    }
}
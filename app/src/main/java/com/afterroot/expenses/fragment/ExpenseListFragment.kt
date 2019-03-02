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

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afterroot.expenses.R
import com.afterroot.expenses.adapter.ExpenseAdapter
import com.afterroot.expenses.model.Expense.Companion.TYPE_EXPENSE
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.ExpensesViewModel
import com.afterroot.expenses.utils.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.context_group.*
import kotlinx.android.synthetic.main.fragment_expense_list.*

class ExpenseListFragment : Fragment(), ListClickCallbacks<QuerySnapshot> {
    private var adapter: ExpenseAdapter? = null
    private var db: FirebaseFirestore = Database.getInstance()
    lateinit var groupDocID: String
    private val _tag = "ExpenseListFragment"
    private val args: ExpenseListFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_expense_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        groupDocID = args.groupDocId
        activity!!.progress.visible(true)
        Handler().postDelayed({
            initFirebaseDb()
        }, 200)

        activity!!.fab.apply {
            setOnClickListener {
                Log.d(_tag, "onViewCreated: FAB Clicked")
                val action = ExpenseListFragmentDirections.toAddExpense(groupDocID)
                view.findNavController().navigate(action)
            }
        }
    }

    private fun initFirebaseDb() {
        Log.d(_tag, "initFirebaseDb: groupId: $groupDocID")

        adapter = ExpenseAdapter(this)

        list.apply {
            val lm = LinearLayoutManager(this.context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
        }

        val expensesViewModel = ViewModelProviders.of(this).get(ExpensesViewModel::class.java)
        expensesViewModel.getSnapshot(groupDocID).observe(this, Observer<QuerySnapshot> { snapshot ->
            adapter!!.setSnapshot(snapshot, TYPE_EXPENSE)
            list.adapter = adapter
            activity!!.progress.visible(false)
        })
    }

    override fun onListItemClick(item: QuerySnapshot?, docId: String, position: Int) {
        val bundle = Bundle().apply {
            putSerializable(Constants.KEY_EXPENSE_SERIALIZE, adapter!!.mList[position] as ExpenseItem)
        }
        Log.d(_tag, "onListItemClick: ${bundle.getSerializable(Constants.KEY_EXPENSE_SERIALIZE)}")
        view!!.findNavController().navigate(R.id.toExpenseDetail, bundle)
    }

    override fun onListItemLongClick(item: QuerySnapshot?, docId: String, position: Int) {
        val bottomSheetDialog = BottomSheetDialog(context!!)
        with(bottomSheetDialog) {
            setContentView(R.layout.context_group)
            show()
            item_edit.setOnClickListener {
                dismiss()
                Log.d(_tag, "onListItemLongClick: Clicked")
            }
            item_delete.setOnClickListener {
                dismiss()
                AlertDialog.Builder(view!!.context)
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to delete this expense?")
                        .setPositiveButton("Delete") { _, _ ->
                            item?.documents?.forEach {
                                Database.delete(it.reference, object : DeleteListener {
                                    override fun onDeleteSuccess() {
                                        adapter?.notifyItemRemoved(position)
                                    }

                                    override fun onDeleteFailed() {

                                    }

                                })
                            }
                        }.setNegativeButton(getString(R.string.text_cancel)) { _, _ ->

                        }

            }

        }

    }
}

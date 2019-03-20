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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afterroot.expenses.Constants
import com.afterroot.expenses.DeleteListener
import com.afterroot.expenses.R
import com.afterroot.expenses.adapter.ExpenseAdapterDelegate
import com.afterroot.expenses.adapter.ItemSelectedCallback
import com.afterroot.expenses.database.DBConstants
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.viewmodel.ExpensesViewModel
import com.afterroot.expenses.visible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.context_group.*
import kotlinx.android.synthetic.main.fragment_expense_list.*
import kotlinx.android.synthetic.main.list_item_expense.view.*
import java.util.*

class ExpenseListFragment : Fragment(), ItemSelectedCallback {
    private var adapter: ExpenseAdapterDelegate? = null
    private lateinit var groupDocID: String
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
                val action = ExpenseListFragmentDirections.toAddExpense(groupDocID, null)
                view.findNavController().navigate(action)
            }
        }
    }

    var mSnapshot: QuerySnapshot? = null
    private fun initFirebaseDb() {
        adapter = ExpenseAdapterDelegate(this)

        list.apply {
            val lm = LinearLayoutManager(this.context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
        }

        val expensesViewModel = ViewModelProviders.of(this).get(ExpensesViewModel::class.java)
        expensesViewModel.getSnapshot(groupDocID).observe(this, Observer<QuerySnapshot> { snapshot ->
            list.adapter = adapter
            mSnapshot = snapshot
            adapter!!.add(snapshot.toObjects(ExpenseItem::class.java) as List<ExpenseItem>)
            activity!!.apply {
                progress.visible(false)
                text_no_expenses.visible(mSnapshot!!.documents.size == 0)
            }
        })
    }

    override fun onClick(position: Int, view: View?) {
        val expenseItem = mSnapshot!!.documents[position].toObject(ExpenseItem::class.java) as ExpenseItem
        val bundle = Bundle().apply {
            putSerializable(Constants.KEY_EXPENSE_SERIALIZE, expenseItem)
            putString("ANIM_AMOUNT", ViewCompat.getTransitionName(view!!.item_amount))
            putString("ANIM_CATEGORY", ViewCompat.getTransitionName(view.item_category))
            putString("ANIM_NOTE", ViewCompat.getTransitionName(view.item_note))
            putString("ANIM_DATE", ViewCompat.getTransitionName(view.item_date))
            putString("ANIM_PAID_BY", ViewCompat.getTransitionName(view.item_paid_by))
        }
        with(view!!) {
            val extras = FragmentNavigatorExtras(
                    this.item_amount to ViewCompat.getTransitionName(this.item_amount)!!,
                    this.item_category to ViewCompat.getTransitionName(this.item_category)!!,
                    this.item_note to ViewCompat.getTransitionName(this.item_note)!!,
                    this.item_date to ViewCompat.getTransitionName(this.item_date)!!,
                    this.item_paid_by to ViewCompat.getTransitionName(this.item_paid_by)!!)
            this@ExpenseListFragment.view!!.findNavController().navigate(R.id.toExpenseDetail, bundle, null, extras)
        }
    }

    override fun onLongClick(position: Int) {
        val docId = mSnapshot!!.documents[position].id
        val bottomSheetDialog = BottomSheetDialog(context!!)
        with(bottomSheetDialog) {
            setContentView(R.layout.context_group)
            show()
            item_edit.setOnClickListener {
                val expenseItem = mSnapshot!!.documents[position].toObject(ExpenseItem::class.java)
                val action = ExpenseListFragmentDirections.toAddExpense(groupDocID, docId)
                with(Bundle()) {
                    putAll(action.arguments)
                    putSerializable(Constants.KEY_EXPENSE_SERIALIZE, expenseItem)
                    view!!.findNavController().navigate(action.actionId, this)
                }

                dismiss()
            }
            item_delete.setOnClickListener {
                dismiss()
                AlertDialog.Builder(view!!.context)
                        .setTitle(getString(R.string.text_dialog_confirm))
                        .setMessage(getString(R.string.msg_dialog_delete_expense))
                        .setPositiveButton(getString(R.string.text_delete)) { _, _ ->
                            val reference = Database.getInstance().collection(DBConstants.GROUPS).document(groupDocID).collection(DBConstants.EXPENSES)
                            Database.delete(reference.document(docId), object : DeleteListener {
                                override fun onDeleteSuccess() {
                                    adapter?.remove(position)
                                }

                                override fun onDeleteFailed() {
                                }
                            })
                        }.setNegativeButton(getString(R.string.text_cancel)) { _, _ ->
                        }.show()
            }
        }
    }
}

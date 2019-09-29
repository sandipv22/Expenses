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

import android.animation.LayoutTransition
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afterroot.expenses.Constants
import com.afterroot.expenses.R
import com.afterroot.expenses.adapter.ExpenseAdapterDelegate
import com.afterroot.expenses.adapter.callback.ItemSelectedCallback
import com.afterroot.expenses.database.DBConstants
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.firebase.DeleteListener
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.viewmodel.ExpensesViewModel
import com.afterroot.expenses.viewmodel.ViewModelState
import com.afterroot.expenses.visible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.context_group.*
import kotlinx.android.synthetic.main.fragment_expense_list.*
import kotlinx.android.synthetic.main.list_item_expense.view.*

class ExpenseListFragment : Fragment(), ItemSelectedCallback {
    private lateinit var groupDocID: String
    private val _tag = "ExpenseListFragment"
    private val args: ExpenseListFragmentArgs by navArgs()
    private val expensesViewModel: ExpensesViewModel by viewModels()
    private var expenseAdapter: ExpenseAdapterDelegate? = null
    private var mSnapshot: QuerySnapshot? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_expense_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        groupDocID = args.groupDocId!!
        activity!!.progress.visible(true)
        Handler().postDelayed({
            initFirebaseDb()
        }, 50)

        var height = 0
        activity!!.fab.apply {
            setOnClickListener {
                val action = ExpenseListFragmentDirections.toAddExpense(groupDocID, null)
                view.findNavController().navigate(action)
            }
            height = getHeight()
        }
        space_view.layoutParams = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, height + 32)
    }

    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(true)
        activity!!.toolbar.apply {
            title = arguments!!.getString(Constants.ARG_GROUP_NAME)
            subtitle = null
            this.layoutTransition = LayoutTransition()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_expenses_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_summary -> {
                val fragment = ExpenseSummaryDialogFragment()
                fragment.arguments = bundleOf(Constants.ARG_GROUP_ID to groupDocID)
                fragment.show(parentFragmentManager, "summary")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initFirebaseDb() {
        expenseAdapter = ExpenseAdapterDelegate(this)

        list.apply {
            val lm = LinearLayoutManager(this.context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            this.adapter = expenseAdapter
        }

        expensesViewModel.getSnapshot(groupDocID).observe(this, Observer<ViewModelState> { state ->
            when (state) {
                is ViewModelState.Loading -> {
                    activity!!.progress.visible(true)
                }

                is ViewModelState.Loaded<*> -> {
                    mSnapshot = state.data as QuerySnapshot
                    expenseAdapter!!.add(mSnapshot!!.toObjects(ExpenseItem::class.java) as List<ExpenseItem>)
                    activity?.apply {
                        progress?.visible(false)
                        text_no_expenses.visible(mSnapshot!!.documents.size == 0)
                    }
                }
            }
        })
    }

    override fun onClick(position: Int, view: View?) {
        val expenseItem = mSnapshot!!.documents[position].toObject(ExpenseItem::class.java) as ExpenseItem
        val bundle = bundleOf(
            Constants.KEY_EXPENSE_SERIALIZE to expenseItem,
            "ANIM_AMOUNT" to ViewCompat.getTransitionName(view!!.item_amount),
            "ANIM_CATEGORY" to ViewCompat.getTransitionName(view.item_category),
            "ANIM_NOTE" to ViewCompat.getTransitionName(view.item_note),
            "ANIM_DATE" to ViewCompat.getTransitionName(view.item_date),
            "ANIM_PAID_BY" to ViewCompat.getTransitionName(view.item_paid_by)
        )
        with(view) {
            val extras = FragmentNavigatorExtras(
                this.item_amount to ViewCompat.getTransitionName(this.item_amount)!!,
                this.item_category to ViewCompat.getTransitionName(this.item_category)!!,
                this.item_note to ViewCompat.getTransitionName(this.item_note)!!,
                this.item_date to ViewCompat.getTransitionName(this.item_date)!!,
                this.item_paid_by to ViewCompat.getTransitionName(this.item_paid_by)!!
            )
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
                        val reference = Database.getInstance().collection(DBConstants.GROUPS).document(groupDocID)
                            .collection(DBConstants.EXPENSES)
                        Database.delete(reference.document(docId), object : DeleteListener {
                            override fun onDeleteSuccess() {
                                expenseAdapter!!.notifyItemRemoved(position)
                            }
                        })
                    }.setNegativeButton(getString(R.string.text_cancel)) { _, _ ->
                    }.show()
            }
        }
    }
}

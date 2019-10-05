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
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.util.set
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import com.afterroot.expenses.Constants
import com.afterroot.expenses.R
import com.afterroot.expenses.adapter.DelegateAdapter
import com.afterroot.expenses.adapter.callback.ItemSelectedCallback
import com.afterroot.expenses.database.DBConstants
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.firebase.QueryCallback
import com.afterroot.expenses.model.Expense
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.ExpensesSummary
import com.afterroot.expenses.model.User
import com.afterroot.expenses.viewmodel.ExpensesViewModel
import com.afterroot.expenses.viewmodel.ViewModelState
import com.afterroot.expenses.visible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.fragment_expense_summary.*

class ExpenseSummaryDialogFragment : BottomSheetDialogFragment(), ItemSelectedCallback,
    AdapterView.OnItemSelectedListener {
    private val _tag = "ExpenseSummaryDialog"
    private val expensesViewModel: ExpensesViewModel by viewModels()
    private val names = ArrayList<String>()
    private val uidMap = SparseArray<User>()
    private var rawUserMap = HashMap<String, User>()
    private var myAdapter: DelegateAdapter? = null
    private var mGroupId: String? = null
    private var mSnapshot: QuerySnapshot? = null
    private var mList = ArrayList<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        mGroupId = arguments!!.getString(Constants.ARG_GROUP_ID)
        super.onCreate(savedInstanceState)
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_expense_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadContent()
    }

    private fun loadContent() {
        loaded(false)
        expensesViewModel.getGroupMembers(mGroupId!!, object : QueryCallback<HashMap<String, User>> {
            override fun onSuccess(value: HashMap<String, User>) {
                loaded(true)
                rawUserMap = value
                try {
                    var i = 0
                    value.forEach {
                        names.add(it.value.name)
                        uidMap[i] = it.value
                        i++
                    }
                    val adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    users_spinner.apply {
                        this.adapter = adapter
                        onItemSelectedListener = this@ExpenseSummaryDialogFragment
                    }
                } catch (e: Exception) {

                }
            }
        })

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    //On Spinner Item Selected
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedUid: String? = uidMap[position]!!.uid
        val paidByQuery = Database.getInstance()
            .collection(DBConstants.GROUPS)
            .document(mGroupId!!)
            .collection(DBConstants.EXPENSES)
        try {
            expensesViewModel.getSnapshot(paidByQuery).observe(this@ExpenseSummaryDialogFragment, Observer {
                when (it) {
                    is ViewModelState.Loaded<*> -> {
                        try {
                            val expenses =
                                (it.data as QuerySnapshot).toObjects(ExpenseItem::class.java) as List<ExpenseItem>
                            val map = HashMap<String, ExpensesSummary>()
                            val list = ArrayList<ExpensesSummary>()
                            expenses.forEach { item ->
                                val count = item.with!!.size + 1
                                val divided = item.amount / count
                                if (item.with!!.contains(selectedUid)) {
                                    map[item.paidBy!!] = ExpensesSummary(
                                        rawUserMap[item.paidBy!!]!!.name,
                                        map[item.paidBy!!]?.payable ?: 0 + divided,
                                        map[item.paidBy!!]?.receivable ?: 0
                                    )
                                    Log.d(_tag, "Divided: $divided Payable ${map[item.paidBy!!]}")
                                }
                                if (item.paidBy.equals(selectedUid!!.trim())) {
                                    item.with!!.forEach { withId ->
                                        map[withId.key] = ExpensesSummary(
                                            rawUserMap[withId.key]!!.name,
                                            map[withId.key]?.payable ?: 0,
                                            map[withId.key]?.receivable ?: 0 + divided
                                        )
                                        Log.d(_tag, "Divided: $divided Receivable ${map[withId.key]}")
                                    }
                                }
                                map[item.paidBy!!] = ExpensesSummary(
                                    rawUserMap[item.paidBy!!]!!.name,
                                    map[item.paidBy!!]?.payable ?: 0,
                                    map[item.paidBy!!]?.receivable ?: 0
                                )
                                Log.d(_tag, "Final ${map[item.paidBy!!]}")
                            }
                            map.remove(selectedUid)
                            map.forEach { mapItem ->
                                list.add(mapItem.value)
                            }
                            loaded(true)
                            loadToAdapter(list)

                        } catch (e: Exception) {

                        }
                    }
                    is ViewModelState.Loading -> {
                        loaded(false)
                    }
                }
            })
        } catch (e: Exception) {

        }
    }

    private fun loadToAdapter(list: ArrayList<ExpensesSummary>) {
        myAdapter = DelegateAdapter(this)

        expense_summary_members_list.apply {
            val lm = LinearLayoutManager(context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            adapter = myAdapter
        }

        myAdapter!!.add(list)
    }

    override fun onClick(position: Int, view: View?) {
    }

    private fun loaded(loaded: Boolean) {
        content.visible(loaded, AutoTransition())
        progress.visible(!loaded, AutoTransition())
    }
}
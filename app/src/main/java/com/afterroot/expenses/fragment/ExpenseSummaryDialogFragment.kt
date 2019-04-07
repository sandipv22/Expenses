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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.transition.AutoTransition
import com.afterroot.expenses.Constants
import com.afterroot.expenses.R
import com.afterroot.expenses.database.DBConstants
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.firebase.QueryCallback
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.User
import com.afterroot.expenses.viewmodel.ExpensesViewModel
import com.afterroot.expenses.viewmodel.ViewModelState
import com.afterroot.expenses.visible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.fragment_expense_summary.*

class ExpenseSummaryDialogFragment : BottomSheetDialogFragment() {
    private val expensesViewModel: ExpensesViewModel by lazy {
        ViewModelProviders.of(this).get(ExpensesViewModel::class.java)
    }
    private val _tag = "ExpenseSummaryDialog"
    private var mGroupId: String? = null
    private var mSnapshot: QuerySnapshot? = null

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
        users_spinner.visible(true, AutoTransition())
        total_spend.visible(true, AutoTransition())
        expensesViewModel.getGroupMembers(mGroupId!!, object : QueryCallback<HashMap<String, User>> {
            override fun onSuccess(value: HashMap<String, User>) {
                try {
                    val array = ArrayList<String>()
                    val uidMap = HashMap<Int, User>()
                    var i = 0
                    value.forEach {
                        array.add(it.value.name)
                        uidMap[i] = it.value
                        i++
                    }
                    val adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_item, array)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    users_spinner.apply {
                        this.adapter = adapter
                        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(parent: AdapterView<*>?) {

                            }

                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                try {
                                    val uid: String? = uidMap[position]!!.uid
                                    Log.d(_tag, "onItemSelected: $uid")
                                    if (uid != null) {
                                        val paidByQuery = Database.getInstance()
                                                .collection(DBConstants.GROUPS)
                                                .document(mGroupId!!)
                                                .collection(DBConstants.EXPENSES)
                                                .whereEqualTo(DBConstants.FIELD_PAID_BY, uid)
                                        expensesViewModel.getSnapshot(paidByQuery).observe(this@ExpenseSummaryDialogFragment, Observer {
                                            when (it) {
                                                is ViewModelState.Loaded<*> -> {
                                                    val snapshot = it.data as QuerySnapshot
                                                    val expenses = snapshot.toObjects(ExpenseItem::class.java) as List<ExpenseItem>
                                                    //expenseAdapter!!.add(expenses) to apply filter to whole list
                                                    var total: Long = 0
                                                    expenses.forEach { item ->
                                                        total += item.amount
                                                    }
                                                    this@ExpenseSummaryDialogFragment.total_spend.text = String.format("%s%d", context.resources.getString(R.string.rs_symbol), total)
                                                }
                                                is ViewModelState.Loading -> {
                                                }
                                            }
                                        })
                                    }
                                } catch (e: Exception) {

                                }

                            }

                        }
                    }
                } catch (e: Exception) {

                }
            }

            override fun onFailed(message: String) {
            }

            override fun onSnapshot(snapshot: DocumentSnapshot) {
            }

        })


    }
}
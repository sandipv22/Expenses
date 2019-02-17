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
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afterroot.expenses.R
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.utils.Constants
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.Utils
import com.afterroot.expenses.utils.getDrawableExt
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.fragment_expense_list.*
import kotlinx.android.synthetic.main.list_item_expense.view.*

class ExpenseListFragment : Fragment() {
    private var firestoreAdapter: FirestoreRecyclerAdapter<ExpenseItem, ExpenseViewHolder>? = null
    private var db: FirebaseFirestore? = null
    lateinit var groupDocID: String
    val _tag = "ExpenseListFragment"
    val args: ExpenseListFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_expense_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        groupDocID = args.groupDocId
        db = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        }
        activity!!.progress.visibility = View.VISIBLE
        initFirebaseDb()
        activity!!.fab.apply {
            setImageDrawable(activity!!.getDrawableExt(R.drawable.ic_add))
            setOnClickListener {
                Log.d(_tag, "onViewCreated: FAB Clicked")
                val action = ExpenseListFragmentDirections.toAddExpense(groupDocID)
                view.findNavController().navigate(action)
            }
        }
    }

    private fun initFirebaseDb() {
        val query = db!!.collection(DBConstants.GROUPS)
                .document(groupDocID)
                .collection(DBConstants.EXPENSES).orderBy("date", Query.Direction.DESCENDING)

        Log.d(_tag, "initFirebaseDb: groupId: $groupDocID")

        val options = FirestoreRecyclerOptions.Builder<ExpenseItem>()
                .setQuery(query, ExpenseItem::class.java)
                .setLifecycleOwner(this)
                .build()

        firestoreAdapter = object : FirestoreRecyclerAdapter<ExpenseItem, ExpenseViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
                val holderView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_expense, parent, false)
                return ExpenseViewHolder(holderView)
            }

            override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int, model: ExpenseItem) {
                holder.amountText.text = String.format("%s%d",
                        holder.amountText.context.resources.getString(R.string.rs_symbol), model.amount)
                holder.noteText.text = String.format("%s, with %s", model.note, Utils.formatNames(model.with!!))
                holder.categoryText.text = model.category

                with(holder.itemView) {
                    tag = model
                    val id = snapshots.getSnapshot(holder.adapterPosition).id
                    setOnClickListener {
                        val bundle = Bundle().apply {
                            putSerializable(Constants.KEY_EXPENSE_SERIALIZE, tag as ExpenseItem)
                        }
                        view!!.findNavController().navigate(R.id.toExpenseDetail, bundle)
                        //listener!!.onListItemClick(tag as ExpenseItem, id)
                    }

                    setOnLongClickListener {
                        //listener!!.onListItemLongClick(tag as ExpenseItem, id)
                        return@setOnLongClickListener true
                    }
                }
            }

            override fun onDataChanged() {
                Log.d(_tag, "onDataChanged: ItemCount: $itemCount")
                if (itemCount == 0) {
                    activity!!.text_no_expenses.visibility = View.VISIBLE
                } else {
                    activity!!.text_no_expenses.visibility = View.INVISIBLE
                }
            }

            override fun onError(e: FirebaseFirestoreException) {
                Toast.makeText(activity!!, e.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        activity!!.progress.visibility = View.GONE
        list.apply {
            val lm = LinearLayoutManager(this.context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            adapter = firestoreAdapter
        }
        activity!!.fab.show()
    }

    inner class ExpenseViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val amountText: AppCompatTextView = view.item_amount
        val noteText: AppCompatTextView = view.item_note
        val categoryText: AppCompatTextView = view.item_category
    }
}

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

package com.afterroot.expenses.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Expense
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.model.GroupAlt
import com.afterroot.expenses.ui.ListClickCallbacks
import com.afterroot.expenses.utils.Utils
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.list_item_expense.view.*
import kotlinx.android.synthetic.main.list_item_group.view.*
import java.util.*

class ExpenseAdapter(callbacks: ListClickCallbacks<QuerySnapshot>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var mList: List<Expense>
    private lateinit var mSnapshot: QuerySnapshot
    private var mCallbacks: ListClickCallbacks<QuerySnapshot> = callbacks

    fun setSnapshot(snapshot: QuerySnapshot, type: Int) {
        mSnapshot = snapshot
        when (type) {
            Expense.TYPE_GROUP -> mList = snapshot.toObjects(Group::class.java)
            Expense.TYPE_EXPENSE -> mList = snapshot.toObjects(ExpenseItem::class.java)
            Expense.TYPE_GROUP_ALT -> mList = snapshot.toObjects(GroupAlt::class.java)
        }
        notifyItemRangeInserted(0, mList.size)
    }

    override fun getItemViewType(position: Int): Int {
        return mList[position].getType()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView: View
        var holder: RecyclerView.ViewHolder? = null
        when (viewType) {
            Expense.TYPE_GROUP -> {
                itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_group, parent, false)
                holder = GroupViewHolder(itemView)
            }
            Expense.TYPE_EXPENSE -> {
                itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_expense, parent, false)
                holder = ExpenseViewHolder(itemView)
            }
            Expense.TYPE_GROUP_ALT -> {
                itemView = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
                holder = GroupAltViewHolder(itemView)
            }
        }
        return holder!!
    }

    override fun getItemCount(): Int {
        return mSnapshot.size()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            Expense.TYPE_GROUP -> {
                (holder as GroupViewHolder).bindView(position)
            }
            Expense.TYPE_EXPENSE -> {
                (holder as ExpenseViewHolder).bindView(position)
            }
            Expense.TYPE_GROUP_ALT -> {
                (holder as GroupAltViewHolder).bindView(position)
            }
        }
    }

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val itemName: AppCompatTextView = view.item_name
        private val itemEmail: AppCompatTextView = view.item_email
        private val itemDate: AppCompatTextView = view.item_time

        fun bindView(position: Int) {
            val group = mList[position] as Group
            itemName.text = group.group_name
            itemDate.text = Utils.getDateDiff(group.date!!, Calendar.getInstance().time)

            with(itemView) {
                tag = group
                setOnClickListener {
                    mCallbacks.onListItemClick(mSnapshot, mSnapshot.documents[adapterPosition].id, position)
                }
                setOnLongClickListener {
                    mCallbacks.onListItemLongClick(mSnapshot, mSnapshot.documents[adapterPosition].id, position)
                    return@setOnLongClickListener true
                }
            }
        }
    }

    inner class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val amountText: AppCompatTextView = view.item_amount
        private val noteText: AppCompatTextView = view.item_note
        private val categoryText: AppCompatTextView = view.item_category

        fun bindView(position: Int) {
            val expenseItem = mList[position] as ExpenseItem
            amountText.text = String.format("%s%d",
                    amountText.context.resources.getString(R.string.rs_symbol), expenseItem.amount)
            noteText.text = String.format("%s, with %s", expenseItem.note, Utils.formatNames(expenseItem.with!!))
            categoryText.text = expenseItem.category

            ViewCompat.setTransitionName(amountText, amountText.toString())
            ViewCompat.setTransitionName(noteText, noteText.toString())
            ViewCompat.setTransitionName(categoryText, categoryText.toString())

            with(itemView) {
                tag = expenseItem
                setOnClickListener {
                    mCallbacks.onListItemClick(mSnapshot, mSnapshot.documents[adapterPosition].id, position, this)
                }
                setOnLongClickListener {
                    mCallbacks.onListItemLongClick(mSnapshot, mSnapshot.documents[adapterPosition].id, position)
                    return@setOnLongClickListener true
                }
            }
        }
    }

    inner class GroupAltViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var text1: TextView = itemView.findViewById(android.R.id.text1)

        fun bindView(position: Int) {
            val group = mList[position] as GroupAlt
            text1.text = group.group_name

            with(itemView) {
                tag = group
                setOnClickListener {
                    mCallbacks.onListItemClick(mSnapshot, mSnapshot.documents[adapterPosition].id, position)
                }
                setOnLongClickListener {
                    mCallbacks.onListItemLongClick(mSnapshot, mSnapshot.documents[adapterPosition].id, position)
                    return@setOnLongClickListener true
                }
            }
        }
    }

}
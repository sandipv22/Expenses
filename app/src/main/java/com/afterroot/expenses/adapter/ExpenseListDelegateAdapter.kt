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

import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.expenses.R
import com.afterroot.expenses.adapter.callback.ItemSelectedCallback
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.firebase.QueryCallback
import com.afterroot.expenses.inflate
import com.afterroot.expenses.model.Expense
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.User
import com.afterroot.expenses.utils.Utils
import kotlinx.android.synthetic.main.list_item_expense.view.*
import java.text.SimpleDateFormat
import java.util.*

class ExpenseListDelegateAdapter(val callbacks: ItemSelectedCallback) : TypeDelegateAdapter {
    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = ExpenseVH(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Expense) {
        holder as ExpenseVH
        holder.bind(item as ExpenseItem)
    }

    inner class ExpenseVH(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.list_item_expense)) {
        private val amountText: AppCompatTextView = itemView.item_amount
        private val noteText: AppCompatTextView = itemView.item_note
        private val categoryText: AppCompatTextView = itemView.item_category
        private val time: AppCompatTextView = itemView.item_date
        private val paidBy: AppCompatTextView = itemView.item_paid_by

        fun bind(item: ExpenseItem) {
            amountText.text = String.format("%s%d",
                    amountText.context.resources.getString(R.string.rs_symbol), item.amount)
            noteText.text = String.format("%s, with %s", item.note, Utils.formatNames(item.with!!))
            categoryText.text = item.category
            time.text =
                SimpleDateFormat(itemView.context.getString(R.string.date_time_format), Locale.US).format(item.date!!)
            Database.getUserByID(item.paidBy!!, object : QueryCallback<User> {
                override fun onSuccess(value: User) {
                    paidBy.text = value.name
                }
            })

            ViewCompat.setTransitionName(amountText, amountText.toString())
            ViewCompat.setTransitionName(noteText, noteText.toString())
            ViewCompat.setTransitionName(categoryText, categoryText.toString())
            ViewCompat.setTransitionName(time, time.toString())
            ViewCompat.setTransitionName(paidBy, paidBy.toString())

            with(itemView) {
                tag = item
                setOnClickListener {
                    callbacks.onClick(adapterPosition, this)
                }
                setOnLongClickListener {
                    callbacks.onLongClick(adapterPosition)
                    return@setOnLongClickListener true
                }
            }
        }
    }
}
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
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.expenses.R
import com.afterroot.expenses.adapter.callback.ItemSelectedCallback
import com.afterroot.expenses.inflate
import com.afterroot.expenses.model.Expense
import com.afterroot.expenses.model.ExpensesSummary
import kotlinx.android.synthetic.main.list_item_expense_summary.view.*

class ExpenseSummaryDelegateAdapter(val callbacks: ItemSelectedCallback) : TypeDelegateAdapter {
    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = ViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Expense) {
        holder as ViewHolder
        holder.bind(item as ExpensesSummary)
    }

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.list_item_expense_summary)) {
        private val name: AppCompatTextView = itemView.item_member_name
        private val payable: AppCompatTextView = itemView.item_payable
        private val receivable: AppCompatTextView = itemView.item_receivable

        fun bind(item: ExpensesSummary) {
            name.text = item.name
            payable.text = String.format("%s%d", "-${itemView.context.getString(R.string.rs_symbol)}", item.payable)
            receivable.text = String.format("%s%d", itemView.context.getString(R.string.rs_symbol), item.receivable)

            with(itemView) {
                tag = item
                setOnClickListener {
                    callbacks.onClick(adapterPosition, this)
                }
            }
        }
    }
}
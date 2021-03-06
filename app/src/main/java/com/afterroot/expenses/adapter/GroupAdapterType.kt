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
import com.afterroot.expenses.inflate
import com.afterroot.expenses.model.Expense
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.utils.Utils
import kotlinx.android.synthetic.main.list_item_group.view.*
import java.util.*

class GroupAdapterType(val callbacks: ItemSelectedCallback) : AdapterType {
    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = GroupVH(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Expense) {
        holder as GroupVH
        holder.bind(item as Group)
    }

    inner class GroupVH(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.list_item_group)) {
        private val itemName: AppCompatTextView = itemView.item_name
        private val itemSecondaryText: AppCompatTextView = itemView.item_secondary_text
        private val itemDate: AppCompatTextView = itemView.item_time

        fun bind(item: Group) {
            itemName.text = item.group_name
            if (item.lastEntry != null) {
                itemDate.text = Utils.getDateDiff(item.lastEntry!!, Calendar.getInstance().time)
            }
            itemSecondaryText.text = item.lastEntryText

            with(super.itemView) {
                tag = item
                setOnClickListener {
                    callbacks.onClick(adapterPosition, itemView)
                }
                setOnLongClickListener {
                    callbacks.onLongClick(adapterPosition)
                    return@setOnLongClickListener true
                }
            }
        }
    }
}


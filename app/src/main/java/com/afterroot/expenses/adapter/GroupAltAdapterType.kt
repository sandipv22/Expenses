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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.expenses.R
import com.afterroot.expenses.adapter.callback.ItemSelectedCallback
import com.afterroot.expenses.inflate
import com.afterroot.expenses.model.Expense
import com.afterroot.expenses.model.Group

class GroupAltAdapterType(val callbacks: ItemSelectedCallback) : AdapterType {
    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = GroupAltVH(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Expense) {
        holder as GroupAltVH
        holder.bind(item as Group)
    }

    inner class GroupAltVH(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.list_item_group)) {
        var text1: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(item: Group) {
            text1.text = item.group_name

            with(super.itemView) {
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
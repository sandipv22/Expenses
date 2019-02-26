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
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.utils.ListClickCallbacks
import com.afterroot.expenses.utils.Utils
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.list_item_group.view.*
import java.util.*

class GroupAdapter(callbacks: ListClickCallbacks<QuerySnapshot>) : RecyclerView.Adapter<GroupAdapter.ViewHolder>() {

    private lateinit var mList: List<Group>
    private lateinit var mSnapshot: QuerySnapshot
    private var mCallbacks: ListClickCallbacks<QuerySnapshot> = callbacks

    fun setSnapshots(snapshot: QuerySnapshot) {
        mSnapshot = snapshot
        mList = snapshot.toObjects(Group::class.java)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holderView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_group, parent, false)
        return ViewHolder(holderView)
    }

    override fun getItemCount(): Int {
        return mSnapshot.size()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            itemName.text = mList[position].group_name
            itemDate.text = Utils.getDateDiff(mList[position].date!!, Calendar.getInstance().time)
        }

        with(holder.itemView) {
            tag = mList[position]
            setOnClickListener {
                mCallbacks.onListItemClick(mSnapshot, mSnapshot.documents[holder.adapterPosition].id)
            }
            setOnLongClickListener {
                mCallbacks.onListItemLongClick(mSnapshot, mSnapshot.documents[holder.adapterPosition].id)
                return@setOnLongClickListener true
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: AppCompatTextView = view.item_name
        val itemEmail: AppCompatTextView = view.item_email
        val itemDate: AppCompatTextView = view.item_time
    }
}
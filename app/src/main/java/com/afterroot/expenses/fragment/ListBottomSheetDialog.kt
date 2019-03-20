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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afterroot.expenses.ListClickCallbacks
import com.afterroot.expenses.R
import com.afterroot.expenses.adapter.ExpenseAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.fragment_bs_dialog.*

class ListBottomSheetDialog : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bs_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ExpenseAdapter(mCallbacks!!)
        bs_list.apply {
            val lm = LinearLayoutManager(this.context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            adapter.setSnapshot(mSnapshot!!, mType)
            this.adapter = adapter
        }
        bs_title.text = mTitle
    }

    override fun onDetach() {
        mCallbacks = null
        super.onDetach()
    }

    companion object {
        private var mCallbacks: ListClickCallbacks<QuerySnapshot>? = null
        private var mSnapshot: QuerySnapshot? = null
        private var mType: Int = 0
        private var mTitle: String? = null
        var instance: ListBottomSheetDialog? = null
        fun with(snapshot: QuerySnapshot, type: Int, title: String, callback: ListClickCallbacks<QuerySnapshot>): ListBottomSheetDialog {
            this.instance = ListBottomSheetDialog().apply {
                mCallbacks = callback
                mSnapshot = snapshot
                mType = type
                mTitle = title
            }
            return instance!!
        }

        fun dismiss() {
            if (instance != null) {
                instance!!.dismiss()
            }
        }
    }
}



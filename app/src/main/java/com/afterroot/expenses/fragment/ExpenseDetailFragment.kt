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
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.transition.ChangeBounds
import androidx.transition.ChangeTransform
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.afterroot.expenses.Callbacks
import com.afterroot.expenses.Constants
import com.afterroot.expenses.R
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.User
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.android.synthetic.main.fragment_expense_detail.*
import kotlinx.android.synthetic.main.fragment_expense_detail.view.*
import java.text.SimpleDateFormat
import java.util.*

class ExpenseDetailFragment : Fragment() {
    private var item: ExpenseItem? = null
    private var fragmentView: View? = null
    private val _tag = "ExpenseDetailFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            item = it.getSerializable(Constants.KEY_EXPENSE_SERIALIZE) as ExpenseItem?
        }
        val transitionSet = TransitionSet().addTransition(ChangeBounds()).addTransition(Fade()).addTransition(ChangeTransform())
        transitionSet.ordering = TransitionSet.ORDERING_TOGETHER
        //transitionSet.duration = 100
        transitionSet.interpolator = AccelerateDecelerateInterpolator()
        sharedElementEnterTransition = transitionSet
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_expense_detail, container, false)
        arguments?.let {
            ViewCompat.setTransitionName(fragmentView!!.detail_amount, it.getString("ANIM_AMOUNT"))
            ViewCompat.setTransitionName(fragmentView!!.detail_category, it.getString("ANIM_CATEGORY"))
            ViewCompat.setTransitionName(fragmentView!!.detail_note, it.getString("ANIM_NOTE"))
            ViewCompat.setTransitionName(fragmentView!!.detail_date, it.getString("ANIM_DATE"))
            ViewCompat.setTransitionName(fragmentView!!.detail_paid_by, it.getString("ANIM_PAID_BY"))
        }
        return fragmentView
    }

    override fun onStart() {
        super.onStart()
        detail_category.text = item!!.category
        detail_amount.text = String.format("%s%d", getString(R.string.rs_symbol), item!!.amount)
        val formatter = SimpleDateFormat(getString(R.string.date_time_format), Locale.US)
        detail_date.text = formatter.format(Date(item!!.date!!.time))
        detail_note.text = item!!.note
        Database.getUserByID(item!!.paidBy!!, object : Callbacks<User> {
            override fun onSnapshot(snapshot: DocumentSnapshot) {

            }

            override fun onSuccess(value: User) {
                detail_paid_by?.text = value.name
            }

            override fun onFailed(message: String) {
            }

        })
        val builder = StringBuilder()
        var i = 0
        item!!.with!!.forEach {
            i++
            when (i) {
                item!!.with!!.size -> builder.append(it.value)
                item!!.with!!.size - 1 -> builder.append(it.value + " and ")
                else -> builder.append(it.value + ", ")
            }
            detail_spenders.text = builder
        }
    }
}

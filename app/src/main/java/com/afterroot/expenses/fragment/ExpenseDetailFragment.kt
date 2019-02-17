package com.afterroot.expenses.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afterroot.expenses.R
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.utils.Constants
import kotlinx.android.synthetic.main.fragment_expense_detail.*

class ExpenseDetailFragment : Fragment() {
    private var item: ExpenseItem? = null
    private var fragmentView: View? = null
    private val _tag = "ExpenseDetailFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
        arguments?.let {
            item = it.getSerializable(Constants.KEY_EXPENSE_SERIALIZE) as ExpenseItem?
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_expense_detail, container, false)
        return fragmentView
    }

    override fun onStart() {
        super.onStart()
        detail_category.text = item!!.category
        detail_amount.text = String.format("%s%d", getString(R.string.rs_symbol), item!!.amount)
        detail_date.text = item!!.date.toString()
        detail_note.text = item!!.note
        detail_paid_by.text = item!!.paidBy
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

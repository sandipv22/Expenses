package com.afterroot.expenses.fragment

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class BottomListFragment : BottomSheetDialogFragment() {

    lateinit var mBuilder: Builder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(mBuilder.layoutID, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mBuilder.listView!!.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mBuilder.mAdapter
        }
    }

    companion object {
        fun with(builder: Builder): BottomListFragment = BottomListFragment().apply {
            mBuilder = builder
        }
    }

    class Builder {
        var layoutID: Int = 0
        var listView: RecyclerView? = null
        var mAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

        fun setFragmentLayout(@LayoutRes id: Int, view: RecyclerView): Builder {
            this.layoutID = id
            this.listView = view
            return this
        }

        fun setAdapter(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>): Builder {
            this.mAdapter = adapter
            return this
        }

        fun build(): BottomListFragment {
            return with(this)
        }
    }
}
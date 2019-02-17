package com.afterroot.expenses.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afterroot.expenses.R
import com.afterroot.expenses.utils.NavigationItemClickCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_bottomsheet.*

class BottomNavigationDrawerFragment : BottomSheetDialogFragment() {

    val _tag = "NavigationFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nav_view.setNavigationItemSelectedListener {
            mCallback.onClick(it)
            dismissAllowingStateLoss()
            true
        }
    }

    companion object {
        lateinit var mCallback: NavigationItemClickCallback
        @JvmStatic
        fun with(callback: NavigationItemClickCallback) = BottomNavigationDrawerFragment().apply {
            mCallback = callback
        }
    }
}
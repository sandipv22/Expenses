package com.afterroot.expenses.utils

import android.view.MenuItem
import com.afterroot.expenses.model.ExpenseItem

interface ExpenseClickCallbacks {
    fun onExpenseClick(item: ExpenseItem?)
    fun onExpenseLongClick(item: ExpenseItem?)
}

interface ListClickCallbacks<in T> {
    fun onListItemClick(item: T?, docId: String)
    fun onListItemLongClick(item: T?, docId: String)
}

interface NavigationItemClickCallback {
    fun onClick(item: MenuItem)
}
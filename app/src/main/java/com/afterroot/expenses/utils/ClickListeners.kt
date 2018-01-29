package com.afterroot.expenses.utils

import android.net.Uri
import com.afterroot.expenses.model.ExpenseItem

interface ExpenseClickCallbacks {
    fun onExpenseClick(item: ExpenseItem?)
    fun onExpenseLongClick(item: ExpenseItem?)
}

interface ListClickCallbacks<in T> {
    fun onListItemClick(item: T?)
    fun onListItemLongClick(item: T?)
}

interface OnSaveButtonClick {
    fun onSaveButtonClicked()
}

interface OnFragmentInteractionListener {
    fun onFragmentInteraction(uri: Uri)
}
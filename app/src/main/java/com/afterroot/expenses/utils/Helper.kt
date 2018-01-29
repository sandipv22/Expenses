package com.afterroot.expenses.utils

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView

class Helper

class ImeHelper {
    interface DonePressedListener {
        fun onDonePressed()
    }

    fun setImeOnDoneListener(doneEditText: EditText,
                             listener: DonePressedListener) {
        doneEditText.setOnEditorActionListener(TextView.OnEditorActionListener { view, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.action == KeyEvent.ACTION_UP) {
                    listener.onDonePressed()
                }
                // We need to return true even if we didn't handle the event to continue
                // receiving future callbacks.
                return@OnEditorActionListener true
            } else if (actionId == EditorInfo.IME_ACTION_DONE) {
                listener.onDonePressed()
                return@OnEditorActionListener true
            }
            false
        })
    }
}
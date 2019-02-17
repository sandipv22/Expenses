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
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

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

fun Activity.getDrawableExt(id: Int, tint: Int? = null): Drawable {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val drawable = resources.getDrawable(id, theme)
        if (tint != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                drawable.setTint(resources.getColor(tint, theme))
            } else {
                drawable.setTint(resources.getColor(tint))
            }
        }
        return drawable
    }
    return resources.getDrawable(id)
}

fun Context.getDrawableExt(id: Int, tint: Int? = null): Drawable {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val drawable = resources.getDrawable(id, theme)
        if (tint != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                drawable.setTint(resources.getColor(tint, theme))
            } else {
                drawable.setTint(resources.getColor(tint))
            }
        }
        return drawable
    }
    return resources.getDrawable(id)
}

fun View.visible(value: Boolean) {
    visibility = when {
        value -> View.VISIBLE
        else -> View.INVISIBLE
    }
}

/**
 * Extension Function for Inflating Layout to ViewGroup
 */
fun ViewGroup.inflate(layoutId: Int, attachToRoot: Boolean = false): View =
        LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
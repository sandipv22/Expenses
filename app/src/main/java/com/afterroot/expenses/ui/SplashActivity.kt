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

package com.afterroot.expenses.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.R
import com.afterroot.expenses.isNetworkAvailable
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import org.jetbrains.anko.browse

class SplashActivity : AppCompatActivity() {

    private val _tag = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null && this.isNetworkAvailable()) {
            tryLogin()
        } else if (auth.currentUser == null && !this.isNetworkAvailable()) {
            MaterialDialog(this).show {
                title(R.string.dialog_title_no_network)
                message(R.string.dialog_msg_no_network)
                positiveButton(R.string.text_action_exit) {
                    finish()
                }
                cancelable(false)
            }
        } else if (intent != null) {
            if (intent.action == Intent.ACTION_VIEW) {
                intent.extras?.let {
                    val link = it.getString("link")
                    if (link != null) {
                        browse(link)
                    }
                    finish()
                }
            } else launch()
        } else {
            launch()
        }
    }

    private fun tryLogin() {
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.launch_icon)
                .setTosAndPrivacyPolicyUrls("", getString(R.string.url_privacy_policy))
                .setAvailableProviders(
                    listOf(
                        AuthUI.IdpConfig.EmailBuilder().setRequireName(true).build(),
                        AuthUI.IdpConfig.GoogleBuilder().build()
                    )
                ).build(), RC_LOGIN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                launch()
            } else {
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                tryLogin()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun launch() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    companion object {
        private const val RC_LOGIN: Int = 2
    }
}
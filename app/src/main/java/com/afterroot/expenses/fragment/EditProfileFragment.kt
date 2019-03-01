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
import androidx.fragment.app.Fragment
import com.afterroot.expenses.R
import com.afterroot.expenses.model.User
import com.afterroot.expenses.utils.*
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import org.jetbrains.anko.design.snackbar


/**
 * Created by Sandip on 13-12-2017.
 */
class EditProfileFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    private lateinit var firebaseUser: FirebaseUser
    private val db = Database.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (FirebaseUtils.isUserSignedIn) {
            firebaseUser = FirebaseUtils.auth?.currentUser!!
            activity!!.progress.visible(true)
            Database.getUserByID(firebaseUser.uid, object : Callbacks<User> {
                override fun onSuccess(value: User) {
                    input_phone?.setText(value.phone)
                    input_profile_name.setText(firebaseUser.displayName)
                    input_email.setText(firebaseUser.email)
                    input_email.isEnabled = false
                    activity!!.progress.visible(false)
                }

                override fun onFailed(message: String) {
                }

                override fun onSnapshot(snapshot: DocumentSnapshot) {
                }

            })
            activity!!.fab.apply {
                setOnClickListener {
                    activity!!.progress.visible(true)
                    val phoneText = this@EditProfileFragment.input_phone.text.toString()
                    val phone: String = Utils.formatPhone(activity!!, phoneText)
                    val request = UserProfileChangeRequest.Builder()
                            .setDisplayName(this@EditProfileFragment.input_profile_name.text.toString())
                            .build()
                    firebaseUser.updateProfile(request).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            db.collection(DBConstants.USERS)
                                    .document(FirebaseUtils.auth!!.currentUser!!.uid)
                                    .set(User(this@EditProfileFragment.input_profile_name.text.toString(),
                                            if (firebaseUser.email == null) "" else firebaseUser.email!!,
                                            firebaseUser.uid,
                                            phone))
                                    .addOnSuccessListener {
                                        activity!!.apply {
                                            progress.visible(false)
                                            root_layout.snackbar("Profile Updated")
                                        }
                                    }
                        }
                    }
                }
            }
        } else {
            //Not Logged In
        }
    }
}

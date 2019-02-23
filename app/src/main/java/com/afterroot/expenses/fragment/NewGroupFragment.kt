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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MultiAutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.utils.*
import com.android.ex.chips.BaseRecipientAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.fragment_new_group.*
import kotlinx.android.synthetic.main.fragment_new_group.view.*
import org.jetbrains.anko.design.snackbar
import java.util.*

class NewGroupFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val _tag = "NewGroupFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        edit_text_group_members.apply {
            setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        }
        val recAdapter = BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, activity!!)
        recAdapter.isShowMobileOnly = false
        edit_text_group_members.setAdapter(recAdapter)

        activity!!.fab.apply {
            setImageDrawable(activity!!.getDrawableExt(R.drawable.ic_save))
            setOnClickListener {
                if (verifyData()) {
                    val selected = edit_text_group_members.sortedRecipients
                    val userMAp = HashMap<String?, Int>()
                    activity!!.progress.visibility = View.VISIBLE
                    Log.d(_tag, "onActivityResult: Adding contacts to Map")
                    var size = selected.size
                    for (item in selected) {
                        Log.d(_tag, "onActivityResult: Query Started for ${item.entry.destination}")
                        val value = Utils.formatPhone(activity!!, item.entry.destination)

                        db.collection(DBConstants.USERS)
                                .whereEqualTo(DBConstants.FIELD_PHONE, value)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (querySnapshot.documents.isNotEmpty()) {
                                        val id = querySnapshot.documents[0].id
                                        Log.d(_tag, "getUID: Query Successful for $value, with id $id")
                                        userMAp[id] = DBConstants.TYPE_MEMBER
                                        size--
                                        if (size == 0) {
                                            userMAp[FirebaseUtils.firebaseUser!!.uid] = DBConstants.TYPE_ADMIN
                                            activity!!.progress.visibility = View.VISIBLE
                                            Log.d(_tag, "onCreateOptionsMenu: Creating Group")
                                            val group = Group(edit_text_group_name.text.toString(), Date(), userMAp)
                                            db.collection(DBConstants.GROUPS).add(group).addOnSuccessListener {
                                                activity!!.apply {
                                                    root_layout.snackbar("Group Created")
                                                    progress.visible(false)
                                                    host_nav_fragment.findNavController().navigateUp()
                                                }
                                            }.addOnFailureListener {
                                                activity!!.apply {
                                                    root_layout.snackbar("Group not created.")
                                                    progress.visible(false)
                                                }
                                            }
                                        }
                                    } else {
                                        Log.d(_tag, "getUID: User Not Available")
                                        activity!!.apply {
                                            root_layout.snackbar("${item.entry.destination} is not available")
                                            progress.visible(false)
                                        }
                                    }
                                }.addOnFailureListener { Log.d(_tag, "getUID: Query Failed") }
                    }
                }
            }
        }
    }

    private fun verifyData(): Boolean {
        return when {
            edit_text_group_name.text!!.isEmpty() -> {
                edit_text_group_name.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        ti_layout_group_name.isErrorEnabled = false
                    }

                })
                ti_layout_group_name.error = "Group name can not be empty"
                false
            }
            edit_text_group_members.sortedRecipients.isEmpty() -> {
                edit_text_group_members.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        ti_layout_group_members.isErrorEnabled = false
                    }

                })
                ti_layout_group_members.error = "Group memebers can not be empty"
                false
            }
            else -> true
        }
    }
}
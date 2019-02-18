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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MultiAutoCompleteTextView
import androidx.fragment.app.Fragment
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.FirebaseUtils
import com.afterroot.expenses.utils.Utils
import com.afterroot.expenses.utils.getDrawableExt
import com.android.ex.chips.BaseRecipientAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.fragment_new_group.*
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
                val selected = edit_text_group_members.sortedRecipients
                if (selected.isNotEmpty()) {
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
                                                activity!!.root_layout.snackbar("Group Created")
                                                activity!!.progress.visibility = View.INVISIBLE
                                                activity!!.supportFragmentManager.popBackStack()
                                            }.addOnFailureListener {
                                                activity!!.root_layout.snackbar("Group not created.")
                                                activity!!.progress.visibility = View.INVISIBLE
                                            }
                                        }
                                    } else {
                                        Log.d(_tag, "getUID: User Not Available")
                                        activity!!.root_layout.snackbar("${item.entry.destination} is not available")
                                        progress.visibility = View.INVISIBLE
                                    }
                                }.addOnFailureListener { Log.d(_tag, "getUID: Query Failed") }
                    }
                }
            }
        }
    }
}
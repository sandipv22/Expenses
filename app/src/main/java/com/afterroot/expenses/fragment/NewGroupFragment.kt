package com.afterroot.expenses.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import android.widget.MultiAutoCompleteTextView
import android.widget.ProgressBar
import com.afterroot.expenses.R
import com.afterroot.expenses.R.id.input_group_name
import com.afterroot.expenses.R.id.text_input_group_members
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.ui.MainActivity
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.FirebaseUtils
import com.android.ex.chips.BaseRecipientAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_new_group.*
import org.jetbrains.anko.design.snackbar
import java.util.*

class NewGroupFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "NewGroupFragment"
    private lateinit var progress: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity!!.toolbar.title = "New Group"
        MainActivity.run {
            animateArrow(1f)
            resetInfoMessage(activity!!.main_info_message)
        }

        setHasOptionsMenu(true)

        text_input_group_members.apply {
            setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        }

        val recAdapter = BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, activity!!)
        text_input_group_members.setAdapter(recAdapter)

        progress = activity!!.progress
    }

    private var saveItem: MenuItem? = null
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        saveItem = menu!!.add("Create")
        with(saveItem!!) {
            setIcon(R.drawable.ic_done_black_24dp)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener {
                val selected = text_input_group_members.sortedRecipients
                if (selected.isNotEmpty()) {
                    val userMAp = HashMap<String?, Int>()
                    progress.visibility = View.VISIBLE
                    Log.d(TAG, "onActivityResult: Adding contacts to Map")
                    var size = selected.size
                    for (item in selected) {
                        Log.d(TAG, "onActivityResult: Query Started for ${item.entry.destination}")
                        val value = item.entry.destination
                        db.collection(DBConstants.USERS)
                                .whereEqualTo(if (isEmail(value)) DBConstants.FIELD_EMAIL else DBConstants.FIELD_PHONE, value)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (querySnapshot.documents.isNotEmpty()) {
                                        val id = querySnapshot.documents[0].id
                                        Log.d(TAG, "getUID: Query Successful for $value, with id $id")
                                        userMAp.put(id, DBConstants.TYPE_MEMBER)
                                        size--
                                        if (size == 0) {
                                            userMAp.put(FirebaseUtils.firebaseUser!!.uid, DBConstants.TYPE_ADMIN)
                                            progress.visibility = View.VISIBLE
                                            Log.d(TAG, "onCreateOptionsMenu: Creating Group")
                                            val group = Group(input_group_name.text.toString(), Date(), userMAp)
                                            db.collection(DBConstants.GROUPS).add(group).addOnSuccessListener { documentReference ->
                                                snackbar(activity!!.main_content, "Group Created")
                                                progress.visibility = View.INVISIBLE
                                                activity!!.supportFragmentManager.popBackStack()
                                            }.addOnFailureListener {
                                                snackbar(activity!!.main_content, "Group not created.")
                                                progress.visibility = View.INVISIBLE
                                            }
                                        }
                                    } else {
                                        Log.d(TAG, "getUID: User Not Available")
                                        snackbar(activity!!.main_content, "${item.entry.destination} is not available")
                                        progress.visibility = View.INVISIBLE
                                    }
                                }.addOnFailureListener { Log.d(TAG, "getUID: Query Failed") }
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun isEmail(value: String): Boolean {
        Log.d(TAG, "isEmail: ${value.contains("@")}")
        return value.contains("@")
    }


    override fun onDetach() {
        super.onDetach()
        MainActivity.animateArrow(0f)
    }
}
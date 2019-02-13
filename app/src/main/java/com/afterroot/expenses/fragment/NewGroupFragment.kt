package com.afterroot.expenses.fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.MultiAutoCompleteTextView
import androidx.fragment.app.Fragment
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.FirebaseUtils
import com.afterroot.expenses.utils.Utils
import com.android.ex.chips.BaseRecipientAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_new_group.*
import org.jetbrains.anko.design.snackbar
import java.util.*

class NewGroupFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "NewGroupFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        text_input_group_members.apply {
            setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        }
        val recAdapter = BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, activity!!)
        recAdapter.isShowMobileOnly = false
        text_input_group_members.setAdapter(recAdapter)
    }

    private var saveItem: MenuItem? = null
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        saveItem = menu!!.add("Create")
        with(saveItem!!) {
            setIcon(R.drawable.ic_done)
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
                        val value = Utils.formatPhone(activity!!, item.entry.destination)

                        db.collection(DBConstants.USERS)
                                .whereEqualTo(DBConstants.FIELD_PHONE, value)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (querySnapshot.documents.isNotEmpty()) {
                                        val id = querySnapshot.documents[0].id
                                        Log.d(TAG, "getUID: Query Successful for $value, with id $id")
                                        userMAp[id] = DBConstants.TYPE_MEMBER
                                        size--
                                        if (size == 0) {
                                            userMAp[FirebaseUtils.firebaseUser!!.uid] = DBConstants.TYPE_ADMIN
                                            progress.visibility = View.VISIBLE
                                            Log.d(TAG, "onCreateOptionsMenu: Creating Group")
                                            val group = Group(input_group_name.text.toString(), Date(), userMAp)
                                            db.collection(DBConstants.GROUPS).add(group).addOnSuccessListener {
                                                activity!!.root_layout.snackbar("Group Created")
                                                progress.visibility = View.INVISIBLE
                                                activity!!.supportFragmentManager.popBackStack()
                                            }.addOnFailureListener {
                                                activity!!.root_layout.snackbar("Group not created.")
                                                progress.visibility = View.INVISIBLE
                                            }
                                        }
                                    } else {
                                        Log.d(TAG, "getUID: User Not Available")
                                        activity!!.root_layout.snackbar("${item.entry.destination} is not available")
                                        progress.visibility = View.INVISIBLE
                                    }
                                }.addOnFailureListener { Log.d(TAG, "getUID: Query Failed") }
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }


}
package com.afterroot.expenses.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.ui.MainActivity
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.DBConstants.GROUPS
import com.afterroot.expenses.utils.FirebaseUtils
import com.afterroot.expenses.utils.ListClickCallbacks
import com.afterroot.expenses.utils.Utils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_expense_list.view.*
import kotlinx.android.synthetic.main.list_item_group.view.*
import java.util.*

class GroupsFragment : Fragment() {

    private var firestoreAdapter: FirestoreRecyclerAdapter<Group, ViewHolder>? = null
    private var db: FirebaseFirestore? = null
    private val TAG = "GroupsFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()
        initFirebaseDb(view)
    }

    lateinit var loading : MaterialDialog
    override fun onStart() {
        super.onStart()
        firestoreAdapter!!.startListening()
        loading = MaterialDialog.Builder(activity!!).content("Loading...").progress(true, 1).build()
        loading.show()
        activity!!.toolbar.title = "Groups"
        Log.d(TAG, "onStart: Started Listening")
        activity!!.fab.hide()
    }

    override fun onStop() {
        super.onStop()
        firestoreAdapter!!.stopListening()
        Log.d(TAG, "onStart: Stopped Listening")
    }

    override fun onDetach() {
        super.onDetach()
        menu.removeItem(addGroupItem!!.itemId)
    }

    private var addGroupItem: MenuItem? = null
    lateinit var menu: Menu
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        this.menu = menu!!
        Log.d(TAG, "onCreateOptionsMenu: Menu Create")
        addGroupItem = menu.add(getString(R.string.menu_new_group))
        with(addGroupItem!!) {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setIcon(R.drawable.ic_add_black_24dp)
        }
    }

    var callbacks: ListClickCallbacks<Group>? = null
    private fun initFirebaseDb(view: View) {
        val query = db!!.collection(GROUPS)
                .whereGreaterThanOrEqualTo("${DBConstants.FIELD_GROUP_MEMBERS}.${FirebaseUtils.firebaseUser!!.uid}", DBConstants.TYPE_MEMBER)
        val options = FirestoreRecyclerOptions.Builder<Group>()
                .setQuery(query, Group::class.java)
                .build()

        firestoreAdapter = object : FirestoreRecyclerAdapter<Group, ViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
                val holderView = LayoutInflater.from(parent!!.context).inflate(R.layout.list_item_group, parent, false)
                return ViewHolder(holderView)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Group) {
                holder.apply {
                    itemName.text = model.group_name
                    itemDate.text = Utils.getDateDiff(model.date!!, Calendar.getInstance().time)
                }

                with(holder.itemView) {
                    tag = model
                    setOnClickListener {
                        callbacks!!.onListItemClick(tag as Group)
                    }
                    setOnLongClickListener {
                        callbacks!!.onListItemLongClick(tag as Group)
                        return@setOnLongClickListener true
                    }
                }
            }

            override fun onDataChanged() {
                Toast.makeText(activity, "Data Changed", Toast.LENGTH_SHORT).show()
                if (itemCount == 0) {
                    MainActivity.setInfoMessage(activity!!.main_info_message, activity!!.resources.getString(R.string.no_groups))
                } else {
                    MainActivity.resetInfoMessage(activity!!.main_info_message)
                }
                loading.dismiss()
            }

            override fun onError(e: FirebaseFirestoreException) {
                Toast.makeText(activity!!, e.message, Toast.LENGTH_SHORT).show()
            }
        }

        view.list.apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = firestoreAdapter
            adapter.notifyDataSetChanged()
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: AppCompatTextView = view.item_name
        val itemEmail: AppCompatTextView = view.item_email
        val itemDate: AppCompatTextView = view.item_time
    }

    companion object {
        fun with(listClickCallbacks: ListClickCallbacks<Group>) = GroupsFragment().apply {
            callbacks = listClickCallbacks
        }
    }
}
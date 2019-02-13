package com.afterroot.expenses.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.navigation.fragment.navArgs
import com.afterroot.expenses.R
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.ListClickCallbacks
import com.afterroot.expenses.utils.Utils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_expense_list.view.*
import kotlinx.android.synthetic.main.list_item_expense.view.*

class ExpenseListFragment : androidx.fragment.app.Fragment() {
    private var listener: ListClickCallbacks<ExpenseItem>? = null
    private var firestoreAdapter: FirestoreRecyclerAdapter<ExpenseItem, ExpenseViewHolder>? = null
    private var db: FirebaseFirestore? = null
    lateinit var groupDocID: String
    val _tag = "ExpenseListFragment"
    val args: ExpenseListFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_expense_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        groupDocID = args.docId
        db = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        }
        initFirebaseDb(view)
        //activity!!.fab.show()
        //activity!!.toolbar.title = arguments!!.getString("GROUP_NAME")
    }

    private fun initFirebaseDb(view: View) {
        val query = db!!.collection(DBConstants.GROUPS)
                .document(groupDocID)
                .collection(DBConstants.EXPENSES).orderBy("date", Query.Direction.DESCENDING)

        Log.d(_tag, "initFirebaseDb: $query")

        val options = FirestoreRecyclerOptions.Builder<ExpenseItem>()
                .setQuery(query, ExpenseItem::class.java)
                .setLifecycleOwner(this)
                .build()

        firestoreAdapter = object : FirestoreRecyclerAdapter<ExpenseItem, ExpenseViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
                val holderView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_expense, parent, false)
                return ExpenseViewHolder(holderView)
            }

            override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int, model: ExpenseItem) {
                holder.amountText.text = String.format("%s%d",
                        holder.amountText.context.resources.getString(R.string.rs_symbol), model.amount)
                holder.noteText.text = String.format("%s, with %s", model.note, Utils.formatNames(model.with!!))
                holder.categoryText.text = model.category

                with(holder.itemView) {
                    tag = model
                    val id = snapshots.getSnapshot(holder.adapterPosition).id
                    setOnClickListener {
                        listener!!.onListItemClick(tag as ExpenseItem, id)
                    }

                    setOnLongClickListener {
                        listener!!.onListItemLongClick(tag as ExpenseItem, id)
                        return@setOnLongClickListener true
                    }
                }
            }

            override fun onDataChanged() {
                if (itemCount == 0) {
                    //MainActivity.setInfoMessage(activity!!.main_info_message, activity!!.resources.getString(R.string.no_expenses))
                    Toast.makeText(activity!!, getString(R.string.no_expenses), Toast.LENGTH_SHORT).show()
                } else {
                    //MainActivity.resetInfoMessage(activity!!.main_info_message)
                }
            }

            override fun onError(e: FirebaseFirestoreException) {
                Toast.makeText(activity!!, e.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        view.list.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(view.context)
            adapter = firestoreAdapter
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null

    }

    companion object {
        @JvmStatic
        fun with(listItemClickCallbacks: ListClickCallbacks<ExpenseItem>) = ExpenseListFragment().apply {
            listener = listItemClickCallbacks
        }
    }

    inner class ExpenseViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val amountText: AppCompatTextView = view.item_amount
        val noteText: AppCompatTextView = view.item_note
        val categoryText: AppCompatTextView = view.item_category
    }
}

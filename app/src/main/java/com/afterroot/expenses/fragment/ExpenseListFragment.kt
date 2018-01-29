package com.afterroot.expenses.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.afterroot.expenses.R
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.ui.MainActivity
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.ListClickCallbacks
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_expense_list.view.*
import kotlinx.android.synthetic.main.list_item_expense.view.*

class ExpenseListFragment : Fragment() {
    private var listener: ListClickCallbacks<ExpenseItem>? = null
    private var firestoreAdapter: FirestoreRecyclerAdapter<ExpenseItem, ExpenseViewHolder>? = null
    private var db: FirebaseFirestore? = null
    lateinit var groupDocID: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_expense_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        MainActivity.animateArrow(1f)
        db = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        }
        initFirebaseDb(view)
        activity!!.fab.show()
    }

    private fun initFirebaseDb(view: View) {
        val query = db!!.collection(DBConstants.GROUPS)
                .document(groupDocID)
                .collection(DBConstants.EXPENSES)

        Log.d(this::class.java.simpleName, "initFirebaseDb: $query")

        val options = FirestoreRecyclerOptions.Builder<ExpenseItem>()
                .setQuery(query, ExpenseItem::class.java)
                .build()

        firestoreAdapter = object : FirestoreRecyclerAdapter<ExpenseItem, ExpenseViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ExpenseViewHolder {
                val holderView = LayoutInflater.from(parent!!.context).inflate(R.layout.list_item_expense, parent, false)
                return ExpenseViewHolder(holderView)
            }

            override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int, model: ExpenseItem) {
                holder.amountText.text = String.format("%s%d",
                        holder.amountText.context.resources.getString(R.string.rs_symbol), model.amount)
                holder.noteText.text = model.note
                holder.categoryText.text = model.category

                with(holder.itemView) {
                    tag = model
                    setOnClickListener {
                        listener!!.onListItemClick(tag as ExpenseItem)
                    }

                    setOnLongClickListener {
                        listener!!.onListItemLongClick(tag as ExpenseItem)
                        return@setOnLongClickListener true
                    }
                }
            }

            override fun onDataChanged() {
                Toast.makeText(activity, "Data Changed", Toast.LENGTH_SHORT).show()
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

    override fun onDetach() {
        super.onDetach()
        listener = null

    }

    override fun onStart() {
        super.onStart()
        firestoreAdapter!!.startListening()
    }

    override fun onStop() {
        super.onStop()
        firestoreAdapter!!.stopListening()
        MainActivity.animateArrow(0f)
    }

    companion object {
        @JvmStatic
        fun with(docId: String, listItemClickCallbacks: ListClickCallbacks<ExpenseItem>) = ExpenseListFragment().apply {
            listener = listItemClickCallbacks
            groupDocID = docId
        }
    }

    inner class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val amountText: AppCompatTextView = view.item_amount
        val noteText: AppCompatTextView = view.item_note
        val categoryText: AppCompatTextView = view.item_category
    }
}

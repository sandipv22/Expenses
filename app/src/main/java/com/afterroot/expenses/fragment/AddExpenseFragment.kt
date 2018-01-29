package com.afterroot.expenses.fragment

import android.app.DatePickerDialog
import android.app.ProgressDialog.show
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.CategoryItemListDialogFragment
import com.afterroot.expenses.R
import com.afterroot.expenses.R.id.fab_add_transaction
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.model.User
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.FirebaseUtils
import com.afterroot.expenses.utils.FirebaseUtils.getByID
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.fragment_add_expense.*
import kotlinx.android.synthetic.main.fragment_add_expense.view.*
import kotlinx.android.synthetic.main.fragment_categoryitem_list.view.*
import kotlinx.android.synthetic.main.list_item_user.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Sandip on 05-12-2017.
 */
class AddExpenseFragment : Fragment(), DatePickerDialog.OnDateSetListener {

    private var fragmentView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_add_expense, container, false)

        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
    }

    private var year: Int? = 0
    private var month: Int? = 0
    private var day: Int? = 0
    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        year = cal.get(Calendar.YEAR)
        month = cal.get(Calendar.MONTH)
        day = cal.get(Calendar.DAY_OF_MONTH)
        val datePicker = DatePickerDialog(activity, this, year!!, month!!, day!!)
        datePicker.show()
    }

    private var db: FirebaseFirestore? = null
    var item: ExpenseItem? = null
    private var pickedDate: GregorianCalendar? = null
    lateinit var groupID: String

    private fun init(view: View) {
        fragmentView!!.apply {
            text_input_date.setOnClickListener {
                showDatePicker()
            }
            text_input_category.setOnClickListener {
                CategoryItemListDialogFragment.newInstance(30).show(activity!!.supportFragmentManager, "category")
            }
            text_paid_by.apply {
                setOnClickListener {
                    val progress = MaterialDialog.Builder(activity!!).progress(true, 1).content("Loading...").show()
                    val ref = db!!.collection(DBConstants.GROUPS).document(groupID)
                    getByID(ref, object : FirebaseUtils.Callbacks<Group> {
                        override fun onSuccess(value: Group) {
                            val usersList = ArrayList<User>()
                            val namesList = ArrayList<String>()
                            var i = value.members!!.size
                            for (item in value.members!!) {
                                getByID(db!!.collection(DBConstants.USERS).document(item.key!!), object : FirebaseUtils.Callbacks<User> {
                                    override fun onSuccess(user: User) {
                                        i--
                                        Log.d("TESTAG", "onSuccess: $user pos : $i")
                                        usersList.add(user)
                                        namesList.add(user.name)
                                        if (i == 0) {
                                            progress.dismiss()
                                            MaterialDialog.Builder(activity!!).items(namesList).itemsCallback { dialog, itemView, position, text ->
                                                Log.d("TESTAG", "onSuccess: $position $text ${usersList[position].name} ${usersList[position].uid}")

                                            }.show()
                                        }
                                    }

                                    override fun onFailed(message: String) {

                                    }

                                })
                            }
                        }

                        override fun onFailed(message: String) {
                        }

                    })
                }
            }
            text_spenders.apply {
                setOnClickListener {

                }
            }
        }
        db = FirebaseFirestore.getInstance()
        db!!.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        Handler().postDelayed({
            fab_add_transaction.apply {
                show()
                setOnClickListener {
                    if (verifyData(view)) {
                        /*val list = text_input_spenders.sortedRecipients
                        val finalList: ArrayList<String> = ArrayList()
                        list.mapTo(finalList) { it.toString() }
                        item = ExpenseItem(view.text_input_amount.text.toString().toLong(),
                                "Test",
                                Date(millis),
                                view.text_input_note.text.toString(), view.text_input_paid_by.text.toString(),
                                finalList
                        )
                        db!!.collection(DBConstants.GROUPS)
                                .document(groupID)
                                .collection(DBConstants.EXPENSES).add(item!!).addOnSuccessListener { documentReference ->
                            *//*activity!!.supportFragmentManager.popBackStack()*//*
                            activity!!.onBackPressed()
                        }*/
                    }

                }
            }
        }, 200)

    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName = view.item_name
        val itemEmail = view.item_email
    }

    private fun verifyData(view: View): Boolean {
        if (view.text_input_amount.text.isEmpty()) {
            view.text_input_amount.error = "Please enter amount"
            return false
        }
        return true
    }

    private var millis: Long = 0
    override fun onDateSet(p0: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        pickedDate = GregorianCalendar(year, monthOfYear, dayOfMonth)
        millis = pickedDate!!.timeInMillis

        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
        fragmentView!!.text_input_date.text = formatter.format(Date(millis))
    }

    companion object {
        fun newInstance(id: String) = AddExpenseFragment().apply {
            groupID = id
        }
    }
}
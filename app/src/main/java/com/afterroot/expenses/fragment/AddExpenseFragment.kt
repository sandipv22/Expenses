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

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.ChangeTransform
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionSet
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.afterroot.expenses.Constants
import com.afterroot.expenses.R
import com.afterroot.expenses.database.DBConstants
import com.afterroot.expenses.database.Database
import com.afterroot.expenses.firebase.FirebaseUtils
import com.afterroot.expenses.firebase.QueryCallback
import com.afterroot.expenses.getDrawableExt
import com.afterroot.expenses.model.Category
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.User
import com.afterroot.expenses.ui.ListClickCallbacks
import com.afterroot.expenses.visible
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.fragment_add_expense.*
import kotlinx.android.synthetic.main.fragment_add_expense.view.*
import org.jetbrains.anko.design.snackbar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

/**
 * Created by Sandip on 05-12-2017.
 */
class AddExpenseFragment : Fragment(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private lateinit var cal: Calendar
    private lateinit var category: String
    private lateinit var groupID: String
    private lateinit var paidByID: String
    private val _tag = "AddExpenseFragment"
    private val args: AddExpenseFragmentArgs by navArgs()
    private val names = ArrayList<String>()
    private val users = ArrayList<User>()
    private val withUserMap = HashMap<String, User>() //Map of Members for 'with' parameter
    private var day: Int? = 0
    private var db: FirebaseFirestore = Database.getInstance()
    private var expenseDocNo: String? = null
    private var hourOfDay: Int? = 0
    private var isAllUsersAdded: Boolean = false
    private var item: ExpenseItem? = null
    private var millis: Long = 0
    private var minute: Int? = 0
    private var month: Int? = 0
    private var pickedDate: GregorianCalendar? = null
    private var referenceMap = HashMap<String, User>() //Map for Reference
    private var usersMap = HashMap<String, User>() //Map of Group members. Not Changed.
    private var year: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = getString(R.string.text_uncategorized)
        expenseDocNo = args.expenseDocNo
        groupID = args.groupDocId
        paidByID = ""
        item = arguments?.getSerializable(Constants.KEY_EXPENSE_SERIALIZE) as ExpenseItem?
        if (item != null) {
            activity!!.toolbar.title = "Edit Expense"
        }
        Database.getGroupMembers(groupID, object : QueryCallback<HashMap<String, User>> {
            override fun onSuccess(value: HashMap<String, User>) {
                mapUserValues(value)
            }

            override fun onFailed(message: String) {
            }

            override fun onSnapshot(snapshot: DocumentSnapshot) {
            }

        })
        val transitionSet = TransitionSet().addTransition(Slide()).addTransition(Fade()).addTransition(ChangeTransform())
        transitionSet.ordering = TransitionSet.ORDERING_TOGETHER
        transitionSet.duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        transitionSet.interpolator = LinearOutSlowInInterpolator()
        enterTransition = transitionSet
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    override fun onStart() {
        super.onStart()
        init()
    }

    fun mapUserValues(value: HashMap<String, User>) {
        try {
            names.clear()
            users.clear()
            view!!.spenders_chip_group.removeAllViews()
            isAllUsersAdded = true
            referenceMap = value
            usersMap = value
            usersMap.values.map {
                names.add(it.name)
                users.add(it)
            }

            users.forEach {
                val chip = Chip(context)
                chip.apply {
                    this.text = it.name
                    tag = it.uid
                    this.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            withUserMap[buttonView.tag.toString()] = usersMap[buttonView.tag]!!
                        } else {
                            withUserMap.remove(buttonView.tag)
                        }
                    }
                }
                view!!.spenders_chip_group.addView(chip)
            }

        } catch (e: Exception) {

        }
    }

    private fun init() {
        if (item != null) { //Edit Mode
            activity!!.progress.visible(true)
            Database.getUserByID(item!!.paidBy!!, object : QueryCallback<User> {
                override fun onSuccess(value: User) {
                    category = item!!.category
                    paidByID = item!!.paidBy!!
                    withUserMap.values.forEach {
                        item!!.with!![it.uid] = it.name
                    }
                    view.apply {
                        text_input_amount.setText(item!!.amount.toString())
                        text_input_category.text = category
                        text_input_date.text = SimpleDateFormat(getString(R.string.date_time_format), Locale.US).format(item!!.date)
                        text_input_note.setText(item!!.note)
                        text_paid_by.text = value.name
                    }
                    activity!!.progress.visible(false)
                }

                override fun onFailed(message: String) {
                }

                override fun onSnapshot(snapshot: DocumentSnapshot) {
                }

            })

        }

        view!!.text_input_date.setOnClickListener {
            showDatePicker()
        }
        view!!.text_input_category.setOnClickListener {
            CategoryItemListDialogFragment.with(groupID, object : ListClickCallbacks<Category> {
                override fun onListItemClick(item: Category?, docId: String, position: Int, view: View?) {
                    category = item!!.name
                    text_input_category.text = item.name
                }

                override fun onListItemLongClick(item: Category?, docId: String, position: Int) {

                }

            }).show(activity!!.supportFragmentManager, "category")
        }
        view!!.text_paid_by.setOnClickListener {
            if (isAllUsersAdded) {
                MaterialDialog(activity!!).show {
                    title(R.string.paid_by)
                    listItems(items = names) { _, position, _ ->
                        paidByID = users[position].uid
                        view!!.text_paid_by.text = users[position].name
                    }
                }
            } else {
                activity!!.root_layout.snackbar("Loading...")
                Database.getGroupMembers(groupID, object : QueryCallback<HashMap<String, User>> {
                    override fun onSuccess(value: HashMap<String, User>) {
                        mapUserValues(value)
                        MaterialDialog(activity!!).show {
                            title(R.string.paid_by)
                            listItems(items = names) { _, position, _ ->
                                paidByID = users[position].uid
                                view!!.text_paid_by.text = users[position].name
                            }
                        }
                    }

                    override fun onFailed(message: String) {
                    }

                    override fun onSnapshot(snapshot: DocumentSnapshot) {
                    }

                })
            }

        }

        activity!!.fab.apply {
            if (item != null) {
                setImageDrawable(activity!!.getDrawableExt(R.drawable.ic_save, R.color.onSecondary))
            }
            setOnClickListener {
                if (verifyData()) {
                    val finalMap: HashMap<String, String>? = HashMap()
                    withUserMap.values.forEach { user ->
                        finalMap!![user.uid] = user.name
                    }

                    val groupRef = db.collection(DBConstants.GROUPS).document(groupID)
                    val reference = groupRef.collection(DBConstants.EXPENSES)
                    if (item != null) {
                        item = ExpenseItem(view!!.text_input_amount.text.toString().toLong(),
                                category,
                                item!!.date,
                                view!!.text_input_note.text.toString(), paidByID,
                                finalMap,
                                hashMapOf(FirebaseUtils.auth!!.uid!! to FirebaseUtils.auth!!.currentUser!!.displayName!!),
                                hashMapOf(paidByID to usersMap[paidByID]!!.name)
                        )
                        reference.document(expenseDocNo!!).set(item!!).addOnSuccessListener {
                            val batch = Database.getInstance().batch()
                            val map = hashMapOf("lastEntry" to Date(millis),
                                    "lastEntryText" to "Edited: ${usersMap[paidByID]!!.name} : ${view!!.text_input_note.text.toString()}"
                            )
                            batch.update(groupRef, map)
                            batch.commit()
                            this@AddExpenseFragment.view!!.findNavController().popBackStack()
                        }
                    } else {
                        item = ExpenseItem(view!!.text_input_amount.text.toString().toLong(),
                                category,
                                Date(millis),
                                view!!.text_input_note.text.toString(), paidByID,
                                finalMap,
                                hashMapOf(FirebaseUtils.auth!!.uid!! to FirebaseUtils.auth!!.currentUser!!.displayName!!),
                                hashMapOf(paidByID to referenceMap[paidByID]!!.name)
                        )
                        reference.add(item!!).addOnCompleteListener {
                            val batch = Database.getInstance().batch()
                            val map = hashMapOf("lastEntry" to Date(millis),
                                    "lastEntryText" to "${usersMap[paidByID]!!.name} : ${view!!.text_input_note.text.toString()}"
                            )
                            batch.update(groupRef, map)
                            batch.commit()
                            this@AddExpenseFragment.view!!.findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    private fun verifyData(): Boolean {
        when {
            text_input_amount.text!!.isEmpty() -> {
                text_input_amount.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        text_input_layout_amount.isErrorEnabled = false
                    }

                })
                text_input_layout_amount.error = "Please enter amount"
                return false
            }
            text_input_note.text!!.isEmpty() -> {
                text_input_note.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {

                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        text_input_layout_note.isErrorEnabled = false
                    }

                })
                text_input_layout_note.error = "Please enter note"
                return false
            }
            paidByID.isBlank() -> {
                activity!!.root_layout.snackbar("Please select Paid by")
                return false
            }
            millis == 0L -> {
                activity!!.root_layout.snackbar("Please select Date")
                return false
            }
            else -> return true
        }
    }

    private fun showDatePicker() {
        cal = Calendar.getInstance()
        if (item != null) {
            cal.timeInMillis = item!!.date!!.time
        }
        day = cal.get(Calendar.DAY_OF_MONTH)
        hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
        minute = cal.get(Calendar.MINUTE)
        month = cal.get(Calendar.MONTH)
        year = cal.get(Calendar.YEAR)
        val datePicker = DatePickerDialog(context!!, this, year!!, month!!, day!!)
        datePicker.show()
    }

    private fun showTimePicker() {
        val picker = TimePickerDialog(this.context, this, hourOfDay!!, minute!!, false)
        picker.show()
    }

    override fun onDateSet(p0: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        this.year = year
        this.month = monthOfYear
        this.day = dayOfMonth
        showTimePicker()
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        pickedDate = GregorianCalendar(year!!, month!!, day!!, hourOfDay, minute)
        millis = pickedDate!!.timeInMillis

        val formatter = SimpleDateFormat(getString(R.string.date_time_format), Locale.US)
        this.view!!.text_input_date.text = formatter.format(Date(millis))
    }

}
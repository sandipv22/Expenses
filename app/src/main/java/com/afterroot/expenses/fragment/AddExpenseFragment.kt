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
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Category
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.model.Group
import com.afterroot.expenses.model.User
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.FirebaseUtils
import com.afterroot.expenses.utils.FirebaseUtils.getByID
import com.afterroot.expenses.utils.ListClickCallbacks
import com.afterroot.expenses.utils.getDrawableExt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_add_expense.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Sandip on 05-12-2017.
 */
class AddExpenseFragment : Fragment(), DatePickerDialog.OnDateSetListener {

    private var fragmentView: View? = null
    val _tag = "AddExpenseFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_add_expense, container, false)
        return fragmentView
    }

    override fun onStart() {
        super.onStart()
        init(fragmentView!!)
    }

    private var year: Int? = 0
    private var month: Int? = 0
    private var day: Int? = 0
    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        year = cal.get(Calendar.YEAR)
        month = cal.get(Calendar.MONTH)
        day = cal.get(Calendar.DAY_OF_MONTH)
        val datePicker = DatePickerDialog(fragmentView!!.context, this, year!!, month!!, day!!)
        datePicker.show()
    }

    private var db: FirebaseFirestore? = null
    var item: ExpenseItem? = null
    private var pickedDate: GregorianCalendar? = null
    lateinit var groupID: String

    val usersMap = HashMap<String, User>()
    private val withUserMap = HashMap<String, User>()
    private var paidByID: String = ""
    private var paidByName: String = ""
    private lateinit var category: String
    private var millis: Long = 0
    private val args: AddExpenseFragmentArgs by navArgs()

    private fun init(view: View) {
        groupID = args.groupDocId
        Log.d(_tag, "init: $groupID")
        category = getString(R.string.text_uncategorized)
        db = FirebaseFirestore.getInstance()
        db!!.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        getGroupUsers(null)
        fragmentView!!.apply {
            text_input_date.setOnClickListener {
                showDatePicker()
            }
            text_input_category.setOnClickListener {
                CategoryItemListDialogFragment.with(groupID, object : ListClickCallbacks<Category> {
                    override fun onListItemClick(item: Category?, docId: String) {
                        category = item!!.name
                        text_input_category.text = item.name
                    }

                    override fun onListItemLongClick(item: Category?, docId: String) {

                    }

                }).show(activity!!.supportFragmentManager, "category")
            }
            text_paid_by.apply {
                setOnClickListener {
                    if (isAllUsersAdded) {
                        MaterialDialog.Builder(activity!!).items(usersMap.keys).itemsCallback { _, _, position, text ->
                            Log.d(_tag, "onSuccess: Direct Load $position $text ${usersMap[text]!!.name} ${usersMap[text]!!.uid}")
                            paidByID = usersMap[text]!!.uid
                            paidByName = text as String
                            view.text_paid_by.text = paidByName
                        }.show()
                    } else {
                        val progress = MaterialDialog.Builder(activity!!).progress(true, 1).content("Loading...").show()
                        getGroupUsers(object : FirebaseUtils.Callbacks<HashMap<String, User>> {
                            override fun onSuccess(value: HashMap<String, User>) {
                                progress.dismiss()
                                MaterialDialog.Builder(activity!!).items(value.keys).itemsCallback { _, _, position, text ->
                                    Log.d(_tag, "onSuccess: Wait Load $position $text ${value[text]!!.name} ${value[text]!!.uid}")
                                    paidByID = value[text]!!.uid
                                    paidByName = text as String
                                    view.text_paid_by.text = paidByName
                                }.show()
                            }

                            override fun onFailed(message: String) {
                            }

                        })
                    }

                }
            }
            text_spenders.apply {
                setOnClickListener {
                    if (isAllUsersAdded) {
                        //TODO add method to remove selected payer
                        //referenceMap.remove(paidByName)
                        val builder = StringBuilder()
                        MaterialDialog.Builder(activity!!).items(referenceMap.keys)
                                .itemsCallbackMultiChoice(null) { dialog: MaterialDialog, which: Array<Int>, text: Array<CharSequence> ->
                                    var i = 0
                                    text.forEach {
                                        i++
                                        when (i) {
                                            text.size -> builder.append(it.toString())
                                            text.size - 1 -> builder.append("$it and ")
                                            else -> builder.append("$it, ")
                                        }
                                        Log.d(_tag, "$builder")
                                        text_spenders.text = builder
                                        val user = referenceMap[it.toString()]
                                        withUserMap[user!!.name] = user
                                        Log.d(_tag, "Added to Final Map ${user.name}")
                                    }
                                    return@itemsCallbackMultiChoice true
                                }.positiveText("OK").onPositive { dialog, which ->
                                }.show()
                    } else {
                        val progress = MaterialDialog.Builder(activity!!).progress(true, 1).content("Loading...").show()
                        getGroupUsers(object : FirebaseUtils.Callbacks<HashMap<String, User>> {
                            override fun onSuccess(value: HashMap<String, User>) {
                                progress.dismiss()
                                value.remove(paidByName)
                                MaterialDialog.Builder(activity!!).items(value.keys)
                                        .itemsCallbackMultiChoice(null) { materialDialog: MaterialDialog, ints: Array<Int>, arrayOfCharSequences: Array<CharSequence> ->
                                            Log.d(_tag, "onSuccess: ${ints.size}")
                                            return@itemsCallbackMultiChoice true
                                        }.positiveText("OK").onPositive { dialog, which ->

                                        }.show()
                            }

                            override fun onFailed(message: String) {
                            }

                        })
                    }
                }
            }
        }
        activity!!.fab.apply {
            setImageDrawable(activity!!.getDrawableExt(R.drawable.ic_done))
            show()
            setOnClickListener {
                if (verifyData(view)) {
                    /* val finalList = ArrayList<String>()
                     withUserMap.values.mapTo(finalList) { it.uid }*/
                    val map: HashMap<String, String>? = HashMap()
                    withUserMap.values.forEach {
                        map!![it.uid] = it.name
                    }
                    item = ExpenseItem(view.text_input_amount.text.toString().toLong(),
                            category,
                            Date(millis),
                            view.text_input_note.text.toString(), paidByID,
                            map
                    )
                    db!!.collection(DBConstants.GROUPS)
                            .document(groupID)
                            .collection(DBConstants.EXPENSES).add(item!!).addOnSuccessListener { documentReference ->
                                activity!!.supportFragmentManager.popBackStack()
                            }
                }

            }
        }

    }

    var isAllUsersAdded: Boolean = false
    val referenceMap: HashMap<String, User> = HashMap()

    private fun getGroupUsers(callbacks: FirebaseUtils.Callbacks<HashMap<String, User>>?) {
        val ref = db!!.collection(DBConstants.GROUPS).document(groupID)
        getByID(ref, object : FirebaseUtils.Callbacks<Group> {
            override fun onSuccess(value: Group) {
                var i = value.members!!.size
                for (user in value.members!!) {
                    getByID(db!!.collection(DBConstants.USERS).document(user.key!!), object : FirebaseUtils.Callbacks<User> {
                        override fun onSuccess(value: User) {
                            i--
                            Log.d(_tag, "onSuccess: $value pos : $i")
                            if (!usersMap.containsKey(value.name)) usersMap[value.name] = value
                            if (!referenceMap.containsKey(value.name)) referenceMap[value.name] = value
                            if (i == 0) {
                                isAllUsersAdded = true
                                callbacks?.onSuccess(usersMap)
                            }
                        }

                        override fun onFailed(message: String) {}
                    })
                }
            }

            override fun onFailed(message: String) {}
        })
    }

    private fun verifyData(view: View): Boolean {
        if (view.text_input_amount.text!!.isEmpty()) {
            view.text_input_amount.error = "Please enter amount"
            return false
        }
        return true
    }

    override fun onDateSet(p0: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        pickedDate = GregorianCalendar(year, monthOfYear, dayOfMonth)
        millis = pickedDate!!.timeInMillis

        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
        fragmentView!!.text_input_date.text = formatter.format(Date(millis))
    }
}
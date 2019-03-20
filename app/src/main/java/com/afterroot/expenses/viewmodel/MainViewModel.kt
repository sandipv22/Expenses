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

package com.afterroot.expenses.viewmodel

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.afterroot.expenses.model.User


class MainViewModel(application: Application) : AndroidViewModel(application) {
    var contacts: MutableLiveData<List<User>> = MutableLiveData()

    fun getContacts(): LiveData<List<User>> {
        val contenetResolver = getApplication<Application>().contentResolver
        val cursor = contenetResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null)
        val list = ArrayList<User>()
        if (cursor!!.count > 0) {
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                if (cursor.getInt(cursor.getColumnIndex(
                                ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    val pCur = contenetResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id), null)
                    while (pCur!!.moveToNext()) {
                        val phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER))
                        val email = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Email.ADDRESS
                        ))
                        val user = User(name, email, id, phoneNo)
                        list.add(user)
                    }
                    pCur.close()
                }
            }
        }
        cursor.close()
        contacts.value = list
        return contacts
    }
}

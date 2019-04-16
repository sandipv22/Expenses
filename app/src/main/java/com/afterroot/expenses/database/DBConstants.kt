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

package com.afterroot.expenses.database

object DBConstants {
    //Collections
    const val CATEGORIES = "categories"
    const val EXPENSES = "expenses"
    const val GROUPS = "groups"

    const val CREATED_AT = "createdAt"

    //Fields
    const val FIELD_ADMIN_ID = "admin_id"
    const val FIELD_DATE = "date"
    const val FIELD_EMAIL = "email"
    const val FIELD_FCM_ID = "fcm_id"
    const val FIELD_GROUP_MEMBERS = "members"
    const val FIELD_GROUP_NAME = "group_name"
    const val FIELD_NAME = "name"
    const val FIELD_PAID_BY = "paidBy"
    const val FIELD_PHONE = "phone"
    const val FIELD_UID = "uid"

    //Member Type
    const val TYPE_ADMIN = 1
    const val TYPE_MEMBER = 0

    const val USED_AT = "usedAt"
    const val USERS = "users"
}
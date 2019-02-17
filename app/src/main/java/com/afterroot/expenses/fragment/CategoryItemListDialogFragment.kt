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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.R
import com.afterroot.expenses.model.Category
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.ListClickCallbacks
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.fragment_categories.*
import kotlinx.android.synthetic.main.list_item_category.view.*

class CategoryItemListDialogFragment : BottomSheetDialogFragment() {
    private var fireCategoryAdapter: FirestoreRecyclerAdapter<Category, CategoryViewHolder>? = null
    private var db: FirebaseFirestore? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        }
        initCategories(view)
    }

    private fun initCategories(view: View) {
        val query = db!!.collection(DBConstants.GROUPS)
                .document(arguments!!.getString("groupId")!!)
                .collection(DBConstants.CATEGORIES)

        val options = FirestoreRecyclerOptions.Builder<Category>()
                .setQuery(query, Category::class.java)
                .setLifecycleOwner(this)
                .build()

        fireCategoryAdapter = object : FirestoreRecyclerAdapter<Category, CategoryViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
                val holderView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_category, parent, false)
                return CategoryViewHolder(holderView)
            }

            override fun onBindViewHolder(holder: CategoryViewHolder, position: Int, model: Category) {
                holder.categoryName.apply {
                    text = model.name
                    setOnClickListener {
                        mListener?.onListItemClick(model, snapshots.getSnapshot(holder.adapterPosition).id)
                        dismiss()
                    }
                    setOnLongClickListener {
                        mListener?.onListItemLongClick(model, snapshots.getSnapshot(holder.adapterPosition).id)
                        var docId: String? = null
                        val ref = db!!.collection(DBConstants.GROUPS)
                                .document(arguments!!.getString("groupId")!!)
                                .collection(DBConstants.CATEGORIES)
                        ref.whereEqualTo(DBConstants.FIELD_NAME, model.name).get()
                                .addOnCompleteListener { task ->
                                    docId = task.result!!.documents[0].id
                                }

                        MaterialDialog.Builder(activity!!)
                                .title("Edit Category")
                                .input("new name", model.name, false) { _, input ->
                                    ref.document(docId!!).set(Category(input.toString()))
                                            .addOnSuccessListener {
                                                Toast.makeText(view.context, "Name Changed", Toast.LENGTH_SHORT).show()
                                            }
                                }
                                .positiveText("SAVE")
                                .neutralText("DELETE")
                                .onNeutral { _, _ -> ref.document(docId!!).delete() }
                                .show()
                        return@setOnLongClickListener true
                    }
                }
            }

            override fun onDataChanged() {
            }

            override fun onError(e: FirebaseFirestoreException) {
                Toast.makeText(view.context, e.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        list_category.apply {
            val lm = LinearLayoutManager(this.context)
            layoutManager = lm
            addItemDecoration(DividerItemDecoration(this.context, lm.orientation))
            adapter = fireCategoryAdapter
        }

        button_add_category.apply {
            setOnClickListener {
                MaterialDialog.Builder(activity!!)
                        .title("New Category")
                        .input("Category name", null, false) { _, input ->
                            val ref = db!!.collection(DBConstants.GROUPS)
                                    .document(arguments!!.getString("groupId")!!)
                                    .collection(DBConstants.CATEGORIES)
                            ref.whereEqualTo(DBConstants.FIELD_NAME, input.toString()).get()
                                    .addOnCompleteListener { task ->
                                        if (task.result!!.documents.size > 0) {
                                            Toast.makeText(it.context, "Category already exists.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            ref.add(Category(input.toString()))
                                                    .addOnSuccessListener {
                                                        Toast.makeText(this.context, "Category added", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(this.context, "Failed to add category.", Toast.LENGTH_SHORT).show()

                                                    }
                                        }
                                    }.addOnFailureListener {

                                    }
                        }.positiveText("ADD")
                        .show()
            }
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    private inner class CategoryViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        var categoryName: TextView = itemView.category_text
    }

    companion object {
        private var mListener: ListClickCallbacks<Category>? = null
        fun with(groupId: String, callback: ListClickCallbacks<Category>): CategoryItemListDialogFragment =
                CategoryItemListDialogFragment().apply {
                    arguments = Bundle().apply {
                        putString("groupId", groupId)
                    }
                    mListener = callback
                }
    }
}

package com.afterroot.expenses.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afterroot.expenses.R
import com.afterroot.expenses.model.User
import com.afterroot.expenses.ui.MainActivity
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.FirebaseUtils
import com.afterroot.expenses.utils.OnSaveButtonClick
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_edit_profile.view.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.support.v4.indeterminateProgressDialog


/**
 * Created by Sandip on 13-12-2017.
 */
class EditProfileFragment : Fragment() {

    private var fragmentView: View? = null

    private var listener: OnSaveButtonClick? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        return fragmentView
    }

    var user: FirebaseUser? = null
    val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity!!.toolbar.title = "Edit Profile"
        MainActivity.animateArrow(1f)

        if (FirebaseUtils.isUserSignedIn) {
            user = FirebaseUtils.auth!!.currentUser
            val dialog = indeterminateProgressDialog("Please Wait...")
            with(view) {
                dialog.show()
                input_profile_name.setText(user!!.displayName)
                input_email.setText(user!!.email)
                input_email.isEnabled = false
                db.collection(DBConstants.USERS).document(user!!.uid).get().addOnSuccessListener { documentSnapshot ->
                    val user = documentSnapshot.toObject(User::class.java)
                    input_phone.setText(user.phone)
                    dialog.dismiss()
                }
                button_save_profile.setOnClickListener {
                    dialog.show()
                    val phoneText = input_phone.text.toString()
                    val phone: String = if (phoneText.startsWith("+91", true)) {
                        phoneText
                    } else {
                        "+91$phoneText"
                    }
                    val request = UserProfileChangeRequest.Builder()
                            .setDisplayName(input_profile_name.text.toString())
                            .build()
                    user!!.updateProfile(request).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            db.collection(DBConstants.USERS)
                                    .document(FirebaseUtils.auth!!.currentUser!!.uid)
                                    .set(User(input_profile_name.text.toString(),
                                            user!!.email!!,
                                            user!!.uid,
                                            phone))
                                    .addOnSuccessListener {
                                        dialog.dismiss()
                                        snackbar(activity!!.main_content, "Profile Updated")
                                    }
                        }
                    }


                    listener!!.onSaveButtonClicked()
                }
            }
        } else {
            //Not Logged In
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        MainActivity.animateArrow(0f)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnSaveButtonClick) {
            listener = context
        } else {
            throw RuntimeException("${context.toString()} must implement OnSaveButtonClick")
        }
    }

    companion object {
        fun newInstance() = EditProfileFragment()
    }
}
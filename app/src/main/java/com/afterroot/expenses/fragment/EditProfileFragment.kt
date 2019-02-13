package com.afterroot.expenses.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.expenses.model.User
import com.afterroot.expenses.model.UserViewModel
import com.afterroot.expenses.utils.DBConstants
import com.afterroot.expenses.utils.FirebaseUtils
import com.afterroot.expenses.utils.OnSaveButtonClick
import com.afterroot.expenses.utils.Utils
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_edit_profile.view.*
import org.jetbrains.anko.design.snackbar


/**
 * Created by Sandip on 13-12-2017.
 */
class EditProfileFragment : Fragment() {

    private val args: EditProfileFragmentArgs by navArgs()
    private lateinit var userViewModel: UserViewModel

    private var listener: OnSaveButtonClick? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val userId = args.uid
        userViewModel = ViewModelProviders.of(this).get(UserViewModel::class.java)
        userViewModel.apply {
            init(userId)
            userViewModel.getUser()!!.observe(this@EditProfileFragment, Observer<User> { user ->

            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(com.afterroot.expenses.R.layout.fragment_edit_profile, container, false)
    }

    var firebaseUser: FirebaseUser? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (FirebaseUtils.isUserSignedIn) {
            firebaseUser = FirebaseUtils.auth!!.currentUser
            val dialog = MaterialDialog.Builder(view.context).title("Please Wait...").progress(true, 0).build()
            with(view) {
                dialog.show()
                input_profile_name.setText(firebaseUser!!.displayName)
                input_email.setText(firebaseUser!!.email)
                input_email.isEnabled = false
                db.collection(DBConstants.USERS).document(firebaseUser!!.uid).get().addOnSuccessListener { documentSnapshot ->
                    val user = documentSnapshot.toObject(User::class.java)
                    input_phone.setText(user!!.phone)
                    dialog.dismiss()
                }
                button_save_profile.setOnClickListener {
                    dialog.show()
                    val phoneText = input_phone.text.toString()
                    val phone: String = Utils.formatPhone(activity!!, phoneText)
                    val request = UserProfileChangeRequest.Builder()
                            .setDisplayName(input_profile_name.text.toString())
                            .build()
                    firebaseUser!!.updateProfile(request).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            db.collection(DBConstants.USERS)
                                    .document(FirebaseUtils.auth!!.currentUser!!.uid)
                                    .set(User(input_profile_name.text.toString(),
                                            if (firebaseUser!!.email == null) "" else firebaseUser!!.email!!,
                                            firebaseUser!!.uid,
                                            phone))
                                    .addOnSuccessListener {
                                        dialog.dismiss()
                                        activity!!.root_layout.snackbar("Profile Updated")
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
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnSaveButtonClick) {
            listener = context
        } else {
            throw RuntimeException("${context.toString()} must implement OnSaveButtonClick")
        }
    }
}
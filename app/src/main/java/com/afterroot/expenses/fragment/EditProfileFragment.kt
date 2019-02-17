package com.afterroot.expenses.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.afterroot.expenses.R
import com.afterroot.expenses.model.User
import com.afterroot.expenses.model.UserViewModel
import com.afterroot.expenses.utils.*
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import kotlinx.android.synthetic.main.fragment_edit_profile.view.*
import org.jetbrains.anko.design.snackbar


/**
 * Created by Sandip on 13-12-2017.
 */
class EditProfileFragment : Fragment() {

    private val args: EditProfileFragmentArgs by navArgs()
    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(com.afterroot.expenses.R.layout.fragment_edit_profile, container, false)
    }

    private lateinit var firebaseUser: FirebaseUser
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (FirebaseUtils.isUserSignedIn) {
            firebaseUser = FirebaseUtils.auth?.currentUser!!
            activity!!.progress.setVisible(true)
            input_profile_name.setText(firebaseUser.displayName)
            input_email.setText(firebaseUser.email)
            input_email.isEnabled = false
            db.collection(DBConstants.USERS).document(firebaseUser.uid).get().addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                input_phone.setText(user!!.phone)
                activity!!.progress.setVisible(false)
            }
            activity!!.fab.apply {
                setImageDrawable(activity!!.getDrawableExt(R.drawable.ic_done))
                setOnClickListener {
                    activity!!.progress.setVisible(true)
                    val phoneText = input_phone.text.toString()
                    val phone: String = Utils.formatPhone(activity!!, phoneText)
                    val request = UserProfileChangeRequest.Builder()
                            .setDisplayName(input_profile_name.text.toString())
                            .build()
                    firebaseUser.updateProfile(request).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            db.collection(DBConstants.USERS)
                                    .document(FirebaseUtils.auth!!.currentUser!!.uid)
                                    .set(User(input_profile_name.text.toString(),
                                            if (firebaseUser.email == null) "" else firebaseUser.email!!,
                                            firebaseUser.uid,
                                            phone))
                                    .addOnSuccessListener {
                                        activity!!.apply {
                                            progress.setVisible(false)
                                            root_layout.snackbar("Profile Updated")
                                        }
                                    }
                        }
                    }
                }
            }
        } else {
            //Not Logged In
        }
    }
}

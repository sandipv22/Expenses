package com.afterroot.expenses.model

import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.*


class UserViewModel : ViewModel() {
    private var userId: String? = null
    private val user: LiveData<User>? = null

    fun init(userId: String) {
        this.userId = userId
    }

    fun getUser(): LiveData<User>? {
        return user
    }
}

class FirestoreQueryLiveData(private var query: Query) : LiveData<QuerySnapshot>() {
    val _tag = "FirestoreQueryLiveData"
    private val listener = MyValueEventListener()
    private lateinit var listenerRegistration: ListenerRegistration

    private var listenerRemovePending = false
    private val handler = Handler()

    private val removeListener = Runnable {
        Log.d(_tag, "onInactive: removeListener")
        listenerRegistration.remove()
        listenerRemovePending = false
    }

    override fun onActive() {
        super.onActive()
        Log.d(_tag, "onActive")

        if (listenerRemovePending) {
            handler.removeCallbacks(removeListener)
        } else {
            listenerRegistration = query.addSnapshotListener(listener)
        }

        listenerRemovePending = false
    }


    private inner class MyValueEventListener : EventListener<QuerySnapshot> {
        override fun onEvent(queryDocumentSnapshots: QuerySnapshot?, e: FirebaseFirestoreException?) {
            if (e != null) {
                Log.e(_tag, "Can't listen to doc snapshots: " + queryDocumentSnapshots + ":::" + e.message)
                return
            }
            value = queryDocumentSnapshots
        }
    }
}
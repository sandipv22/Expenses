package com.afterroot.expenses.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import com.afterroot.expenses.R
import com.afterroot.expenses.model.ExpenseItem
import com.afterroot.expenses.utils.Constants
import com.afterroot.expenses.utils.OnFragmentInteractionListener

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ExpenseDetailFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ExpenseDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ExpenseDetailFragment : Fragment() {
    private var item: ExpenseItem? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
        arguments?.let {
            item = it.getSerializable(Constants.KEY_EXPENSE_SERIALIZE) as ExpenseItem?
            Log.d("ExpenseDetailFragment", "onAttach: ${item.toString()}")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_expense_detail, container, false)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.menu_expense_detail, menu)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        @JvmStatic
        fun newInstance(item: ExpenseItem) =
                ExpenseDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(Constants.KEY_EXPENSE_SERIALIZE, item)
                    }
                }
    }
}

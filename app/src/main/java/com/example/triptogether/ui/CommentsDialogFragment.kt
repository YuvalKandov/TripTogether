package com.example.triptogether.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptogether.R
import com.example.triptogether.adapters.CommentsAdapter
import com.example.triptogether.databinding.DialogCommentsBinding
import com.example.triptogether.utilities.FirebaseManager
import com.example.triptogether.utilities.SignalManager
import com.google.firebase.database.ValueEventListener

class CommentsDialogFragment : DialogFragment() {
    private var binding: DialogCommentsBinding? = null
    private var memoryId: String = ""
    private var commentsAdapter: CommentsAdapter = CommentsAdapter()
    private var commentsListener: ValueEventListener? = null

    companion object {
        private const val ARG_MEMORY_ID = "memory_id"

        fun newInstance(memoryId: String): CommentsDialogFragment {
            val fragment = CommentsDialogFragment()
            val args = Bundle()
            args.putString(ARG_MEMORY_ID, memoryId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_TripTogether_FullScreenDialog)
        arguments?.let {
            memoryId = it.getString(ARG_MEMORY_ID, "")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogCommentsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        listenToComments()
    }

    private fun initViews() {
        binding?.commentsRVList?.layoutManager = LinearLayoutManager(requireContext())
        binding?.commentsRVList?.adapter = commentsAdapter

        binding?.commentsBTNClose?.setOnClickListener {
            dismiss()
        }

        binding?.commentsBTNSend?.setOnClickListener {
            sendComment()
        }
    }

    private fun sendComment() {
        val text = binding?.commentsETInput?.text?.toString()?.trim() ?: ""

        if (text.isEmpty()) {
            SignalManager.getInstance().toast("Please enter a comment")
            return
        }

        binding?.commentsBTNSend?.isEnabled = false

        FirebaseManager.getInstance().addComment(
            memoryId,
            text,
            { commentId ->
                binding?.commentsETInput?.text?.clear()
                binding?.commentsBTNSend?.isEnabled = true
                SignalManager.getInstance().vibrate()
            },
            { error ->
                binding?.commentsBTNSend?.isEnabled = true
                SignalManager.getInstance().toast("Failed to add comment")
            }
        )
    }

    private fun listenToComments() {
        binding?.commentsPBLoading?.isVisible = true
        binding?.commentsRVList?.isVisible = false
        binding?.commentsLLEmpty?.isVisible = false

        commentsListener = FirebaseManager.getInstance().listenToComments(
            memoryId
        ) { comments ->
            binding?.commentsPBLoading?.isVisible = false
            if (comments.isEmpty()) {
                binding?.commentsRVList?.isVisible = false
                binding?.commentsLLEmpty?.isVisible = true
            } else {
                binding?.commentsRVList?.isVisible = true
                binding?.commentsLLEmpty?.isVisible = false
                commentsAdapter.comments = comments
                commentsAdapter.notifyDataSetChanged()
                // Scroll to bottom to show newest comments
                binding?.commentsRVList?.scrollToPosition(comments.size - 1)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        commentsListener?.let {
            FirebaseManager.getInstance().removeCommentListener(memoryId, it)
        }
        commentsListener = null
        super.onDestroyView()
        binding = null
    }
}

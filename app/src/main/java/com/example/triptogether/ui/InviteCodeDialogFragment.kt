package com.example.triptogether.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.triptogether.databinding.FragmentInviteCodeDialogBinding
import com.example.triptogether.utilities.SignalManager

class InviteCodeDialogFragment : DialogFragment() {
    private var _binding: FragmentInviteCodeDialogBinding? = null
    private val binding get() = _binding!!

    private var inviteCode: String = ""
    private var tripName: String = ""

    companion object {
        private const val ARG_INVITE_CODE = "invite_code"
        private const val ARG_TRIP_NAME = "trip_name"

        fun newInstance(inviteCode: String, tripName: String): InviteCodeDialogFragment {
            val fragment = InviteCodeDialogFragment()
            val args = Bundle()
            args.putString(ARG_INVITE_CODE, inviteCode)
            args.putString(ARG_TRIP_NAME, tripName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            inviteCode = it.getString(ARG_INVITE_CODE, "")
            tripName = it.getString(ARG_TRIP_NAME, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInviteCodeDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun initViews() {
        binding.inviteLBLCode.text = inviteCode

        // Copy to clipboard when code is clicked
        binding.inviteCVCode.setOnClickListener {
            copyToClipboard()
        }

        // Share button
        binding.inviteBTNShare.setOnClickListener {
            shareInviteCode()
        }

        // Close button
        binding.inviteBTNClose.setOnClickListener {
            dismiss()
        }
    }

    private fun copyToClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Invite Code", inviteCode)
        clipboard.setPrimaryClip(clip)

        SignalManager.getInstance().toast("Invite code copied to clipboard")
        SignalManager.getInstance().vibrate()
    }

    private fun shareInviteCode() {
        val shareText = buildString {
            append("Join my trip \"$tripName\" on TripTogether!\n\n")
            append("Invite Code: $inviteCode\n\n")
            append("1. Open TripTogether app\n")
            append("2. Tap \"Join Trip\"\n")
            append("3. Enter the code above")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Join my trip on TripTogether")
        }

        startActivity(Intent.createChooser(shareIntent, "Share invite code via"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

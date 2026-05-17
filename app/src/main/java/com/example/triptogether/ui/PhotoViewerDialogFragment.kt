package com.example.triptogether.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.example.triptogether.databinding.DialogPhotoViewerBinding
import com.example.triptogether.utilities.ImageLoader

class PhotoViewerDialogFragment : DialogFragment() {

    private var binding: DialogPhotoViewerBinding? = null
    private var photoUrl: String = ""

    companion object {
        private const val ARG_PHOTO_URL: String = "photo_url"

        fun newInstance(photoUrl: String): PhotoViewerDialogFragment {
            val fragment = PhotoViewerDialogFragment()
            val args = Bundle()
            args.putString(ARG_PHOTO_URL, photoUrl)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        arguments?.let {
            photoUrl = it.getString(ARG_PHOTO_URL, "")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogPhotoViewerBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the photo
        binding?.photoviewerIMGPhoto?.let { imageView ->
            if (photoUrl.isNotEmpty()) {
                ImageLoader.getInstance().loadImage(photoUrl, imageView)
            }
        }

        // Close on click
        binding?.photoviewerIMGPhoto?.setOnClickListener {
            dismiss()
        }

        binding?.photoviewerBTNClose?.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}

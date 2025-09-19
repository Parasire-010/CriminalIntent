package com.bignerdranch.android.criminalintent

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File
import kotlin.math.max

class PhotoDetailFragment : DialogFragment() {

    private lateinit var photoView: ImageView
    private var photoFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_photo_detail, container, false)
        photoView = root.findViewById(R.id.crime_solved)
        return root
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full screen
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoPath = requireArguments().getString(ARG_PHOTO_PATH)
        photoFile = photoPath?.let { File(it) }

        photoFile?.takeIf { it.exists() }?.let { file ->
            // Load a scaled bitmap to avoid OOM on big photos
            val bmp = decodeScaledBitmap(file, view.width.coerceAtLeast(1), view.height.coerceAtLeast(1))
            photoView.setImageBitmap(bmp)
        }
    }

    private fun decodeScaledBitmap(file: File, reqW: Int, reqH: Int): Bitmap {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, opts)

        var inSample = 1
        val (w, h) = opts.outWidth to opts.outHeight
        if (h > reqH || w > reqW) {
            val halfH = h / 2
            val halfW = w / 2
            while ((halfH / inSample) >= reqH && (halfW / inSample) >= reqW) {
                inSample *= 2
            }
        }
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = max(1, inSample) })
    }

    companion object {
        private const val ARG_PHOTO_PATH = "photo_path"

        fun newInstance(photoPath: String): PhotoDetailFragment =
            PhotoDetailFragment().apply {
                arguments = Bundle().apply { putString(ARG_PHOTO_PATH, photoPath) }
            }
    }
}

package com.bignerdranch.android.criminalintent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrimeDetailFragment : Fragment() {

    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding get() = _binding!!

    private val args: CrimeDetailFragmentArgs by navArgs()
    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels {
        CrimeDetailViewModelFactory(args.crimeId)
    }

    private var crime: Crime? = null
    private var photoFileName: String? = null
    private var photoFile: File? = null
    private var photoName: String? = null

    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? -> uri?.let { parseContactSelection(it) } }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto ->
        val currentPhotoName = photoName

        if (didTakePhoto && currentPhotoName != null) {
            crimeDetailViewModel.updateCrime { oldCrime ->
                oldCrime.copy(photoFileName = currentPhotoName)
            }
            updatePhoto(currentPhotoName)

            // announce after UI updates
            binding.root.postDelayed({
                val f = File(requireContext().applicationContext.filesDir, currentPhotoName)
                val msg = if (f.exists()) {
                    getString(R.string.a11y_photo_updated)
                } else {
                    getString(R.string.a11y_photo_failed)
                }
                binding.root.announceForAccessibility(msg)
            }, 300)
        } else {
            binding.root.postDelayed({
                binding.root.announceForAccessibility(getString(R.string.a11y_photo_canceled))
            }, 300)
        }
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            makeCallToSuspect()
        } else {
            Toast.makeText(
                requireContext(),
                "Permission denied to access contacts",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fragment Result listener for DateTimePickerFragment
        parentFragmentManager.setFragmentResultListener(
            DateTimePickerFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val newDate = BundleCompat.getSerializable(
                bundle,
                DateTimePickerFragment.RESULT_KEY,
                Date::class.java
            ) ?: return@setFragmentResultListener

            crime?.let { currentCrime ->
                crimeDetailViewModel.updateCrime { currentCrime.copy(date = newDate) }
            }
        }

        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        toolbar.inflateMenu(R.menu.fragment_crime_detail)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.delete_crime -> {
                    deleteCrime()
                    true
                }
                else -> false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeDetailViewModel.crime.collect { currentCrime ->
                    currentCrime?.let {
                        crime = it
                        photoFileName = it.photoFileName
                        photoFile = photoFileName?.let { fileName ->
                            File(requireContext().filesDir, fileName)
                        }
                        updateUi(it)
                    }
                }
            }
        }

        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crime?.let { currentCrime ->
                    crimeDetailViewModel.updateCrime { currentCrime.copy(title = text.toString()) }
                }
            }

            // Show the picker dialog (no setTargetFragment)
            crimeDateTime.setOnClickListener {
                crime?.let { currentCrime ->
                    DateTimePickerFragment.newInstance(currentCrime.date)
                        .show(parentFragmentManager, "dateTimePicker")
                }
            }

            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crime?.let { currentCrime ->
                    crimeDetailViewModel.updateCrime { currentCrime.copy(isSolved = isChecked) }
                }
            }

            crimeSuspect.setOnClickListener {
                selectSuspect.launch(null)
            }

            crimeReport.setOnClickListener {
                crime?.let { currentCrime ->
                    val reportIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, getCrimeReport(currentCrime))
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
                    }
                    startActivity(Intent.createChooser(reportIntent, getString(R.string.send_report)))
                }
            }

            callSuspect.setOnClickListener {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    makeCallToSuspect()
                } else {
                    requestPermission.launch(Manifest.permission.READ_CONTACTS)
                }
            }

            crimeCamera.setOnClickListener {
                photoName = "IMG_${
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                }.jpg"

                // Save where <files-path path="."> points (internal filesDir)
                val file = File(requireContext().applicationContext.filesDir, photoName!!)

                // ✅ derive authority from the app's package, no BuildConfig needed
                val authority = "${requireContext().applicationContext.packageName}.fileprovider"

                val uri = FileProvider.getUriForFile(requireContext(), authority, file)
                takePhoto.launch(uri)
            }

            val captureImageIntent = takePhoto.contract.createIntent(
                requireContext(),
                "".toUri()
            )
            crimeCamera.isEnabled = canResolveIntent(captureImageIntent)

            crimePhoto.setOnClickListener {
                photoFile?.let { file ->
                    if (file.exists()) {
                        val photoDialog = PhotoDetailFragment.newInstance(file.path)
                        photoDialog.show(parentFragmentManager, "photo_dialog")
                    } else {
                        Toast.makeText(requireContext(), "Photo not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager = requireContext().packageManager
        return intent.resolveActivity(packageManager) != null
    }

    private fun parseContactSelection(contactUri: Uri) {
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val cursor = requireActivity().contentResolver.query(
            contactUri,
            queryFields,
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val suspect = it.getString(0)
                crime?.let { currentCrime ->
                    crimeDetailViewModel.updateCrime { currentCrime.copy(suspect = suspect) }
                }
            }
        }
    }

    private fun updatePhoto(photoFileName: String?) {
        if (binding.crimePhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }

            if (photoFile?.exists() == true) {
                binding.crimePhoto.doOnLayout { measuredView ->
                    val scaledBitmap = BitmapFactory.decodeFile(photoFile.path)
                        ?.scale(measuredView.width, measuredView.height)
                    binding.crimePhoto.setImageBitmap(scaledBitmap)
                    binding.crimePhoto.tag = photoFileName
                    // photo is set
                    binding.crimePhoto.contentDescription =
                        getString(R.string.crime_photo_image_description)
                }
            } else {
                binding.crimePhoto.setImageBitmap(null)
                binding.crimePhoto.tag = null
                // photo not set
                binding.crimePhoto.contentDescription =
                    getString(R.string.crime_photo_no_image_description)
            }
        }
    }

    private fun deleteCrime() {
        viewLifecycleOwner.lifecycleScope.launch {
            crime?.let { currentCrime ->
                crimeDetailViewModel.deleteCrime(currentCrime)
                findNavController().navigateUp()
            }
        }
    }

    private fun updateUi(crime: Crime) {
        binding.apply {
            if (crimeTitle.text.toString() != crime.title) {
                crimeTitle.setText(crime.title)
            }

            val locale = resources.configuration.locales[0]
            val dateTimeFormatter = SimpleDateFormat("EEE, MMM dd, yyyy hh:mm a", locale)
            crimeDateTime.text = dateTimeFormatter.format(crime.date)

            crimeSolved.isChecked = crime.isSolved
            crimeSuspect.text = crime.suspect.ifEmpty { getString(R.string.crime_suspect_text) }
            callSuspect.isEnabled = crime.suspect.isNotBlank()
            updatePhoto(crime.photoFileName)
        }
    }

    private fun getCrimeReport(crime: Crime): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val locale = resources.configuration.locales[0]
        val dateTimeFormatter = SimpleDateFormat("EEE, MMM dd, yyyy hh:mm a", locale)
        val formattedDateTime = dateTimeFormatter.format(crime.date)

        val suspectText = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.crime_report,
            crime.title, formattedDateTime, solvedString, suspectText
        )
    }

    private fun makeCallToSuspect() {
        crime?.let { currentCrime ->
            val phoneNumber = getPhoneNumber(currentCrime.suspect)
            phoneNumber?.let {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = "tel:$it".toUri()
                }
                startActivity(dialIntent)
            } ?: Toast.makeText(
                requireContext(),
                "No phone number found for suspect",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getPhoneNumber(suspect: String): String? {
        val contentResolver = requireActivity().contentResolver
        val contactUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val queryFields = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(suspect)
        val cursor = contentResolver.query(contactUri, queryFields, selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

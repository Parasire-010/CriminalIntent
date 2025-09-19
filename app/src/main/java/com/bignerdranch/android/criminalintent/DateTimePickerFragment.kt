package com.bignerdranch.android.criminalintent

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import java.util.Calendar
import java.util.Date

class DateTimePickerFragment : DialogFragment(),
    DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {

    companion object {
        // Keys for Fragment Result API
        const val REQUEST_KEY = "request_date_time"
        const val RESULT_KEY = "result_date_time"

        private const val ARG_DATE = "date"
        private const val ARG_PICKED_YEAR = "picked_year"
        private const val ARG_PICKED_MONTH = "picked_month"
        private const val ARG_PICKED_DAY = "picked_day"

        fun newInstance(date: Date): DateTimePickerFragment =
            DateTimePickerFragment().apply {
                arguments = bundleOf(ARG_DATE to date)
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val baseDate: Date = BundleCompat.getSerializable(
            requireArguments(),
            ARG_DATE,
            Date::class.java
        ) ?: Date()
        val cal = Calendar.getInstance().apply { time = baseDate }

        return DatePickerDialog(
            requireContext(),
            this,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Date picked → show time picker
    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        val baseDate: Date = BundleCompat.getSerializable(
            requireArguments(),
            ARG_DATE,
            Date::class.java
        ) ?: Date()
        val cal = Calendar.getInstance().apply { time = baseDate }

        // remember picked date so we can combine with time later
        requireArguments().putInt(ARG_PICKED_YEAR, year)
        requireArguments().putInt(ARG_PICKED_MONTH, month)
        requireArguments().putInt(ARG_PICKED_DAY, day)

        TimePickerDialog(
            requireContext(),
            this,
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false // 12-hour; change to true if you prefer 24-hour
        ).show()
    }

    // Time picked → send combined result back via Fragment Result API
    override fun onTimeSet(view: TimePicker, hour: Int, minute: Int) {
        val baseDate: Date = BundleCompat.getSerializable(
            requireArguments(),
            ARG_DATE,
            Date::class.java
        ) ?: Date()
        val cal = Calendar.getInstance().apply { time = baseDate }

        val y = requireArguments().getInt(ARG_PICKED_YEAR, cal.get(Calendar.YEAR))
        val m = requireArguments().getInt(ARG_PICKED_MONTH, cal.get(Calendar.MONTH))
        val d = requireArguments().getInt(ARG_PICKED_DAY, cal.get(Calendar.DAY_OF_MONTH))

        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m)
        cal.set(Calendar.DAY_OF_MONTH, d)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)

        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(RESULT_KEY to cal.time)
        )
    }
}

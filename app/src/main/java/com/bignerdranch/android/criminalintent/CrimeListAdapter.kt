package com.bignerdranch.android.criminalintent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimeBinding
import java.text.SimpleDateFormat
import java.util.*

class CrimeHolder(
    private val binding: ListItemCrimeBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(crime: Crime, onCrimeClicked: (crimeId: UUID) -> Unit) {
        binding.crimeTitle.text = crime.title

        // format date/time using device locale
        val locale = binding.root.resources.configuration.locales[0]
        val dateTimeFormatter = SimpleDateFormat("EEE, MMM dd, yyyy hh:mm a", locale)
        val formattedDateTime = dateTimeFormatter.format(crime.date)
        binding.crimeDate.text = formattedDateTime

        // icon visibility + a11y label when visible
        binding.crimeSolvedIcon.apply {
            visibility = if (crime.isSolved) View.VISIBLE else View.GONE
            if (visibility == View.VISIBLE) {
                contentDescription = itemView.context.getString(R.string.crime_solved_icon_desc)
            }
        }

        // one concise description for the whole row
        val solvedText = if (crime.isSolved) {
            itemView.context.getString(R.string.crime_row_solved)
        } else {
            itemView.context.getString(R.string.crime_row_not_solved)
        }
        binding.root.contentDescription = itemView.context.getString(
            R.string.crime_row_a11y,
            crime.title,
            formattedDateTime,
            solvedText
        )

        // prevent TalkBack from reading children separately
        binding.crimeTitle.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        binding.crimeDate.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        binding.crimeSolvedIcon.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        binding.root.isFocusable = true

        // click through to detail
        binding.root.setOnClickListener { onCrimeClicked(crime.id) }
    }
}

class CrimeListAdapter(
    private val crimes: List<Crime>,
    private val onCrimeClicked: (crimeId: UUID) -> Unit
) : RecyclerView.Adapter<CrimeHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
        return CrimeHolder(binding)
    }

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        holder.bind(crimes[position], onCrimeClicked)
    }

    override fun getItemCount() = crimes.size
}

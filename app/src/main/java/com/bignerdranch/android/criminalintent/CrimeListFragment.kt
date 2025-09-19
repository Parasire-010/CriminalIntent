package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeListBinding
import kotlinx.coroutines.launch
import java.util.*

class CrimeListFragment : Fragment() {

    private var _binding: FragmentCrimeListBinding? = null
    private val binding get() = _binding!!

    private val crimeListViewModel: CrimeListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeListBinding.inflate(inflater, container, false)

        binding.crimeRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Show empty state by default; switch once data arrives in the collector
        binding.crimeRecyclerView.visibility = View.GONE
        binding.emptyPlaceholder.visibility = View.VISIBLE

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // (FAB removed)

        // Top-right menu "+"
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_crime_list, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.new_crime -> { addNewCrime(); true }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.STARTED)

        // Observe list and update UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeListViewModel.crimes.collect { crimes ->
                    if (crimes.isEmpty()) {
                        binding.crimeRecyclerView.visibility = View.GONE
                        binding.emptyPlaceholder.visibility = View.VISIBLE
                    } else {
                        binding.emptyPlaceholder.visibility = View.GONE
                        binding.crimeRecyclerView.visibility = View.VISIBLE
                    }

                    binding.crimeRecyclerView.adapter =
                        CrimeListAdapter(crimes) { crimeId ->
                            findNavController().navigate(
                                CrimeListFragmentDirections
                                    .actionCrimeListFragmentToCrimeDetailFragment(crimeId)
                            )
                        }
                }
            }
        }
    }

    private fun addNewCrime() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newCrime = Crime(
                id = UUID.randomUUID(),
                title = "",
                date = Date(),
                isSolved = false
            )
            crimeListViewModel.addCrime(newCrime)
            findNavController().navigate(
                CrimeListFragmentDirections
                    .actionCrimeListFragmentToCrimeDetailFragment(newCrime.id)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

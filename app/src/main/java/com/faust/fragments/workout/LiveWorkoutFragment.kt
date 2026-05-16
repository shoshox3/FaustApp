package com.faust.fragments.workout

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.faust.R
import com.faust.adapters.WorkoutExerciseAdapter
import com.faust.databinding.FragmentLiveWorkoutBinding
import com.faust.models.Exercise
import com.faust.models.WorkoutTemplate
import com.faust.util.show
import com.faust.util.hide
import com.faust.util.toDurationString
import com.faust.viewmodels.LiveWorkoutViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LiveWorkoutFragment : Fragment() {

    private var _binding: FragmentLiveWorkoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LiveWorkoutViewModel by activityViewModels()

    private lateinit var exerciseAdapter: WorkoutExerciseAdapter
    private var elapsedSeconds = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        startTimer()

        // Start workout if not already started
        val template = arguments?.getParcelable<WorkoutTemplate>("template")
        if (viewModel.currentWorkout.value == null) {
            viewModel.startWorkout(template)
        }
    }

    private fun setupRecyclerView() {
        exerciseAdapter = WorkoutExerciseAdapter(
            onSetCompleted = { exerciseId, setId, reps, weight ->
                viewModel.completeSet(exerciseId, setId, reps, weight)
            },
            onAddSet = { exerciseId ->
                viewModel.addSetToExercise(exerciseId)
            }
        )
        binding.rvExercises.apply {
            adapter = exerciseAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupListeners() {
        binding.btnFinishWorkout.setOnClickListener {
            showFinishConfirmation()
        }
        binding.btnAddExercise.setOnClickListener {
            // Navigate to exercise picker
            findNavController().navigate(R.id.action_liveWorkout_to_exercisePicker)
        }
        binding.btnCancelWorkout.setOnClickListener {
            showCancelConfirmation()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentWorkout.collectLatest { workout ->
                workout ?: return@collectLatest
                binding.tvWorkoutName.text = workout.name
                exerciseAdapter.submitList(workout.exercises)

                // Update stats
                binding.tvTotalSets.text = workout.exercises
                    .sumOf { it.sets.count { s -> s.isCompleted } }.toString()
                binding.tvTotalVolume.text = workout.exercises
                    .sumOf { ex -> ex.sets.filter { it.isCompleted }.sumOf { (it.weight * it.reps).toDouble() } }
                    .toFloat().let { "${it.toInt()} kg" }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.workoutSaved.collectLatest {
                findNavController().navigate(R.id.action_liveWorkout_to_workoutSummary)
            }
        }
    }

    private fun startTimer() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                elapsedSeconds++
                binding.tvTimer.text = elapsedSeconds.toDurationString()
            }
        }
    }

    private fun showFinishConfirmation() {
        // Show dialog then finish
        viewModel.finishWorkout()
    }

    private fun showCancelConfirmation() {
        // Show dialog then cancel
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

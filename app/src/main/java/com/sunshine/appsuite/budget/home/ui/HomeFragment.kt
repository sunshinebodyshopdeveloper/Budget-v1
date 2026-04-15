package com.sunshine.appsuite.budget.home.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunshine.appsuite.budget.BudgetApp
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.databinding.FragmentHomeBinding
import com.sunshine.appsuite.budget.home.data.HomeSectionRepository
import com.sunshine.appsuite.budget.home.model.HomeSection
import com.sunshine.appsuite.budget.home.ui.adapter.HomeSectionsAdapter
import com.sunshine.appsuite.budget.home.ui.touch.HomeSectionsDragStartListener
import com.sunshine.appsuite.budget.home.ui.touch.HomeSectionsTouchHelperCallback
import com.sunshine.appsuite.budget.home.ui.widgets.appointments.HomeAppointmentsSectionController
import com.sunshine.appsuite.budget.home.ui.widgets.tracking.HomeTrackingSectionController
import com.sunshine.appsuite.budget.home.ui.widgets.tracking.OrdersStatsApi
import com.sunshine.appsuite.budget.home.ui.widgets.tracking.OrdersStatsRepository
import com.sunshine.appsuite.budget.tools.appointments.data.network.AppointmentsApi
import com.sunshine.appsuite.budget.tools.appointments.data.repository.AppointmentsRepository
import com.sunshine.appsuite.budget.tools.appointments.ui.create.CreateAppointmentActivity
import com.sunshine.appsuite.budget.orders.ServiceOrderActivity
import com.sunshine.appsuite.budget.assistant.AssistantActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: HomeSectionRepository
    private lateinit var adapter: HomeSectionsAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    // Sección: Citas en Home
    private lateinit var appointmentsRepo: AppointmentsRepository
    private lateinit var appointmentsController: HomeAppointmentsSectionController

    // Sección: Tracking en Home
    private lateinit var trackingRepo: OrdersStatsRepository
    private lateinit var trackingController: HomeTrackingSectionController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = HomeSectionRepository(requireContext())

        // IMPORTANT: inicializa controllers ANTES de setear el adapter (para que bindSection ya tenga todo listo)
        setupAppointmentsSection()
        setupTrackingSection()

        val sections = repo.getSections()

        adapter = HomeSectionsAdapter(
            sections = sections,
            dragStartListener = HomeSectionsDragStartListener { vh ->
                itemTouchHelper.startDrag(vh)
            },
            onBindSection = ::bindSection
        )

        binding.recyclerHomeSections.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHomeSections.adapter = adapter

        val callback = HomeSectionsTouchHelperCallback(
            adapter = adapter,
            onDragFinished = { repo.saveOrder(adapter.getCurrentSections()) }
        )
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerHomeSections)

        setupSwipeToRefresh()
    }

    private fun setupAppointmentsSection() {
        val app = requireActivity().application as BudgetApp
        val api = app.retrofit.create(AppointmentsApi::class.java)
        appointmentsRepo = AppointmentsRepository(api)

        appointmentsController = HomeAppointmentsSectionController(
            repo = appointmentsRepo,
            onError = { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupTrackingSection() {
        val app = requireActivity().application as BudgetApp
        val api = app.retrofit.create(OrdersStatsApi::class.java)
        trackingRepo = OrdersStatsRepository(api)

        val appContext = requireContext().applicationContext

        trackingController = HomeTrackingSectionController(
            repo = trackingRepo,
            onError = { msg ->
                val ctx = context ?: return@HomeTrackingSectionController
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            },
            onTileClick = { tile ->
                // listo para conectar navegación / filtros por tile
            }
        )
    }


    override fun onResume() {
        super.onResume()

        if (!::repo.isInitialized || !::adapter.isInitialized) return

        if (repo.consumeDirty()) {
            adapter.setSections(repo.getSections())
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            adapter.setSections(repo.getSections())

            if (::appointmentsController.isInitialized) {
                appointmentsController.refresh(viewLifecycleOwner.lifecycleScope)
            }

            if (::trackingController.isInitialized) {
                trackingController.refresh(viewLifecycleOwner.lifecycleScope)
            }

            binding.swipeRefresh.isRefreshing = false
            Toast.makeText(requireContext(), "Datos sincronizados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindSection(itemView: View, section: HomeSection) {
        when (section.type) {

            HomeSection.Type.FUNCTIONS -> {
                itemView.findViewById<View>(R.id.cardAssistant)?.setOnClickListener {
                    startActivity(Intent(requireContext(), AssistantActivity::class.java))
                }

                itemView.findViewById<View>(R.id.cardOrders)?.setOnClickListener {
                    startActivity(Intent(requireContext(), ServiceOrderActivity::class.java))
                }

                itemView.findViewById<View>(R.id.cardAppointments)?.setOnClickListener {
                    startActivity(Intent(requireContext(), CreateAppointmentActivity::class.java))
                }
            }

            HomeSection.Type.QUICK_ACTIONS -> Unit

            HomeSection.Type.APPOINTMENTS -> {
                if (!::appointmentsController.isInitialized) return
                appointmentsController.bind(itemView)
                appointmentsController.ensureLoaded(viewLifecycleOwner.lifecycleScope)
            }

            HomeSection.Type.TRACKING -> {
                if (!::trackingController.isInitialized) return
                trackingController.bind(itemView)
                trackingController.ensureLoaded(viewLifecycleOwner.lifecycleScope)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (::appointmentsController.isInitialized) appointmentsController.unbind()
        if (::trackingController.isInitialized) trackingController.unbind()

        _binding = null
    }
}

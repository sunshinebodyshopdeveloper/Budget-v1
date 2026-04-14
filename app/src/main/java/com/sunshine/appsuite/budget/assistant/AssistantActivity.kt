package com.sunshine.appsuite.budget.assistant

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.lifecycle.*
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.assistant.data.AssistantApiService
import com.sunshine.appsuite.budget.assistant.domain.GeminiAssistantManager
import com.sunshine.appsuite.budget.assistant.ui.AssistantAdapter
import com.sunshine.appsuite.budget.assistant.ui.AssistantViewModel
import com.sunshine.appsuite.budget.assistant.ui.ChatMessage
import com.sunshine.appsuite.budget.assistant.ui.PopItemAnimator
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.data.network.UserApi
import com.sunshine.appsuite.budget.security.TokenManager
import com.sunshine.appsuite.databinding.ActivityAssistantBinding
import kotlinx.coroutines.launch
import kotlin.math.max

class AssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssistantBinding
    private val chatAdapter = AssistantAdapter()

    private val viewModel: AssistantViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val tokenManager = TokenManager(applicationContext)
                val retrofit = ApiClient.createRetrofit(tokenManager)
                val api = retrofit.create(AssistantApiService::class.java)
                return AssistantViewModel(api, GeminiAssistantManager()) as T
            }
        }
    }

    // --- Sound System ---
    private lateinit var soundPool: SoundPool
    private var popSoundId: Int = 0
    private var soundsReady: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initResources()
        setupViews()
        observeViewModel()
        loadInitialData()
    }

    private fun initResources() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        popSoundId = soundPool.load(this, R.raw.bubble_pop, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundsReady = true
        }
    }

    private fun setupViews() {
        setupEdgeToEdge()
        setupToolbar()

        // RecyclerView Setup
        binding.rvChat.apply {
            adapter = chatAdapter
            itemAnimator = PopItemAnimator()
        }

        // Input Setup
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                processUserInteraction(text, isChip = false)
                binding.etMessage.text?.clear()
            }
        }

        // Suggestions Setup
        binding.chipStatus.setOnClickListener {
            processUserInteraction(binding.chipStatus.text.toString(), isChip = true)
        }
        binding.chipDate.setOnClickListener {
            processUserInteraction(binding.chipDate.text.toString(), isChip = true)
        }
    }

    /**
     * Centraliza la interacción del usuario (ya sea por teclado o por Chips)
     */
    private fun processUserInteraction(text: String, isChip: Boolean) {
        ensureChatVisibility()

        // UI Update
        playBubblePop()
        chatAdapter.addMessage(ChatMessage(text, isUser = true))
        binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)

        // ViewModel Logic
        if (isChip) {
            viewModel.onChipClicked(text)
        } else {
            viewModel.sendMessage(text)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.aiResponse.collect { message ->
                    displayAssistantMessage(message)
                }
            }
        }
    }

    private fun displayAssistantMessage(text: String) {
        ensureChatVisibility()
        playBubblePop()
        chatAdapter.addMessage(ChatMessage(text, isUser = false))
        binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun ensureChatVisibility() {
        if (binding.welcomeContainer.visibility == View.VISIBLE) {
            binding.welcomeContainer.visibility = View.GONE
            binding.rvChat.visibility = View.VISIBLE
        }
    }

    private fun playBubblePop() {
        if (soundsReady && binding.rvChat.visibility == View.VISIBLE) {
            soundPool.play(popSoundId, 0.7f, 0.7f, 1, 0, 1f)
        }
    }

    private fun loadInitialData() {
        // 1. Contexto de orden si viene de otra pantalla
        val orderId = intent.getIntExtra("EXTRA_ORDER_ID", -1)
        if (orderId > 0) viewModel.initAssistant(orderId)

        // 2. Personalización del saludo
        lifecycleScope.launch {
            try {
                val tokenManager = TokenManager(applicationContext)
                val userApi = ApiClient.createRetrofit(tokenManager).create(UserApi::class.java)
                val user = userApi.getUser()
                val name = user.name?.split(" ")?.firstOrNull() ?: ""
                binding.tvGreeting.text = if (name.isNotEmpty()) "Hola, $name" else "Hola"
            } catch (e: Exception) {
                binding.tvGreeting.text = "Hola"
            }
        }
    }

    // --- UI/UX Helpers ---

    private fun setupEdgeToEdge() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.google_background_settings)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.google_white)

        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // Manejo inteligente del teclado y barra de navegación
        ViewCompat.setOnApplyWindowInsetsListener(binding.inputBar) { v, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            // Usamos un padding base de 12dp (convertido a px) + el teclado
            val basePadding = (12 * resources.displayMetrics.density).toInt()
            v.updatePadding(bottom = max(imeBottom, navBottom) + basePadding)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundPool.isInitialized) soundPool.release()
    }
}
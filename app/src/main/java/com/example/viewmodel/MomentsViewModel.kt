package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GeminiClient
import com.example.data.Memory
import com.example.data.MemoryRepository
import com.example.utils.ImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MomentsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MemoryRepository(database.memoryDao())

    // All local memories retrieved from Room reactively
    val allMemories: StateFlow<List<Memory>> = repository.allMemories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived StateFlow showing today's moments carved into Morning, Noon, Evening, Night slots
    val todayMemories: StateFlow<Map<String, Memory?>> = allMemories
        .map { memories ->
            val mapped = mutableMapOf<String, Memory?>(
                "Morning" to null,
                "Noon" to null,
                "Evening" to null,
                "Night" to null
            )
            val calendar = Calendar.getInstance()
            val todayYear = calendar.get(Calendar.YEAR)
            val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

            for (mem in memories) {
                calendar.timeInMillis = mem.timestamp
                val targetYear = calendar.get(Calendar.YEAR)
                val targetDay = calendar.get(Calendar.DAY_OF_YEAR)

                if (todayYear == targetYear && todayDay == targetDay) {
                    mapped[mem.timeOfDay] = mem
                }
            }
            mapped
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = mapOf(
                "Morning" to null,
                "Noon" to null,
                "Evening" to null,
                "Night" to null
            )
        )

    // Detailed Polaroid overlay target
    private val _selectedMemoryDetail = MutableStateFlow<Memory?>(null)
    val selectedMemoryDetail: StateFlow<Memory?> = _selectedMemoryDetail.asStateFlow()

    // Gemini poetical response states
    private val _currentAuraReflection = MutableStateFlow<String>("")
    val currentAuraReflection: StateFlow<String> = _currentAuraReflection.asStateFlow()

    private val _isAuraLoading = MutableStateFlow<Boolean>(false)
    val isAuraLoading: StateFlow<Boolean> = _isAuraLoading.asStateFlow()

    // Camera creation state properties
    val targetCaptureSlot = MutableStateFlow("Morning") // The time slot currently being captured
    val activeFilter = MutableStateFlow("Standard Soft") // Vintage filters
    val activeCaption = MutableStateFlow("")
    val activeMood = MutableStateFlow("Peaceful")
    val activeLocation = MutableStateFlow("Private Spot")
    val selectedPresetName = MutableStateFlow("Sunbeam Window")

    // Active screen navigation
    private val _currentScreen = MutableStateFlow("Home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Session state
    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _loggedInUserEmail = MutableStateFlow("")
    val loggedInUserEmail: StateFlow<String> = _loggedInUserEmail.asStateFlow()

    // Predefined nostalgic aesthetic preset cards for mock capture
    val presetAesthetics = mapOf(
        "Morning" to listOf(
            PresetConfig("Sunbeam Window", "Amber Sunbeams", "Bedroom Sill", "Morning sun light cascading on soft bedsheets."),
            PresetConfig("Crisp Steam Tea", "Crisp Morn Air", "Kitchen Nook", "Quiet coffee cup rising steam in the dim morning."),
            PresetConfig("Mist Pathway", "Misty Scent", "Garden Fence", "Dewdrops on leaves. A sleeping quiet neighborhood.")
        ),
        "Noon" to listOf(
            PresetConfig("Desk Dust Motes", "Warm Shadows", "Study Corner", "Floating dust motes glowing under direct solar heat."),
            PresetConfig("Cold Ice condensation", "Fresh Breeze", "Porch Steps", "Iced lemonade drops dripping, refreshing the midday heat."),
            PresetConfig("Solar Geometry", "Clear Contrast", "Balcony Siding", "Sharp geometric balcony shadows drawn on building concrete.")
        ),
        "Evening" to listOf(
            PresetConfig("Sunset Horizon", "Peach Gradients", "Dusk Lookout", "Unwinding orange clouds holding transient warmth."),
            PresetConfig("Sip of Espresso", "Cinematic Amber", "Old Town Cafe", "Watching evening commuters fading into the streetlights."),
            PresetConfig("Vinyl Spinning", "Retro Dusk", "Living Hearth", "Old acoustic sounds filling the small warm living space.")
        ),
        "Night" to listOf(
            PresetConfig("Moonlight Woods", "Silver Velvet", "Attic Window", "Full moon sweeping soft silver rays on historic books."),
            PresetConfig("Warm Blanket Light", "Cozy Yellow", "Bed Pillow", "Gentle bedside yellow light keeping dark nightmares at bay."),
            PresetConfig("Balcony Silence", "Deep Moonlight", "Outside Patio", "Distant city crickets humming under a constellation sky.")
        )
    )

    init {
        // Load persistent login session
        val sharedPrefs = application.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        _isUserLoggedIn.value = sharedPrefs.getBoolean("is_logged_in", false)
        _loggedInUserEmail.value = sharedPrefs.getString("email", "") ?: ""

        // Pre-populate database with some evocative aesthetic memory items on first launching
        // so the user starts with a gorgeous-looking nostalgic layout!
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(application)
            val dao = db.memoryDao()
            // Check if there are any memories
            val count = dao.getAllMemories().stateIn(this).value.size
            if (count == 0) {
                // Populate nostalgic demo data back-dated
                val calendar = Calendar.getInstance()

                // YESTERDAY
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                
                // Yesterday Morning
                calendar.set(Calendar.HOUR_OF_DAY, 8)
                dao.insertMemory(
                    Memory(
                        caption = "The morning coffee steam was particularly slow today.",
                        mood = "Quiet",
                        timestamp = calendar.timeInMillis,
                        timeOfDay = "Morning",
                        photoPath = "preset_morning_tea",
                        weather = "Misty Sunrise",
                        location = "Kitchen Window",
                        filterApplied = "Vintage Chrome"
                    )
                )

                // Yesterday Evening
                calendar.set(Calendar.HOUR_OF_DAY, 18)
                dao.insertMemory(
                    Memory(
                        caption = "Soft pink sky reflected on the rain puddles.",
                        mood = "Nostalgic",
                        timestamp = calendar.timeInMillis,
                        timeOfDay = "Evening",
                        photoPath = "preset_evening_sunset",
                        weather = "Sunset Peach",
                        location = "Riverside Path",
                        filterApplied = "Warm Grain"
                    )
                )

                // TWO DAYS AGO
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                
                // Two days ago Noon
                calendar.set(Calendar.HOUR_OF_DAY, 13)
                dao.insertMemory(
                    Memory(
                        caption = "Drawn shadows of high leaves on the table wood.",
                        mood = "Peaceful",
                        timestamp = calendar.timeInMillis,
                        timeOfDay = "Noon",
                        photoPath = "preset_noon_leaves",
                        weather = "Bright Air",
                        location = "Backyard Table",
                        filterApplied = "Standard Soft"
                    )
                )

                // Two days ago Night
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                dao.insertMemory(
                    Memory(
                        caption = "Quiet streets breathing under soft yellow streetlights.",
                        mood = "Reflective",
                        timestamp = calendar.timeInMillis,
                        timeOfDay = "Night",
                        photoPath = "preset_night_street",
                        weather = "Moonlit Blue",
                        location = "Attic Window",
                        filterApplied = "Noir"
                    )
                )
            }
        }
    }

    fun selectMemory(memory: Memory?) {
        _selectedMemoryDetail.value = memory
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // Capture simulated image or save customized photos locally
    fun saveNostalgicMemory(context: Context, customBitmap: Bitmap? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val slot = targetCaptureSlot.value
            val caption = activeCaption.value.ifBlank { "Quiet moment captured." }
            val mood = activeMood.value
            val filter = activeFilter.value
            val presetName = selectedPresetName.value
            val preset = presetAesthetics[slot]?.firstOrNull { it.title == presetName }
            
            val weatherVal = preset?.weather ?: "Warm Light"
            val locationVal = activeLocation.value.ifBlank { preset?.location ?: "Private Spot" }

            val savedPath = if (customBitmap != null) {
                ImageStorage.saveBitmapToInternal(context, customBitmap, "photo_${slot.lowercase()}")
            } else {
                // Generate a highly visual simulated nostalgic camera bitmap instead!
                val simulatedBmp = ImageStorage.createRetroSimulatedBitmap(
                    timeOfDay = slot,
                    themeTitle = presetName,
                    customCaption = caption,
                    mood = mood,
                    filterName = filter
                )
                ImageStorage.saveBitmapToInternal(context, simulatedBmp, "sim_${slot.lowercase()}")
            }

            val newMemory = Memory(
                caption = caption,
                mood = mood,
                timestamp = System.currentTimeMillis(),
                timeOfDay = slot,
                photoPath = savedPath,
                weather = weatherVal,
                location = locationVal,
                filterApplied = filter
            )

            repository.insert(newMemory)

            // Reset forms and navigate back home
            withContext(Dispatchers.Main) {
                resetCameraForm()
                _currentScreen.value = "Home"
            }
        }
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(memory)
            if (_selectedMemoryDetail.value?.id == memory.id) {
                _selectedMemoryDetail.value = null
            }
        }
    }

    fun updateMemory(memory: Memory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(memory)
            if (_selectedMemoryDetail.value?.id == memory.id) {
                _selectedMemoryDetail.value = memory
            }
        }
    }

    private fun resetCameraForm() {
        activeCaption.value = ""
        activeMood.value = "Peaceful"
        activeLocation.value = ""
        activeFilter.value = "Standard Soft"
    }

    fun generateAuraInterpretation() {
        viewModelScope.launch {
            _isAuraLoading.value = true
            _currentAuraReflection.value = "Consulting Aura mirroring..."
            // Aggregate user feeling logs
            val recentMoments = allMemories.value.take(6)
            if (recentMoments.isEmpty()) {
                _currentAuraReflection.value = "Your memory thread is quiet. Take a photo of morning tea or nightly moonlight to see your aura shimmer."
                _isAuraLoading.value = false
                return@launch
            }

            val aggregatePrompt = recentMoments.joinToString("\n") {
                "- Captured a ${it.timeOfDay} moment in ${it.location} expressing feeling '${it.mood}' with words: '${it.caption}'"
            }

            val resultText = withContext(Dispatchers.IO) {
                GeminiClient.generatePoeticReflection(aggregatePrompt)
            }

            _currentAuraReflection.value = resultText
            _isAuraLoading.value = false
        }
    }

    fun login(email: String, password: String): Boolean {
        if (email.contains("@") && password.length >= 6) {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("user_session", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("email", email)
                .apply()
            _isUserLoggedIn.value = true
            _loggedInUserEmail.value = email
            return true
        }
        return false
    }

    fun logout() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        _isUserLoggedIn.value = false
        _loggedInUserEmail.value = ""
        _currentScreen.value = "Home"
    }
}

data class PresetConfig(
    val title: String,
    val weather: String,
    val location: String,
    val caption: String
)

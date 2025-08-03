package id.xms.xtrakernelmanager.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.xms.xtrakernelmanager.data.repository.TuningRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ThermalProfile(val displayName: String, val index: Int)

@HiltViewModel
class TuningViewModel @Inject constructor(
    private val application: Application,
    private val repo: TuningRepository
) : AndroidViewModel(application) {

    private val thermalPrefs: SharedPreferences by lazy {
        application.getSharedPreferences("thermal_settings_prefs", Context.MODE_PRIVATE)
    }
    @Suppress("PrivatePropertyName")
    private val KEY_LAST_APPLIED_THERMAL_INDEX = "last_applied_thermal_index"

    val cpuClusters = listOf("cpu0", "cpu4", "cpu7")

    // --- CPU States ---
    private val _generalAvailableCpuGovernors = MutableStateFlow<List<String>>(emptyList())
    val generalAvailableCpuGovernors: StateFlow<List<String>> = _generalAvailableCpuGovernors.asStateFlow()

    private val _availableCpuFrequenciesPerClusterMap = MutableStateFlow<Map<String, List<Int>>>(emptyMap())

    private val _currentCpuGovernors = mutableMapOf<String, MutableStateFlow<String>>()
    private val _currentCpuFrequencies = mutableMapOf<String, MutableStateFlow<Pair<Int, Int>>>()

    // --- GPU States ---
    private val _availableGpuGovernors = MutableStateFlow<List<String>>(emptyList())
    val availableGpuGovernors: StateFlow<List<String>> = _availableGpuGovernors.asStateFlow()

    private val _currentGpuGovernor = MutableStateFlow("...")
    val currentGpuGovernor: StateFlow<String> = _currentGpuGovernor.asStateFlow()

    private val _availableGpuFrequencies = MutableStateFlow<List<Int>>(emptyList())
    val availableGpuFrequencies: StateFlow<List<Int>> = _availableGpuFrequencies.asStateFlow()

    private val _currentGpuMinFreq = MutableStateFlow(0)
    val currentGpuMinFreq: StateFlow<Int> = _currentGpuMinFreq.asStateFlow()

    private val _currentGpuMaxFreq = MutableStateFlow(0)
    val currentGpuMaxFreq: StateFlow<Int> = _currentGpuMaxFreq.asStateFlow()

    private val _gpuPowerLevelRange = MutableStateFlow(0f to 5f)
    val gpuPowerLevelRange: StateFlow<Pair<Float, Float>> = _gpuPowerLevelRange.asStateFlow()

    private val _currentGpuPowerLevel = MutableStateFlow(0f)
    val currentGpuPowerLevel: StateFlow<Float> = _currentGpuPowerLevel.asStateFlow()

    // --- OpenGLES Driver ---
    private val _currentOpenGlesDriver = MutableStateFlow("Loading...")
    val currentOpenGlesDriver: StateFlow<String> = _currentOpenGlesDriver.asStateFlow()

    // --- GPU Renderer ---
    private val _currentGpuRenderer = MutableStateFlow("Loading...")
    val currentGpuRenderer: StateFlow<String> = _currentGpuRenderer.asStateFlow()

    // --- Vulkan API Version ---
    private val _vulkanApiVersion = MutableStateFlow("Loading...")
    val vulkanApiVersion: StateFlow<String> = _vulkanApiVersion.asStateFlow()

    val availableGpuRenderers = listOf(
        "Default", "OpenGL", "Vulkan", "ANGLE", "OpenGL (SKIA)", "Vulkan (SKIA)"
    )

    // State untuk mengelola dialog konfirmasi reboot
    private val _showRebootConfirmationDialog = MutableStateFlow(false)
    val showRebootConfirmationDialog: StateFlow<Boolean> = _showRebootConfirmationDialog.asStateFlow()

    // Opsional: Untuk memberi tahu UI jika perintah reboot gagal (misalnya untuk menampilkan Snackbar)
    private val _rebootCommandFeedback = MutableSharedFlow<String>() // String untuk pesan feedback
    val rebootCommandFeedback: SharedFlow<String> = _rebootCommandFeedback.asSharedFlow()


    // --- Swappiness State ---
    private val _swappiness = MutableStateFlow(60)
    val swappiness: StateFlow<Int> = _swappiness.asStateFlow()

    // --- Thermal States ---
    val availableThermalProfiles = listOf(
        ThermalProfile("Disable", 0),
        ThermalProfile("Extreme", 2),
        ThermalProfile("Incalls", 8),
        ThermalProfile("Dynamic", 10),
        ThermalProfile("PUBG", 13),
        ThermalProfile("Thermal 20", 20),
        ThermalProfile("Game", 40),
        ThermalProfile("Camera", 42),
        ThermalProfile("Game 2", 50),
        ThermalProfile("YouTube", 51)
    ).sortedBy { it.displayName }

    private val _isThermalLoading = MutableStateFlow(true)
    val isThermalLoading: StateFlow<Boolean> = _isThermalLoading.asStateFlow()

    private val _currentThermalModeIndex = MutableStateFlow(-1)
    val currentThermalModeIndex: StateFlow<Int> = _currentThermalModeIndex.asStateFlow()

    val currentThermalProfileName: StateFlow<String> = _currentThermalModeIndex.map { currentIndex ->
        val profileName = availableThermalProfiles.find { it.index == currentIndex }?.displayName
        when {
            profileName != null -> profileName
            currentIndex == -1 && _isThermalLoading.value -> "Loading..."
            currentIndex == -1 -> "Disable"
            else -> "Unknown ($currentIndex)"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    init {
        Log.d("TuningVM_Init", "ViewModel initializing...")
        initializeCpuStateFlows()
        fetchAllInitialData()
        Log.d("TuningVM_Init", "ViewModel initialization complete.")
    }

    private fun initializeCpuStateFlows() {
        Log.d("TuningVM_Init", "Initializing CPU StateFlows for clusters: $cpuClusters")
        cpuClusters.forEach { cluster ->
            _currentCpuGovernors.getOrPut(cluster) { MutableStateFlow("...") }
            _currentCpuFrequencies.getOrPut(cluster) { MutableStateFlow(0 to 0) }
        }
    }

    private fun fetchAllInitialData() {
        viewModelScope.launch {
            launch(Dispatchers.IO) { fetchAllCpuData() }
            launch(Dispatchers.IO) { fetchGpuData() }
            launch(Dispatchers.IO) { fetchCurrentSwappiness() }
            fetchCurrentThermalMode(isInitialLoad = true)
            launch(Dispatchers.IO) { fetchOpenGlesDriver() }
            launch(Dispatchers.IO) { fetchCurrentGpuRenderer() }
            launch(Dispatchers.IO) { fetchVulkanApiVersion() }
        }
    }

    // --- CPU Control Methods ---
    private suspend fun fetchAllCpuData() {
        Log.d("TuningVM_CPU", "Fetching all CPU data...")
        val tempAvailableGovernorsMap = mutableMapOf<String, List<String>>()
        val tempAvailableFrequenciesMap = mutableMapOf<String, List<Int>>()

        cpuClusters.forEach { cluster ->
            Log.d("TuningVM_CPU", "Fetching data for CPU cluster: $cluster")
            coroutineScope {
                launch {
                    repo.getCpuGov(cluster)
                        .take(1)
                        .catch { e -> Log.e("TuningVM_CPU", "Error current gov for $cluster", e); _currentCpuGovernors[cluster]?.value = "Error" }
                        .collect { _currentCpuGovernors[cluster]?.value = it }
                }
                launch {
                    repo.getCpuFreq(cluster)
                        .take(1)
                        .catch { e -> Log.e("TuningVM_CPU", "Error current freq for $cluster", e); _currentCpuFrequencies[cluster]?.value = (0 to -1) }
                        .collect { _currentCpuFrequencies[cluster]?.value = it }
                }
                launch {
                    repo.getAvailableCpuGovernors(cluster)
                        .catch { e -> Log.e("TuningVM_CPU", "Error available governors for $cluster", e); emit(emptyList()) }
                        .collect { tempAvailableGovernorsMap[cluster] = it }
                }
                launch {
                    repo.getAvailableCpuFrequencies(cluster)
                        .catch { e -> Log.e("TuningVM_CPU", "Error available frequencies for $cluster", e); emit(emptyList()) }
                        .collect { tempAvailableFrequenciesMap[cluster] = it.sorted() }
                }
            }
        }
        _availableCpuFrequenciesPerClusterMap.value = tempAvailableFrequenciesMap
        if (tempAvailableGovernorsMap.isNotEmpty()) {
            _generalAvailableCpuGovernors.value = tempAvailableGovernorsMap.values.flatten().distinct().sorted()
        }
        Log.d("TuningVM_CPU", "Finished fetching all CPU data.")
    }

    fun getCpuGov(cluster: String): StateFlow<String> =
        _currentCpuGovernors.getOrPut(cluster) { MutableStateFlow("...") }.asStateFlow()

    fun getCpuFreq(cluster: String): StateFlow<Pair<Int, Int>> =
        _currentCpuFrequencies.getOrPut(cluster) { MutableStateFlow(0 to 0) }.asStateFlow()

    fun getAvailableCpuFrequencies(clusterName: String): StateFlow<List<Int>> =
        _availableCpuFrequenciesPerClusterMap.map { it[clusterName]?.sorted() ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun setCpuGov(cluster: String, gov: String) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_CPU_Set", "Set CPU gov $cluster to '$gov'")
        try {
            repo.setCpuGov(cluster, gov)
            _currentCpuGovernors[cluster]?.value = gov
        } catch (e: Exception) {
            Log.e("TuningVM_CPU_Set", "Error set CPU gov for $cluster to '$gov'", e)
            repo.getCpuGov(cluster).take(1).collect { _currentCpuGovernors[cluster]?.value = it }
        }
    }

    fun setCpuFreq(cluster: String, min: Int, max: Int) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_CPU_Set", "Set CPU freq $cluster to Min: $min, Max: $max")
        if (min <= 0 || max <= 0 || min > max) {
            Log.e("TuningVM_CPU_Set", "Invalid freq values for $cluster. Min: $min, Max: $max.")
            return@launch
        }
        try {
            repo.setCpuFreq(cluster, min, max)
            _currentCpuFrequencies[cluster]?.value = min to max
        } catch (e: Exception) {
            Log.e("TuningVM_CPU_Set", "Error set CPU freq for $cluster to $min-$max", e)
            repo.getCpuFreq(cluster).take(1).collect { _currentCpuFrequencies[cluster]?.value = it }
        }
    }

    // --- GPU Control Methods ---
    fun fetchGpuData() = viewModelScope.launch(Dispatchers.IO) {
        Log.d("TuningVM_GPU", "Fetching all GPU data...")
        try {
            repo.getAvailableGpuGovernors()
                .catch { e -> Log.e("TuningVM_GPU", "Error available GPU governors", e); emit(emptyList()) }
                .collect { _availableGpuGovernors.value = it.sorted() }

            repo.getGpuGov()
                .take(1)
                .catch { e -> Log.e("TuningVM_GPU", "Error current GPU gov", e); _currentGpuGovernor.value = "Error" }
                .collect { _currentGpuGovernor.value = it }

            repo.getAvailableGpuFrequencies()
                .catch { e -> Log.e("TuningVM_GPU", "Error available GPU frequencies", e); emit(emptyList()) }
                .collect { _availableGpuFrequencies.value = it.sorted() }

            repo.getGpuFreq()
                .take(1)
                .catch { e -> Log.e("TuningVM_GPU", "Error current GPU min/max freq", e); _currentGpuMinFreq.value = 0; _currentGpuMaxFreq.value = 0 }
                .collect { freqPair ->
                    _currentGpuMinFreq.value = freqPair.first
                    _currentGpuMaxFreq.value = freqPair.second
                }

            repo.getGpuPowerLevelRange()
                .catch { e -> Log.e("TuningVM_GPU", "Error GPU power level range", e); emit(0f to 0f) }
                .collect { _gpuPowerLevelRange.value = it }

            repo.getCurrentGpuPowerLevel()
                .catch { e -> Log.e("TuningVM_GPU", "Error current GPU power level", e); emit(0f) }
                .collect { _currentGpuPowerLevel.value = it }

        } catch (e: Exception) {
            Log.e("TuningVM_GPU", "Unhandled error in fetchGpuData", e)
        }
        Log.d("TuningVM_GPU", "Finished fetching GPU data.")
    }

    fun setGpuGovernor(gov: String) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_GPU_Set", "Attempting to set GPU governor to '$gov'")
        try {
            repo.setGpuGov(gov)
            _currentGpuGovernor.value = gov
        } catch (e: Exception) {
            Log.e("TuningVM_GPU_Set", "Error setting GPU governor to '$gov'", e)
            fetchGpuData() // Revert atau refresh
        }
    }

    fun setGpuMinFrequency(freqKHz: Int) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_GPU_Set", "Attempting to set GPU Min frequency to '$freqKHz' KHz")
        if (freqKHz <= 0 || (_currentGpuMaxFreq.value > 0 && freqKHz > _currentGpuMaxFreq.value)) {
            Log.w("TuningVM_GPU_Set", "Invalid Min GPU frequency: $freqKHz KHz. Max is ${_currentGpuMaxFreq.value} KHz")
            return@launch
        }
        try {
            repo.setGpuMinFreq(freqKHz) // Repo harus menerima KHz
            _currentGpuMinFreq.value = freqKHz
        } catch (e: Exception) {
            Log.e("TuningVM_GPU_Set", "Error setting GPU Min frequency to '$freqKHz' KHz", e)
            fetchGpuData()
        }
    }

    fun setGpuMaxFrequency(freqKHz: Int) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_GPU_Set", "Attempting to set GPU Max frequency to '$freqKHz' KHz")
        if (freqKHz <= 0 || freqKHz < _currentGpuMinFreq.value) {
            Log.w("TuningVM_GPU_Set", "Invalid Max GPU frequency: $freqKHz KHz. Min is ${_currentGpuMinFreq.value} KHz")
            return@launch
        }
        try {
            repo.setGpuMaxFreq(freqKHz) // Repo harus menerima KHz
            _currentGpuMaxFreq.value = freqKHz
        } catch (e: Exception) {
            Log.e("TuningVM_GPU_Set", "Error setting GPU Max frequency to '$freqKHz' KHz", e)
            fetchGpuData()
        }
    }

    fun setGpuPowerLevel(level: Float) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_GPU_Set", "Attempting to set GPU Power Level to '$level'")
        val range = _gpuPowerLevelRange.value
        if (level < range.first || level > range.second) {
            Log.w("TuningVM_GPU_Set", "Invalid GPU Power Level: $level. Range is $range")
            return@launch
        }
        try {
            repo.setGpuPowerLevel(level)
            _currentGpuPowerLevel.value = level
        } catch (e: Exception) {
            Log.e("TuningVM_GPU_Set", "Error setting GPU Power Level to '$level'", e)
            fetchGpuData()
        }
    }

    // --- OpenGLES Driver Methods ---
    internal fun fetchOpenGlesDriver() = viewModelScope.launch(Dispatchers.IO) { // Sudah IO
        Log.d("TuningVM_GLES", "Fetching OpenGLES Driver...")
        repo.getOpenGlesDriver()
            .catch { e -> Log.e("TuningVM_GLES", "Error getting OpenGLES Driver", e); _currentOpenGlesDriver.value = "Error" }
            .collect { _currentOpenGlesDriver.value = it.ifBlank { "N/A" } }
        Log.d("TuningVM_GLES", "Finished fetching OpenGLES Driver: ${_currentOpenGlesDriver.value}")
    }

    // --- GPU Renderer Methods ---
    internal fun fetchCurrentGpuRenderer() = viewModelScope.launch(Dispatchers.IO) { // Sudah IO
        Log.d("TuningVM_GPURenderer", "Fetching current GPU Renderer...")
        repo.getGpuRenderer()
            .catch { e -> Log.e("TuningVM_GPURenderer", "Error getting GPU Renderer", e); _currentGpuRenderer.value = "Error" }
            .collect { renderer -> _currentGpuRenderer.value = renderer.ifBlank { "Default" } }
        Log.d("TuningVM_GPURenderer", "Finished fetching GPU Renderer: ${_currentGpuRenderer.value}")
    }

    // --- Vulkan API Version Methods ---
    internal fun fetchVulkanApiVersion() = viewModelScope.launch(Dispatchers.IO) {
        Log.d("TuningVM_Vulkan", "Fetching Vulkan API Version...")
        repo.getVulkanApiVersion()
            .catch { e -> Log.e("TuningVM_Vulkan", "Error getting Vulkan API Version", e); _vulkanApiVersion.value = "Error" }
            .collect { _vulkanApiVersion.value = it.ifBlank { "N/A" } }
        Log.d("TuningVM_Vulkan", "Finished fetching Vulkan API Version: ${_vulkanApiVersion.value}")
    }


    fun userSelectedGpuRenderer(renderer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i("TuningVM_GPURenderer_Set", "Attempting to set GPU Renderer to '$renderer'")
            repo.setGpuRenderer(renderer).collect { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        _currentGpuRenderer.value = renderer
                        Log.d("TuningViewModel", "GPU Renderer set to $renderer. Prompting for reboot.")
                        _showRebootConfirmationDialog.value = true
                    } else {
                        Log.e("TuningViewModel", "Failed to set GPU Renderer to '$renderer'.")
                        _rebootCommandFeedback.emit("Gagal mengatur GPU Renderer.")
                        // Fetch ulang nilai sebenarnya jika gagal
                        launch(Dispatchers.IO) { fetchCurrentGpuRenderer() }
                    }
                }
            }
        }
    }


    fun confirmAndRebootDevice() {
        viewModelScope.launch(Dispatchers.Main) {
            _showRebootConfirmationDialog.value = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("TuningViewModel", "Reboot confirmed. Attempting to execute reboot command...")
            repo.rebootDevice().collect { success ->
                if (success) {
                    Log.i("TuningViewModel", "Reboot command sent successfully to repository.")
                } else {
                    Log.e("TuningViewModel", "Repository reported failure in sending reboot command.")
                    _rebootCommandFeedback.emit("Perintah reboot gagal dikirim.")
                }
            }
        }
    }

    fun cancelRebootConfirmation() {
        _showRebootConfirmationDialog.value = false
    }


    // --- Swappiness Control ---
    private suspend fun fetchCurrentSwappiness() {
        Log.d("TuningVM_Mem", "Fetching current swappiness...")
        repo.getSwappiness()
            .catch { e -> Log.e("TuningVM_Mem", "Error getting current swappiness", e); _swappiness.value = 60 } // Default on error
            .collect { _swappiness.value = it }
        Log.d("TuningVM_Mem", "Finished fetching swappiness: ${_swappiness.value}")
    }

    fun setSwappiness(value: Int) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_Mem_Set", "Attempting to set swappiness to $value")
        if (value !in 0..200) {
            Log.e("TuningVM_Mem_Set", "Invalid swappiness value: $value. Must be 0-200.")
            _rebootCommandFeedback.emit("Nilai Swappiness tidak valid (0-200).")
            return@launch
        }
        try {
            repo.setSwappiness(value)
            _swappiness.value = value
        } catch (e: Exception) {
            Log.e("TuningVM_Mem_Set", "Error setting swappiness to $value", e)
            fetchCurrentSwappiness() // Revert
        }
    }

    // --- Thermal Control Methods ---
    private fun fetchCurrentThermalMode(isInitialLoad: Boolean = false) {
        if (isInitialLoad) _isThermalLoading.value = true
        Log.d("TuningVM_Thermal", "Fetching current thermal mode. Initial: $isInitialLoad")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.getCurrentThermalModeIndex()
                    .catch { e ->
                        Log.e("TuningVM_Thermal", "Error get thermal mode index", e)
                        withContext(Dispatchers.Main) {
                            if (isInitialLoad) applyLastSavedThermalProfile() else _isThermalLoading.value = false
                        }
                    }
                    .collect { indexFromKernel ->
                        Log.d("TuningVM_Thermal", "Fetched thermal index: $indexFromKernel")
                        withContext(Dispatchers.Main) {
                            _currentThermalModeIndex.value = indexFromKernel
                            if (isInitialLoad) applyLastSavedThermalProfile() else _isThermalLoading.value = false
                        }
                    }
            } catch (e: Exception) {
                Log.e("TuningVM_Thermal", "Exception in fetchCurrentThermalMode: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isThermalLoading.value = false
                    if (isInitialLoad && _currentThermalModeIndex.value == -1) {
                        _currentThermalModeIndex.value = 0
                    }
                }
            }
        }
    }

    private suspend fun applyLastSavedThermalProfile() {
        val lastSavedIndex = thermalPrefs.getInt(KEY_LAST_APPLIED_THERMAL_INDEX, -1)
        Log.d("TuningVM_Thermal", "Apply last saved profile. Saved: $lastSavedIndex, Kernel: ${_currentThermalModeIndex.value}")

        if (lastSavedIndex != -1) {
            val profileToRestore = availableThermalProfiles.find { it.index == lastSavedIndex }
            if (profileToRestore != null) {

                if (_currentThermalModeIndex.value == -1 || _currentThermalModeIndex.value != lastSavedIndex) {
                    Log.i("TuningVM_Thermal", "Restoring thermal profile: ${profileToRestore.displayName}")
                    setThermalProfileInternal(profileToRestore, isRestoring = true)
                } else {
                    _isThermalLoading.value = false
                }
            } else {

                thermalPrefs.edit().remove(KEY_LAST_APPLIED_THERMAL_INDEX).apply()
                _isThermalLoading.value = false
            }
        } else {
            if (_currentThermalModeIndex.value == -1) _currentThermalModeIndex.value = 0 // Default ke "Disable"
            _isThermalLoading.value = false
        }
    }


    private suspend fun setThermalProfileInternal(profile: ThermalProfile, isRestoring: Boolean) {
        if (!isRestoring) _isThermalLoading.value = true

        withContext(Dispatchers.IO) {
            Log.d("TuningVM_Thermal_Set", "Internal set: ${profile.displayName}, Restoring: $isRestoring")
            repo.setThermalModeIndex(profile.index)
                .catch { e ->
                    Log.e("TuningVM_Thermal_Set", "Error set profile ${profile.displayName}", e)
                    withContext(Dispatchers.Main) { fetchCurrentThermalMode() }
                }
                .collect { success ->
                    withContext(Dispatchers.Main) {
                        if (success) {
                            _currentThermalModeIndex.value = profile.index
                            if (!isRestoring) {
                                thermalPrefs.edit().putInt(KEY_LAST_APPLIED_THERMAL_INDEX, profile.index).apply()
                            }
                        } else {
                            fetchCurrentThermalMode()
                        }

                        if (!isRestoring || !success) {
                            if (success && isRestoring) {
                            } else {
                                _isThermalLoading.value = false
                            }
                        }
                    }
                }
        }

        if (isRestoring) {
            val finalIndex = _currentThermalModeIndex.value
            val restoredSuccessfully = availableThermalProfiles.any { it.index == finalIndex && finalIndex == profile.index }
            if (restoredSuccessfully) _isThermalLoading.value = false

        }
    }


    fun setThermalProfile(profile: ThermalProfile) {
        Log.i("TuningVM_Thermal_Set", "User request set profile: ${profile.displayName}")
        viewModelScope.launch(Dispatchers.Main) {
            setThermalProfileInternal(profile, isRestoring = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("TuningVM_Lifecycle", "ViewModel onCleared")

    }
}

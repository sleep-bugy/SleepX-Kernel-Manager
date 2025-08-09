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
    private val KEY_LAST_APPLIED_THERMAL_INDEX = "last_applied_thermal_index"

    val cpuClusters = listOf("cpu0", "cpu4", "cpu7")

    /* ---------------- CPU ---------------- */
    private val _coreStates = MutableStateFlow(List(8) { true })
    val coreStates: StateFlow<List<Boolean>> = _coreStates.asStateFlow()

    private val _generalAvailableCpuGovernors = MutableStateFlow<List<String>>(emptyList())
    val generalAvailableCpuGovernors: StateFlow<List<String>> = _generalAvailableCpuGovernors.asStateFlow()

    private val _availableCpuFrequenciesPerClusterMap = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    private val _currentCpuGovernors = mutableMapOf<String, MutableStateFlow<String>>()
    private val _currentCpuFrequencies = mutableMapOf<String, MutableStateFlow<Pair<Int, Int>>>()

    /* ---------------- GPU ---------------- */
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

    /* ---------------- OpenGL / Vulkan / Renderer ---------------- */
    private val _currentOpenGlesDriver = MutableStateFlow("Loading...")
    val currentOpenGlesDriver: StateFlow<String> = _currentOpenGlesDriver.asStateFlow()

    private val _currentGpuRenderer = MutableStateFlow("Loading...")
    val currentGpuRenderer: StateFlow<String> = _currentGpuRenderer.asStateFlow()

    private val _vulkanApiVersion = MutableStateFlow("Loading...")
    val vulkanApiVersion: StateFlow<String> = _vulkanApiVersion.asStateFlow()

    val availableGpuRenderers = listOf(
        "Default", "OpenGL", "Vulkan", "ANGLE", "OpenGL (SKIA)", "Vulkan (SKIA)"
    )

    /* ---------------- Reboot dialog ---------------- */
    private val _showRebootConfirmationDialog = MutableStateFlow(false)
    val showRebootConfirmationDialog: StateFlow<Boolean> = _showRebootConfirmationDialog.asStateFlow()

    private val _rebootCommandFeedback = MutableSharedFlow<String>()
    val rebootCommandFeedback: SharedFlow<String> = _rebootCommandFeedback.asSharedFlow()

    /* ---------------- RAM Control ---------------- */
    private val _zramEnabled = MutableStateFlow(false)
    val zramEnabled: StateFlow<Boolean> = _zramEnabled.asStateFlow()

    private val _zramDisksize = MutableStateFlow(536870912L) // 512 MB default
    val zramDisksize: StateFlow<Long> = _zramDisksize.asStateFlow()

    private val _compressionAlgorithms = MutableStateFlow<List<String>>(emptyList())
    val compressionAlgorithms: StateFlow<List<String>> = _compressionAlgorithms.asStateFlow()

    private val _currentCompression = MutableStateFlow("")
    val currentCompression: StateFlow<String> = _currentCompression.asStateFlow()

    private val _swappiness = MutableStateFlow(60)
    val swappiness: StateFlow<Int> = _swappiness.asStateFlow()

    private val _dirtyRatio = MutableStateFlow(20)
    val dirtyRatio: StateFlow<Int> = _dirtyRatio.asStateFlow()

    private val _dirtyBackgroundRatio = MutableStateFlow(10)
    val dirtyBackgroundRatio: StateFlow<Int> = _dirtyBackgroundRatio.asStateFlow()

    private val _dirtyWriteback = MutableStateFlow(30)
    val dirtyWriteback: StateFlow<Int> = _dirtyWriteback.asStateFlow()

    private val _dirtyExpireCentisecs = MutableStateFlow(300)
    val dirtyExpireCentisecs: StateFlow<Int> = _dirtyExpireCentisecs.asStateFlow()

    private val _minFreeMemory = MutableStateFlow(128)
    val minFreeMemory: StateFlow<Int> = _minFreeMemory.asStateFlow()

    /* Max ZRAM otomatis 6 GB untuk 8 GB RAM */
    private val _maxZramSize = MutableStateFlow(repo.calculateMaxZramSize())
    val maxZramSize: StateFlow<Long> = _maxZramSize.asStateFlow()

    /* ---------------- Thermal ---------------- */
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

    val currentThermalProfileName: StateFlow<String> =
        _currentThermalModeIndex.map { idx ->
            availableThermalProfiles.find { it.index == idx }?.displayName
                ?: if (idx == -1 && _isThermalLoading.value) "Loading..."
                else if (idx == -1) "Disable"
                else "Unknown ($idx)"
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    /* ---------------- Init ---------------- */
    init {
        Log.d("TuningVM_Init", "ViewModel initializing...")
        initializeCpuStateFlows()
        fetchAllInitialData()
        refreshCoreStates()
        fetchRamControlData()
        Log.d("TuningVM_Init", "ViewModel initialization complete.")
    }

    /* ---------------- CPU ---------------- */
    private fun initializeCpuStateFlows() {
        Log.d("TuningVM_Init", "Initializing CPU StateFlows for clusters: $cpuClusters")
        cpuClusters.forEach { cluster ->
            _currentCpuGovernors.getOrPut(cluster) { MutableStateFlow("...") }
            _currentCpuFrequencies.getOrPut(cluster) { MutableStateFlow(0 to 0) }
        }
    }

    private suspend fun fetchAllCpuData() {
        Log.d("TuningVM_CPU", "Fetching all CPU data...")
        val tempGovernors = mutableMapOf<String, List<String>>()
        val tempFreqs = mutableMapOf<String, List<Int>>()

        cpuClusters.forEach { cluster ->
            coroutineScope {
                launch { repo.getCpuGov(cluster).take(1).collect { _currentCpuGovernors[cluster]?.value = it } }
                launch { repo.getCpuFreq(cluster).take(1).collect { _currentCpuFrequencies[cluster]?.value = it } }
                launch { repo.getAvailableCpuGovernors(cluster).collect { tempGovernors[cluster] = it } }
                launch { repo.getAvailableCpuFrequencies(cluster).collect { tempFreqs[cluster] = it } }
            }
        }
        _availableCpuFrequenciesPerClusterMap.value = tempFreqs
        if (tempGovernors.isNotEmpty()) _generalAvailableCpuGovernors.value = tempGovernors.values.flatten().distinct().sorted()
        Log.d("TuningVM_CPU", "Finished fetching all CPU data.")
    }

    fun getCpuGov(cluster: String): StateFlow<String> = _currentCpuGovernors.getOrPut(cluster) { MutableStateFlow("...") }.asStateFlow()
    fun getCpuFreq(cluster: String): StateFlow<Pair<Int, Int>> = _currentCpuFrequencies.getOrPut(cluster) { MutableStateFlow(0 to 0) }.asStateFlow()
    fun getAvailableCpuFrequencies(cluster: String): StateFlow<List<Int>> = _availableCpuFrequenciesPerClusterMap.map { it[cluster] ?: emptyList() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun setCpuGov(cluster: String, gov: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.setCpuGov(cluster, gov)
    }

    fun setCpuFreq(cluster: String, min: Int, max: Int) = viewModelScope.launch(Dispatchers.IO) {
        repo.setCpuFreq(cluster, min, max)
    }

    fun toggleCore(coreId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val newStates = _coreStates.value.toMutableList()
        val newState = !newStates[coreId]
        if (!newState && newStates.count { it } == 1) {
            _rebootCommandFeedback.emit("Setidaknya 1 core harus tetap online")
            return@launch
        }
        if (repo.setCoreOnline(coreId, newState)) {
            newStates[coreId] = newState
            _coreStates.value = newStates
        } else {
            Log.e("TuningVM_CPU", "Failed toggle core $coreId")
        }
    }

    fun refreshCoreStates() = viewModelScope.launch(Dispatchers.IO) {
        _coreStates.value = (0 until 8).map { repo.getCoreOnline(it) }
    }

    /* ---------------- GPU ---------------- */
    fun fetchGpuData() = viewModelScope.launch(Dispatchers.IO) {
        _availableGpuGovernors.value = repo.getAvailableGpuGovernors().first()
        _currentGpuGovernor.value = repo.getGpuGov().first()
        _availableGpuFrequencies.value = repo.getAvailableGpuFrequencies().first()
        val (min, max) = repo.getGpuFreq().first()
        _currentGpuMinFreq.value = min
        _currentGpuMaxFreq.value = max
        _gpuPowerLevelRange.value = repo.getGpuPowerLevelRange().first()
        _currentGpuPowerLevel.value = repo.getCurrentGpuPowerLevel().first()
    }

    fun setGpuGovernor(gov: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.setGpuGov(gov)
    }

    fun setGpuMinFrequency(freqKHz: Int) = viewModelScope.launch(Dispatchers.IO) {
        repo.setGpuMinFreq(freqKHz)
    }

    fun setGpuMaxFrequency(freqKHz: Int) = viewModelScope.launch(Dispatchers.IO) {
        repo.setGpuMaxFreq(freqKHz)
    }

    fun setGpuPowerLevel(level: Float) = viewModelScope.launch(Dispatchers.IO) {
        repo.setGpuPowerLevel(level)
    }

    /* ---------------- OpenGL / Vulkan ---------------- */
    internal fun fetchOpenGlesDriver() = viewModelScope.launch(Dispatchers.IO) {
        repo.getOpenGlesDriver().collect { _currentOpenGlesDriver.value = it }
    }

    internal fun fetchCurrentGpuRenderer() = viewModelScope.launch(Dispatchers.IO) {
        repo.getGpuRenderer().collect { _currentGpuRenderer.value = it }
    }

    internal fun fetchVulkanApiVersion() = viewModelScope.launch(Dispatchers.IO) {
        repo.getVulkanApiVersion().collect { _vulkanApiVersion.value = it }
    }

    fun userSelectedGpuRenderer(renderer: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.setGpuRenderer(renderer).collect { success ->
            if (success) {
                _currentGpuRenderer.value = renderer
                _showRebootConfirmationDialog.value = true
            } else {
                _rebootCommandFeedback.emit("Gagal mengatur GPU Renderer.")
            }
        }
    }

    fun confirmAndRebootDevice() {
        _showRebootConfirmationDialog.value = false
        viewModelScope.launch { repo.rebootDevice().collect { /* ignore, device reboot */ } }
    }

    fun cancelRebootConfirmation() {
        _showRebootConfirmationDialog.value = false
    }

    /* ---------------- RAM Control ---------------- */
    private fun fetchRamControlData() = viewModelScope.launch {
        launch(Dispatchers.IO) { repo.getZramEnabled().collect { _zramEnabled.value = it } }
        launch(Dispatchers.IO) { repo.getZramDisksize().collect { _zramDisksize.value = it } }
        launch(Dispatchers.IO) {
            repo.getCompressionAlgorithms().collect {
                _compressionAlgorithms.value = it
                // Ambil nilai kompresi saat ini setelah daftar algoritma tersedia
                repo.getCurrentCompression().firstOrNull()?.let { currentAlgo -> _currentCompression.value = currentAlgo }
            }
        }
        launch(Dispatchers.IO) { repo.getSwappiness().collect { _swappiness.value = it } }
        launch(Dispatchers.IO) { repo.getDirtyRatio().collect { _dirtyRatio.value = it } }
        launch(Dispatchers.IO) { repo.getDirtyBackgroundRatio().collect { _dirtyBackgroundRatio.value = it } }
        launch(Dispatchers.IO) { repo.getDirtyWriteback().collect { _dirtyWriteback.value = it } }
        launch(Dispatchers.IO) { repo.getDirtyExpireCentisecs().collect { _dirtyExpireCentisecs.value = it } }
        launch(Dispatchers.IO) { repo.getMinFreeMemory().collect { _minFreeMemory.value = it } }
    }

    fun setZramEnabled(enabled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repo.setZramEnabled(enabled).collect { _zramEnabled.value = it }
    }

    fun setZramDisksize(sizeBytes: Long) = viewModelScope.launch(Dispatchers.IO) {
        val max = repo.calculateMaxZramSize()
        if (sizeBytes < 512 * 1024 * 1024 || sizeBytes > max) {
            _rebootCommandFeedback.emit("Ukuran ZRAM tidak valid (512 MB - ${max / 1024 / 1024} MB)")
            return@launch
        }
        repo.resizeZramSafely(sizeBytes)
        repo.getZramDisksize().collect { _zramDisksize.value = it }
    }

    fun setCompression(algo: String) = viewModelScope.launch(Dispatchers.IO) {
        if (algo != _currentCompression.value) {
            try {
                Log.d("TuningVM_RAM", "Attempting to set compression to: $algo")
                repo.setCompressionAlgorithm(algo)
                val newAlgo = repo.getCurrentCompression().first()
                Log.d("TuningVM_RAM", "New currentCompression fetched after set: $newAlgo")
                _currentCompression.value = newAlgo
                Log.d("TuningVM_RAM", "_currentCompression.value after set: ${_currentCompression.value}")
            } catch (e: Exception) {
                Log.e("TuningVM_RAM", "Error setting or getting compression: $algo", e)
                // Mungkin coba refresh lagi atau emit error
                _currentCompression.value = "Error" // Atau nilai default yang jelas
            }
        } else {
            Log.d("TuningVM_RAM", "Skipping setCompression, algo is already: $algo")
        }
    }


    fun setSwappiness(value: Int) = viewModelScope.launch(Dispatchers.IO) { repo.setSwappiness(value) }
    fun setDirtyRatio(value: Int) = viewModelScope.launch(Dispatchers.IO) { repo.setDirtyRatio(value) }
    fun setDirtyBackgroundRatio(value: Int) = viewModelScope.launch(Dispatchers.IO) { repo.setDirtyBackgroundRatio(value) }
    fun setDirtyWriteback(value: Int) = viewModelScope.launch(Dispatchers.IO) { repo.setDirtyWriteback(value * 100) }
    fun setDirtyExpireCentisecs(value: Int) = viewModelScope.launch(Dispatchers.IO) { repo.setDirtyExpireCentisecs(value) }
    fun setMinFreeMemory(value: Int) = viewModelScope.launch(Dispatchers.IO) { repo.setMinFreeMemory(value * 1024) }

    /* ---------------- Thermal ---------------- */
    private fun fetchCurrentThermalMode(isInitialLoad: Boolean = false) {
        if (isInitialLoad) _isThermalLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            repo.getCurrentThermalModeIndex()
                .catch {
                    if (isInitialLoad) applyLastSavedThermalProfile() else _isThermalLoading.value = false
                }
                .collect {
                    _currentThermalModeIndex.value = it
                    if (isInitialLoad) applyLastSavedThermalProfile() else _isThermalLoading.value = false
                }
        }
    }

    private suspend fun applyLastSavedThermalProfile() {
        val idx = thermalPrefs.getInt(KEY_LAST_APPLIED_THERMAL_INDEX, -1)
        val profile = availableThermalProfiles.find { it.index == idx }
        if (profile != null && _currentThermalModeIndex.value != idx) {
            setThermalProfileInternal(profile, isRestoring = true)
        } else {
            _isThermalLoading.value = false
        }
    }

    private suspend fun setThermalProfileInternal(profile: ThermalProfile, isRestoring: Boolean) {
        if (!isRestoring) _isThermalLoading.value = true
        repo.setThermalModeIndex(profile.index).collect { ok ->
            if (ok) {
                _currentThermalModeIndex.value = profile.index
                if (!isRestoring) thermalPrefs.edit().putInt(KEY_LAST_APPLIED_THERMAL_INDEX, profile.index).apply()
            } else {
                fetchCurrentThermalMode()
            }
            _isThermalLoading.value = false
        }
    }

    fun setThermalProfile(profile: ThermalProfile) =
        viewModelScope.launch { setThermalProfileInternal(profile, isRestoring = false) }

    /* ---------------- Init ---------------- */
    private fun fetchAllInitialData() {
        viewModelScope.launch {
            launch(Dispatchers.IO) { fetchAllCpuData() }
            launch(Dispatchers.IO) { fetchGpuData() }
            fetchCurrentThermalMode(isInitialLoad = true)
            launch(Dispatchers.IO) { fetchOpenGlesDriver() }
            launch(Dispatchers.IO) { fetchCurrentGpuRenderer() }
            launch(Dispatchers.IO) { fetchVulkanApiVersion() }
            fetchRamControlData()
        }
    }
}
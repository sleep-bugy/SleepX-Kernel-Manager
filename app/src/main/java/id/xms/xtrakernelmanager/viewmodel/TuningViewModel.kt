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
    // Tidak diexpose langsung, gunakan fungsi getter di bawah

    private val _currentCpuGovernors = mutableMapOf<String, MutableStateFlow<String>>()
    private val _currentCpuFrequencies = mutableMapOf<String, MutableStateFlow<Pair<Int, Int>>>()

    // --- GPU States ---
    private val _availableGpuGovernors = MutableStateFlow<List<String>>(emptyList())
    val availableGpuGovernors: StateFlow<List<String>> = _availableGpuGovernors.asStateFlow()

    private val _currentGpuGovernor = MutableStateFlow("...")
    val currentGpuGovernor: StateFlow<String> = _currentGpuGovernor.asStateFlow()

    private val _currentGpuFrequency = MutableStateFlow(0 to 0) // Pair<Min, Max>
    val currentGpuFrequency: StateFlow<Pair<Int, Int>> = _currentGpuFrequency.asStateFlow()

    // --- Swappiness State ---
    private val _swappiness = MutableStateFlow(60) // Default value
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
        ThermalProfile("Balance Game", 50),
        ThermalProfile("YouTube", 51)
        // Tambahkan profil lain sesuai kebutuhan
    ).sortedBy { it.displayName }

    private val _isThermalLoading = MutableStateFlow(true)
    val isThermalLoading: StateFlow<Boolean> = _isThermalLoading.asStateFlow()

    private val _currentThermalModeIndex = MutableStateFlow(-1) // -1: Belum dimuat/Tidak Ditemukan/Default
    val currentThermalModeIndex: StateFlow<Int> = _currentThermalModeIndex.asStateFlow()

    val currentThermalProfileName: StateFlow<String> = _currentThermalModeIndex.map { currentIndex ->
        val profileName = availableThermalProfiles.find { it.index == currentIndex }?.displayName
        when {
            profileName != null -> profileName
            currentIndex == -1 && _isThermalLoading.value -> "Loading..."
            currentIndex == -1 -> "Disable" // Default jika tidak ada profil & tidak loading
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
            // Thermal fetch memiliki logika pemulihan sendiri, jadi dipanggil setelah yang lain
            // untuk memastikan state awal CPU/GPU sudah ada jika thermal bergantung padanya
            // (meskipun dalam kasus ini tidak secara langsung).
            fetchCurrentThermalMode(isInitialLoad = true)
        }
    }

    // --- CPU Control Methods ---
    private suspend fun fetchAllCpuData() { // Dibuat suspend agar bisa dipanggil dengan Dispatchers.IO
        Log.d("TuningVM_CPU", "Fetching all CPU data...")
        val tempAvailableGovernorsMap = mutableMapOf<String, List<String>>()
        val tempAvailableFrequenciesMap = mutableMapOf<String, List<Int>>()

        cpuClusters.forEach { cluster ->
            Log.d("TuningVM_CPU", "Fetching data for CPU cluster: $cluster")
            try {
                // Get current governor
                repo.getCpuGov(cluster)
                    .take(1)
                    .catch { e ->
                        Log.e("TuningVM_CPU", "Error getting current gov for $cluster", e)
                        _currentCpuGovernors[cluster]?.value = "Error"
                    }
                    .collect { currentGov ->
                        Log.d("TuningVM_CPU", "Current gov for $cluster: $currentGov")
                        _currentCpuGovernors[cluster]?.value = currentGov
                    }

                // Get current frequencies
                repo.getCpuFreq(cluster)
                    .take(1)
                    .catch { e ->
                        Log.e("TuningVM_CPU", "Error getting current freq for $cluster", e)
                        _currentCpuFrequencies[cluster]?.value = (0 to -1) // Error state
                    }
                    .collect { currentFreqPair ->
                        Log.d("TuningVM_CPU", "Current freq for $cluster: $currentFreqPair")
                        _currentCpuFrequencies[cluster]?.value = currentFreqPair
                    }

                // Get available governors
                repo.getAvailableCpuGovernors(cluster)
                    .catch { e ->
                        Log.e("TuningVM_CPU", "Error getting available governors for $cluster", e)
                        emit(emptyList())
                    }
                    .collect { governors ->
                        Log.d("TuningVM_CPU", "Available governors for $cluster: $governors")
                        tempAvailableGovernorsMap[cluster] = governors
                    }

                // Get available frequencies
                repo.getAvailableCpuFrequencies(cluster)
                    .catch { e ->
                        Log.e("TuningVM_CPU", "Error getting available frequencies for $cluster", e)
                        emit(emptyList())
                    }
                    .collect { frequencies ->
                        Log.d("TuningVM_CPU", "Available frequencies for $cluster: $frequencies")
                        tempAvailableFrequenciesMap[cluster] = frequencies.sorted()
                    }
            } catch (e: Exception) {
                Log.e("TuningVM_CPU", "Unhandled error processing cluster $cluster in fetchAllCpuData", e)
            }
        }
        _availableCpuFrequenciesPerClusterMap.value = tempAvailableFrequenciesMap
        if (tempAvailableGovernorsMap.isNotEmpty()) {
            val allGovs = tempAvailableGovernorsMap.values.flatten().distinct().sorted()
            Log.d("TuningVM_CPU", "General available CPU governors: $allGovs")
            _generalAvailableCpuGovernors.value = allGovs
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
        Log.i("TuningVM_CPU_Set", "Attempting to set CPU governor for $cluster to '$gov'")
        try {
            repo.setCpuGov(cluster, gov) // Asumsi repo.setCpuGov sudah benar
            _currentCpuGovernors[cluster]?.value = gov
            Log.i("TuningVM_CPU_Set", "Successfully called repo.setCpuGov for $cluster. UI updated to '$gov'.")

        } catch (e: Exception) {
            Log.e("TuningVM_CPU_Set", "Error setting CPU governor for $cluster to '$gov'", e)

        }
    }

    fun setCpuFreq(cluster: String, min: Int, max: Int) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_CPU_Set", "Attempting to set CPU freq for $cluster to Min: $min, Max: $max")
        if (min <= 0 || max <= 0 || min > max) {
            Log.e("TuningVM_CPU_Set", "Invalid frequency values for $cluster. Min: $min, Max: $max. Aborting set.")
            return@launch
        }
        try {
            repo.setCpuFreq(cluster, min, max)
            _currentCpuFrequencies[cluster]?.value = min to max
            Log.i("TuningVM_CPU_Set", "Successfully called repo.setCpuFreq for $cluster. UI updated to $min-$max.")

        } catch (e: Exception) {
            Log.e("TuningVM_CPU_Set", "Error setting CPU frequency for $cluster to $min-$max", e)

        }
    }

    // --- GPU Control Methods ---
    private suspend fun fetchGpuData() { // Dibuat suspend
        Log.d("TuningVM_GPU", "Fetching GPU data...")
        try {
            repo.getAvailableGpuGovernors()
                .catch { e -> Log.e("TuningVM_GPU", "Error getting available GPU governors", e); emit(emptyList()) }
                .collect { governors ->
                    Log.d("TuningVM_GPU", "Available GPU governors: $governors")
                    _availableGpuGovernors.value = governors.sorted()
                }

            repo.getGpuGov()
                .take(1)
                .catch { e -> Log.e("TuningVM_GPU", "Error getting current GPU gov", e); _currentGpuGovernor.value = "Error" }
                .collect { currentGov ->
                    Log.d("TuningVM_GPU", "Current GPU governor: $currentGov")
                    _currentGpuGovernor.value = currentGov
                }

            repo.getGpuFreq()
                .take(1)
                .catch { e -> Log.e("TuningVM_GPU", "Error getting current GPU freq", e); _currentGpuFrequency.value = (0 to -1) }
                .collect { currentFreq ->
                    Log.d("TuningVM_GPU", "Current GPU frequency: $currentFreq")
                    _currentGpuFrequency.value = currentFreq
                }
        } catch (e: Exception) {
            Log.e("TuningVM_GPU", "Unhandled error in fetchGpuData", e)
        }
        Log.d("TuningVM_GPU", "Finished fetching GPU data.")
    }

    fun setGpuGov(gov: String) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_GPU_Set", "Attempting to set GPU governor to '$gov'")
        try {
            repo.setGpuGov(gov)
            _currentGpuGovernor.value = gov
            Log.i("TuningVM_GPU_Set", "Successfully called repo.setGpuGov. UI updated to '$gov'.")
        } catch (e: Exception) {
            Log.e("TuningVM_GPU_Set", "Error setting GPU governor to '$gov'", e)

        }
    }

    fun setGpuFreq(min: Int, max: Int) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_GPU_Set", "Attempting to set GPU frequency to Min: $min, Max: $max")
        if (min < 0 || max < 0 || (min > 0 && max > 0 && min > max) ) { // min=0 atau max=0 bisa jadi valid untuk 'auto'
            Log.e("TuningVM_GPU_Set", "Invalid GPU frequency values. Min: $min, Max: $max. Aborting set.")
            // return@launch // Hati-hati, beberapa kernel mungkin menerima 0 sebagai 'auto'
        }
        try {
            repo.setGpuFreq(min, max)
            _currentGpuFrequency.value = min to max
            Log.i("TuningVM_GPU_Set", "Successfully called repo.setGpuFreq. UI updated to $min-$max.")
            // Opsional: Verifikasi
        } catch (e: Exception) {
            Log.e("TuningVM_GPU_Set", "Error setting GPU frequency to $min-$max", e)
            // Pertimbangkan untuk refresh GPU freq dari kernel
        }
    }

    // --- Swappiness Control ---
    private suspend fun fetchCurrentSwappiness() { // Dibuat suspend
        Log.d("TuningVM_Mem", "Fetching current swappiness...")
        try {
            repo.getSwappiness()
                .catch { e ->
                    Log.e("TuningVM_Mem", "Error getting current swappiness", e)
                }
                .collect { value ->
                    Log.d("TuningVM_Mem", "Current swappiness: $value")
                    _swappiness.value = value
                }
        } catch (e: Exception) {
            Log.e("TuningVM_Mem", "Unhandled error in fetchCurrentSwappiness", e)
        }
        Log.d("TuningVM_Mem", "Finished fetching swappiness.")
    }

    fun setSwappiness(value: Int) = viewModelScope.launch(Dispatchers.IO) {
        Log.i("TuningVM_Mem_Set", "Attempting to set swappiness to $value")
        if (value < 0 || value > 200) { // Batas umum untuk swappiness, bisa disesuaikan
            Log.e("TuningVM_Mem_Set", "Invalid swappiness value: $value. Must be between 0-200. Aborting.")
            return@launch
        }
        try {
            repo.setSwappiness(value)
            _swappiness.value = value
            Log.i("TuningVM_Mem_Set", "Successfully set swappiness to $value. UI updated.")
        } catch (e: Exception) {
            Log.e("TuningVM_Mem_Set", "Error setting swappiness to $value", e)
        }
    }

    // --- Thermal Control Methods ---
    private fun fetchCurrentThermalMode(isInitialLoad: Boolean = false) {
        if (isInitialLoad) {
            _isThermalLoading.value = true
        }
        Log.d("TuningVM_Thermal", "Fetching current thermal mode. Initial load: $isInitialLoad")
        viewModelScope.launch {
            repo.getCurrentThermalModeIndex()
                .catch { e ->
                    Log.e("TuningVM_Thermal", "Error getting current thermal mode index from repo", e)
                    if (isInitialLoad) {
                        applyLastSavedThermalProfile()
                    } else {
                        _isThermalLoading.value = false
                    }
                }
                .collect { indexFromKernel ->
                    Log.d("TuningVM_Thermal", "Fetched current thermal mode index from kernel: $indexFromKernel")
                    _currentThermalModeIndex.value = indexFromKernel
                    if (isInitialLoad) {
                        applyLastSavedThermalProfile()
                    } else {
                        _isThermalLoading.value = false
                    }
                }
        }
    }

    private fun applyLastSavedThermalProfile() {
        val lastSavedIndex = thermalPrefs.getInt(KEY_LAST_APPLIED_THERMAL_INDEX, -1)
        Log.d("TuningVM_Thermal", "Attempting to apply last saved thermal profile. Saved index: $lastSavedIndex, Current kernel index: ${_currentThermalModeIndex.value}")

        if (lastSavedIndex != -1) {
            val profileToApply = availableThermalProfiles.find { it.index == lastSavedIndex }
            if (profileToApply != null) {
                if (_currentThermalModeIndex.value == -1 || _currentThermalModeIndex.value != lastSavedIndex) {
                    Log.i("TuningVM_Thermal", "Applying last saved thermal profile: ${profileToApply.displayName} (Index: ${profileToApply.index}) as it differs from kernel or kernel read failed.")
                    setThermalProfileInternal(profileToApply, isRestoring = true)
                } else {
                    Log.i("TuningVM_Thermal", "Last saved profile (${profileToApply.displayName}) matches current kernel value (${_currentThermalModeIndex.value}). No re-apply needed.")
                    _isThermalLoading.value = false // Selesai loading
                }
            } else {
                Log.w("TuningVM_Thermal", "Last saved thermal index $lastSavedIndex not found in available profiles. Removing invalid preference.")
                thermalPrefs.edit().remove(KEY_LAST_APPLIED_THERMAL_INDEX).apply()
                _isThermalLoading.value = false // Selesai loading
            }
        } else {
            Log.i("TuningVM_Thermal", "No last saved thermal profile found.")
            _isThermalLoading.value = false // Selesai loading
        }
    }

    private fun setThermalProfileInternal(profile: ThermalProfile, isRestoring: Boolean) {
        if (!isRestoring) { // Hanya set loading true jika ini aksi pengguna, bukan pemulihan otomatis
            _isThermalLoading.value = true
        }
        Log.d("TuningVM_Thermal_Set", "Internal setThermalProfile: ${profile.displayName} (Index: ${profile.index}), isRestoring: $isRestoring")

        viewModelScope.launch { // Sebaiknya operasi repo di IO dispatcher jika belum
            repo.setThermalModeIndex(profile.index) // setThermalModeIndex di repo sudah flowOn(Dispatchers.IO)
                .catch { e ->
                    Log.e("TuningVM_Thermal_Set", "Error setting thermal profile via repo for ${profile.displayName}", e)
                    // Jika gagal, coba refresh dari kernel untuk mendapatkan state yang sebenarnya
                    fetchCurrentThermalMode() // Ini akan mengatur _isThermalLoading.value = false pada akhirnya
                }
                .collect { success ->
                    if (success) {
                        Log.i("TuningVM_Thermal_Set", "Successfully set thermal profile to ${profile.displayName} via repo.")
                        _currentThermalModeIndex.value = profile.index
                        if (!isRestoring) { // Hanya simpan jika ini adalah aksi pengguna, bukan pemulihan
                            thermalPrefs.edit().putInt(KEY_LAST_APPLIED_THERMAL_INDEX, profile.index).apply()
                            Log.i("TuningVM_Thermal_Set", "Saved thermal profile: ${profile.displayName} to SharedPreferences.")
                        }
                    } else {
                        Log.e("TuningVM_Thermal_Set", "Failed to set thermal profile to ${profile.displayName} via repo (repo returned false).")
                        // Jika gagal, refresh dari kernel untuk mendapatkan state yang sebenarnya
                        fetchCurrentThermalMode()
                    }
                    // _isThermalLoading akan diatur false oleh fetchCurrentThermalMode atau applyLastSavedThermalProfile
                    // jika ini adalah akhir dari initial load, atau jika ini adalah set manual yang berhasil dan tidak memicu fetch ulang.
                    // Jika ini set manual yang berhasil dan tidak memicu fetch, loading harus di set false.
                    if (!isRestoring || !success) {
                        _isThermalLoading.value = false
                    }
                }
        }
    }

    fun setThermalProfile(profile: ThermalProfile) {
        Log.i("TuningVM_Thermal_Set", "User requested to set thermal profile to: ${profile.displayName} (Index: ${profile.index})")
        setThermalProfileInternal(profile, isRestoring = false)
    }

    // Helper untuk refresh data satu cluster CPU jika diperlukan (misalnya setelah error set)
    // fun fetchSingleClusterData(cluster: String) = viewModelScope.launch(Dispatchers.IO) {
    //     Log.d("TuningVM_CPU", "Refreshing data for single CPU cluster: $cluster")
    //     try {
    //         repo.getCpuGov(cluster).take(1).collect { _currentCpuGovernors[cluster]?.value = it }
    //         repo.getCpuFreq(cluster).take(1).collect { _currentCpuFrequencies[cluster]?.value = it }
    //     } catch (e: Exception) {
    //         Log.e("TuningVM_CPU", "Error refreshing data for cluster $cluster", e)
    //         _currentCpuGovernors[cluster]?.value = "Error"
    //         _currentCpuFrequencies[cluster]?.value = (0 to -1)
    //     }
    // }

    override fun onCleared() {
        super.onCleared()
        Log.d("TuningVM_Lifecycle", "ViewModel onCleared")
        // Tidak ada pembatalan coroutine manual yang diperlukan karena viewModelScope menangani ini.
    }
}

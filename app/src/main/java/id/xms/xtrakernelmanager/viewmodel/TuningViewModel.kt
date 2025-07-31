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
    private val application: Application, // Inject Application
    private val repo: TuningRepository
) : AndroidViewModel(application) { // Warisi dari AndroidViewModel

    // SharedPreferences untuk persistensi Thermal Profile
    private val thermalPrefs: SharedPreferences by lazy {
        application.getSharedPreferences("thermal_settings_prefs", Context.MODE_PRIVATE)
    }
    @Suppress("PrivatePropertyName")
    private val KEY_LAST_APPLIED_THERMAL_INDEX = "last_applied_thermal_index"

    val cpuClusters = listOf("cpu0", "cpu4", "cpu7")

    private val _generalAvailableCpuGovernors = MutableStateFlow<List<String>>(emptyList())
    val generalAvailableCpuGovernors: StateFlow<List<String>> = _generalAvailableCpuGovernors.asStateFlow()

    private val _availableCpuFrequenciesPerClusterMap = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    private val _currentCpuGovernors = mutableMapOf<String, MutableStateFlow<String>>()
    private val _currentCpuFrequencies = mutableMapOf<String, MutableStateFlow<Pair<Int, Int>>>()

    private val _availableGpuGovernors = MutableStateFlow<List<String>>(emptyList())
    val availableGpuGovernors: StateFlow<List<String>> = _availableGpuGovernors.asStateFlow()

    private val _currentGpuGovernor = MutableStateFlow("...")
    val currentGpuGovernor: StateFlow<String> = _currentGpuGovernor.asStateFlow()

    private val _currentGpuFrequency = MutableStateFlow(0 to 0)
    val currentGpuFrequency: StateFlow<Pair<Int, Int>> = _currentGpuFrequency.asStateFlow()

    private val _swappiness = MutableStateFlow(60)
    val swappiness: StateFlow<Int> = _swappiness.asStateFlow()

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
    ).sortedBy { it.displayName }

    // Indikator loading khusus untuk operasi thermal
    private val _isThermalLoading = MutableStateFlow(true)
    val isThermalLoading: StateFlow<Boolean> = _isThermalLoading.asStateFlow()

    private val _currentThermalModeIndex = MutableStateFlow(-1) // -1: Belum dimuat/Tidak Ditemukan/Default
    val currentThermalModeIndex: StateFlow<Int> = _currentThermalModeIndex.asStateFlow()

    val currentThermalProfileName: StateFlow<String> = _currentThermalModeIndex.map { currentIndex ->
        availableThermalProfiles.find { it.index == currentIndex }?.displayName
            ?: if (currentIndex == -1 && _isThermalLoading.value) "Loading..."
            else if (currentIndex == -1) "Disable" // Atau state default lain jika tidak loading
            else "Unknown ($currentIndex)"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")


    init {
        initializeCpuStateFlows()
        fetchAllCpuData()
        fetchGpuData()
        fetchCurrentSwappiness()
        // Urutan penting: fetch dulu, baru coba apply yang disimpan
        fetchCurrentThermalMode(isInitialLoad = true)
    }

    private fun initializeCpuStateFlows() {
        cpuClusters.forEach { cluster ->
            _currentCpuGovernors.getOrPut(cluster) { MutableStateFlow("...") }
            _currentCpuFrequencies.getOrPut(cluster) { MutableStateFlow(0 to 0) }
        }
    }

    private fun fetchAllCpuData() {
        viewModelScope.launch(Dispatchers.IO) {
            val tempAvailableGovernorsMap = mutableMapOf<String, List<String>>()
            val tempAvailableFrequenciesMap = mutableMapOf<String, List<Int>>()

            cpuClusters.forEach { cluster ->
                try {
                    repo.getCpuGov(cluster)
                        .take(1)
                        .catch { e -> Log.e("TuningVM", "Error getting current gov for $cluster", e); _currentCpuGovernors[cluster]?.value = "Error" }
                        .collect { currentGov -> _currentCpuGovernors[cluster]?.value = currentGov }

                    repo.getCpuFreq(cluster)
                        .take(1)
                        .catch { e -> Log.e("TuningVM", "Error getting current freq for $cluster", e); _currentCpuFrequencies[cluster]?.value = (0 to -1) }
                        .collect { currentFreqPair -> _currentCpuFrequencies[cluster]?.value = currentFreqPair }

                    repo.getAvailableCpuGovernors(cluster)
                        .catch { emit(emptyList()) }
                        .collect { governors -> tempAvailableGovernorsMap[cluster] = governors }

                    repo.getAvailableCpuFrequencies(cluster)
                        .catch { emit(emptyList()) }
                        .collect { frequencies -> tempAvailableFrequenciesMap[cluster] = frequencies.sorted() }
                } catch (e: Exception) {
                    Log.e("TuningVM", "Error processing cluster $cluster in fetchAllCpuData", e)
                }
            }
            _availableCpuFrequenciesPerClusterMap.value = tempAvailableFrequenciesMap
            if (tempAvailableGovernorsMap.isNotEmpty()) {
                _generalAvailableCpuGovernors.value = tempAvailableGovernorsMap.values.flatten().distinct().sorted()
            }
        }
    }

    fun getCpuGov(cluster: String): StateFlow<String> = _currentCpuGovernors.getOrPut(cluster) { MutableStateFlow("...") }.asStateFlow()
    fun getCpuFreq(cluster: String): StateFlow<Pair<Int, Int>> = _currentCpuFrequencies.getOrPut(cluster) { MutableStateFlow(0 to 0) }.asStateFlow()
    fun getAvailableCpuFrequencies(clusterName: String): StateFlow<List<Int>> =
        _availableCpuFrequenciesPerClusterMap.map { it[clusterName]?.sorted() ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCpuGov(cluster: String, gov: String) = viewModelScope.launch(Dispatchers.IO) { /* ... */ }
    fun setCpuFreq(cluster: String, min: Int, max: Int) = viewModelScope.launch(Dispatchers.IO) { /* ... */ }

    private fun fetchGpuData() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getAvailableGpuGovernors()
                .catch { e -> Log.e("TuningVM", "Error getting available GPU governors", e); emit(emptyList()) }
                .collect { governors -> _availableGpuGovernors.value = governors.sorted() }

            repo.getGpuGov()
                .take(1)
                .catch { e -> Log.e("TuningVM", "Error getting current GPU gov", e); _currentGpuGovernor.value = "Error" }
                .collect { currentGov -> _currentGpuGovernor.value = currentGov }

            repo.getGpuFreq()
                .take(1)
                .catch { e -> Log.e("TuningVM", "Error getting current GPU freq", e); _currentGpuFrequency.value = (0 to -1) }
                .collect { currentFreq -> _currentGpuFrequency.value = currentFreq }
        }
    }

    fun setGpuGov(gov: String) = viewModelScope.launch(Dispatchers.IO) { /* ... */ }
    fun setGpuFreq(min: Int, max: Int) = viewModelScope.launch(Dispatchers.IO) { /* ... */ }

    private fun fetchCurrentSwappiness() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getSwappiness()
                .catch { e -> Log.e("TuningVM", "Error getting current swappiness", e) }
                .collect { value -> _swappiness.value = value }
        }
    }

    fun setSwappiness(value: Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repo.setSwappiness(value)
            _swappiness.value = value
        } catch (e: Exception) {
            Log.e("TuningVM", "Error setting swappiness to $value", e)
        }
    }

    // --- Thermal Control Methods ---
    private fun fetchCurrentThermalMode(isInitialLoad: Boolean = false) {
        if (isInitialLoad) {
            _isThermalLoading.value = true
        }
        Log.d("TuningVM", "Fetching current thermal mode. Initial load: $isInitialLoad")
        viewModelScope.launch { // Coroutine utama untuk operasi thermal
            repo.getCurrentThermalModeIndex()
                .catch { e ->
                    Log.e("TuningVM", "Error getting current thermal mode index from repo", e)
                    // Jika gagal fetch, _currentThermalModeIndex tetap pada nilai sebelumnya atau -1 (default)
                    // Pertimbangkan untuk tidak mengubahnya jika sudah ada nilai valid dari applyLastSaved
                    // atau set ke nilai default yang jelas menunjukkan error jika perlu.
                    // Untuk sekarang, kita biarkan saja agar applyLastSavedThermalProfile bisa mencoba.
                    if (isInitialLoad) {
                        // Jika ini adalah load awal dan gagal, coba terapkan yang disimpan
                        applyLastSavedThermalProfile()
                    } else {
                        // Jika bukan load awal (misalnya refresh setelah gagal set), set loading false
                        _isThermalLoading.value = false
                    }
                }
                .collect { indexFromKernel ->
                    Log.d("TuningVM", "Fetched current thermal mode index from kernel: $indexFromKernel")
                    _currentThermalModeIndex.value = indexFromKernel
                    if (isInitialLoad) {
                        applyLastSavedThermalProfile() // Panggil setelah fetch berhasil
                    } else {
                        _isThermalLoading.value = false // Selesai loading jika ini adalah refresh
                    }
                }
        }
    }

    private fun applyLastSavedThermalProfile() {
        val lastSavedIndex = thermalPrefs.getInt(KEY_LAST_APPLIED_THERMAL_INDEX, -1)
        Log.d("TuningVM", "Attempting to apply last saved thermal profile. Saved index: $lastSavedIndex, Current kernel index: ${_currentThermalModeIndex.value}")

        if (lastSavedIndex != -1) {
            val profileToApply = availableThermalProfiles.find { it.index == lastSavedIndex }
            if (profileToApply != null) {
                // Hanya terapkan jika berbeda dari yang saat ini ada di kernel ATAU jika kernel gagal dibaca (-1)
                // Ini untuk menghindari penerapan ulang yang tidak perlu jika sudah sinkron
                if (_currentThermalModeIndex.value == -1 || _currentThermalModeIndex.value != lastSavedIndex) {
                    Log.i("TuningVM", "Applying last saved thermal profile: ${profileToApply.displayName} (Index: ${profileToApply.index}) as it differs from kernel or kernel read failed.")
                    // Panggil setThermalProfile internal dengan flag isRestoring
                    setThermalProfileInternal(profileToApply, isRestoring = true)
                } else {
                    Log.i("TuningVM", "Last saved profile (${profileToApply.displayName}) matches current kernel value (${_currentThermalModeIndex.value}). No re-apply needed.")
                    _isThermalLoading.value = false // Selesai loading
                }
            } else {
                Log.w("TuningVM", "Last saved thermal index $lastSavedIndex not found in available profiles.")
                thermalPrefs.edit().remove(KEY_LAST_APPLIED_THERMAL_INDEX).apply() // Hapus preferensi yang tidak valid
                _isThermalLoading.value = false // Selesai loading
            }
        } else {
            Log.i("TuningVM", "No last saved thermal profile found.")
            _isThermalLoading.value = false // Selesai loading
        }
    }

    // Fungsi internal untuk menghindari pemanggilan _isThermalLoading.value = true saat restore
    private fun setThermalProfileInternal(profile: ThermalProfile, isRestoring: Boolean) {
        if (!isRestoring) {
            _isThermalLoading.value = true
        }
        Log.d("TuningVM", "Internal setThermalProfile: ${profile.displayName} (Index: ${profile.index}), isRestoring: $isRestoring")

        viewModelScope.launch {
            repo.setThermalModeIndex(profile.index)
                .catch { e ->
                    Log.e("TuningVM", "Error setting thermal profile via repo", e)
                    // Jika gagal, refresh dari kernel
                    fetchCurrentThermalMode() // Ini akan mengatur _isThermalLoading.value = false pada akhirnya
                }
                .collect { success ->
                    if (success) {
                        _currentThermalModeIndex.value = profile.index
                        if (!isRestoring) { // Hanya simpan jika ini adalah aksi pengguna, bukan pemulihan
                            thermalPrefs.edit().putInt(KEY_LAST_APPLIED_THERMAL_INDEX, profile.index).apply()
                            Log.i("TuningVM", "Successfully set and saved thermal profile: ${profile.displayName}")
                        } else {
                            Log.i("TuningVM", "Successfully restored thermal profile: ${profile.displayName}")
                        }
                    } else {
                        Log.e("TuningVM", "Failed to set thermal profile to ${profile.displayName} via repo.")
                        // Jika gagal, refresh dari kernel untuk mendapatkan state yang sebenarnya
                        fetchCurrentThermalMode()
                    }
                    // _isThermalLoading akan diatur false oleh fetchCurrentThermalMode atau applyLastSaved jika ini adalah akhir dari initial load
                    // atau langsung jika ini adalah set manual yang berhasil/gagal dan tidak memicu fetch ulang.
                    if (!isRestoring || !success) { // Jika ini set manual atau restore gagal, pastikan loading selesai.
                        _isThermalLoading.value = false
                    }
                }
        }
    }

    // Fungsi publik yang dipanggil oleh UI
    fun setThermalProfile(profile: ThermalProfile) {
        setThermalProfileInternal(profile, isRestoring = false)
    }
}

package il.ronmad.speedruntimer.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import il.ronmad.speedruntimer.ui.util.Event
import il.ronmad.speedruntimer.web.SplitsIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SplitsIOViewModel : ViewModel() {

    // Import
    private val _importedRun = MutableLiveData<SplitsIO.Run>()
    val importedRun: LiveData<Event<SplitsIO.Run>> = Transformations.map(_importedRun) { Event(it) }

    private val _progressBar = MutableLiveData<Boolean>()
    val progressBar: LiveData<Boolean>
        get() = _progressBar

    private val _toast = MutableLiveData<String>()
    val toast: LiveData<Event<String>> = Transformations.map(_toast) { Event(it) }

    // Export
    private val _claimUri = MutableLiveData<String>()
    val claimUri: LiveData<Event<String>> = Transformations.map(_claimUri) { Event(it) }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    fun importRun(id: String) = scope.launch {
        _progressBar.value = true
        val run = SplitsIO().getRun(id)
        _importedRun.postValue(run)
        _toast.value = if (run == SplitsIO.Run.EMPTY_RUN) "Import failed"
        else "Splits imported successfully"
        _progressBar.value = false
    }

    fun exportRun(run: SplitsIO.Run) = scope.launch {
        val uri = SplitsIO().uploadRun(run)
        _claimUri.postValue(uri)
        if (uri.isEmpty()) {
            _toast.value = "Upload failed"
        }
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}
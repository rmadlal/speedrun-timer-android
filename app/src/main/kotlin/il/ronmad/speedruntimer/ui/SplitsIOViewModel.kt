package il.ronmad.speedruntimer.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import il.ronmad.speedruntimer.ui.util.Event
import il.ronmad.speedruntimer.web.Failure
import il.ronmad.speedruntimer.web.SplitsIO
import il.ronmad.speedruntimer.web.Success
import kotlinx.coroutines.*
import java.io.IOException

class SplitsIOViewModel : ViewModel() {

    // Import
    private val _importedRun = MutableLiveData<SplitsIO.Run>()
    val importedRun: LiveData<Event<SplitsIO.Run>> = Transformations.map(_importedRun) { Event(it) }

    private val _progressBar = MutableLiveData<Boolean>()
    val progressBar: LiveData<Boolean>
        get() = _progressBar

    private val _toast = MutableLiveData<SplitsIOToast>()
    val toast: LiveData<Event<SplitsIOToast>> = Transformations.map(_toast) { Event(it) }

    // Export
    private val _claimUri = MutableLiveData<String>()
    val claimUri: LiveData<Event<String>> = Transformations.map(_claimUri) { Event(it) }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    fun importRun(id: String) = scope.launch {
        try {
            _progressBar.value = true
            when (val result = SplitsIO().getRun(id)) {
                is Success -> {
                    _importedRun.postValue(result.value)
                    _toast.value = ToastImportSuccess
                }
                is Failure -> _toast.value = ToastImportFail
            }
        } catch (e: IOException) {
            delay(1000)
            _toast.value = ToastIOError
        } finally {
            _progressBar.value = false
        }
    }

    fun exportRun(run: SplitsIO.Run) = scope.launch {
        try {
            when (val result = SplitsIO().uploadRun(run)) {
                is Success -> _claimUri.postValue(result.value)
                is Failure -> _toast.value = ToastExportFail
            }
        } catch (e: IOException) {
            delay(1000)
            _toast.value = ToastIOError
        }
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}

sealed class SplitsIOToast(val message: String)
object ToastImportSuccess : SplitsIOToast("Splits imported successfully")
object ToastImportFail : SplitsIOToast("Import failed")
object ToastExportFail : SplitsIOToast("Upload failed")
object ToastIOError : SplitsIOToast("Connection error")

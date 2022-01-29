package il.ronmad.speedruntimer.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import il.ronmad.speedruntimer.ui.util.Event
import il.ronmad.speedruntimer.web.Failure
import il.ronmad.speedruntimer.web.SplitsIO
import il.ronmad.speedruntimer.web.Success
import kotlinx.coroutines.*
import java.io.IOException

class SplitsIOViewModel : ViewModel(), CoroutineScope by MainScope() {

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

    fun importRun(id: String) = launch {
        try {
            _progressBar.value = true
            val result = withContext(Dispatchers.IO) {
                SplitsIO().getRun(id)
            }
            when (result) {
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

    fun exportRun(run: SplitsIO.Run) = launch {
        try {
            val result = withContext(Dispatchers.IO) {
                SplitsIO().uploadRun(run)
            }
            when (result) {
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
        cancel()
    }
}

sealed class SplitsIOToast(val message: String)
object ToastImportSuccess : SplitsIOToast("Splits imported successfully")
object ToastImportFail : SplitsIOToast("Import failed")
object ToastExportFail : SplitsIOToast("Upload failed")
object ToastIOError : SplitsIOToast("Connection error")

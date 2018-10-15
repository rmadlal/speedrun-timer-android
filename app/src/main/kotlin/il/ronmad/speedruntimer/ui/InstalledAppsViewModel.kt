package il.ronmad.speedruntimer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import il.ronmad.speedruntimer.MyApplication
import il.ronmad.speedruntimer.ui.util.Event
import kotlinx.coroutines.*

/**
 * Getting the list of apps that are installed on the system takes time (PackageManager is slow),
 * so it must be done in a background thread to get a fast app startup time.
 * This ViewModel is for MainActivity. It both abstracts coroutines away from the activity,
 * and allows the activity to process the result of the calculation in the main thread
 * (necessary because it involves work with Realm).
 * The installed apps data is in MyApplication so that it can be accessible from any timer launch
 *  - from the app, from the Widget, etc.
 */
class InstalledAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val _setupDone = MutableLiveData<Boolean>()
    val setupDone: LiveData<Event<Boolean>> = Transformations.map(_setupDone) { Event(it) }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    fun setupInstalledAppsMap() = scope.launch {
        withContext(Dispatchers.Default) {
            getApplication<MyApplication>().setupInstalledAppsMap()
        }
        _setupDone.postValue(true)
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}

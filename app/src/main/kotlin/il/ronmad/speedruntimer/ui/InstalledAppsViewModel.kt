package il.ronmad.speedruntimer.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import il.ronmad.speedruntimer.MyApplication
import il.ronmad.speedruntimer.ui.util.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    private val scope = CoroutineScope(Dispatchers.Default + job)

    fun setupInstalledAppsMap() = scope.launch {
        getApplication<MyApplication>().setupInstalledAppsMap()
        _setupDone.postValue(true)
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}

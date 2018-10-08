package il.ronmad.speedruntimer.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import il.ronmad.speedruntimer.ui.util.Event
import il.ronmad.speedruntimer.web.Src
import il.ronmad.speedruntimer.web.SrcLeaderboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

class GameInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _leaderboards = MutableLiveData<List<SrcLeaderboard>>()
    val leaderboards: LiveData<List<SrcLeaderboard>>
        get() = _leaderboards

    private val _refreshSpinner = MutableLiveData<Boolean>()
    val refreshSpinner: LiveData<Boolean>
        get() = _refreshSpinner

    private val _toast = MutableLiveData<String>()
    val toast: LiveData<Event<String>> = Transformations.map(_toast) { Event(it) }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    fun refreshInfo(gameName: String) = scope.launch {
        try {
            _refreshSpinner.value = true
            val data = Src(getApplication()).fetchLeaderboardsForGame(gameName)
            _leaderboards.postValue(data)
            if (data.isEmpty()) {
                _toast.value = ToastMsg.FETCH_FAIL()
            }
        } catch (e: IOException) {
            _leaderboards.postValue(emptyList())
            _toast.value = ToastMsg.FETCH_FAIL()
        } finally {
            _refreshSpinner.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    enum class ToastMsg(private val message: String) {
        FETCH_FAIL("No data available");

        operator fun invoke() = message
    }
}

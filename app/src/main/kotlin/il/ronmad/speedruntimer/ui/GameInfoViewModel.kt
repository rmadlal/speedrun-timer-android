package il.ronmad.speedruntimer.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import il.ronmad.speedruntimer.web.Src
import il.ronmad.speedruntimer.web.SrcLeaderboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GameInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _leaderboards = MutableLiveData<List<SrcLeaderboard>>()
    val leaderboards: LiveData<List<SrcLeaderboard>>
        get() = _leaderboards

    private val _refreshSpinner = MutableLiveData<Boolean>()
    val refreshSpinner: LiveData<Boolean>
        get() = _refreshSpinner

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    fun refreshInfo(gameName: String) = scope.launch {
        _refreshSpinner.value = true
        _leaderboards.postValue(Src(getApplication()).fetchLeaderboardsForGame(gameName))
        _refreshSpinner.value = false
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}

package il.ronmad.speedruntimer.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import il.ronmad.speedruntimer.ui.util.Event
import il.ronmad.speedruntimer.web.Failure
import il.ronmad.speedruntimer.web.Src
import il.ronmad.speedruntimer.web.SrcLeaderboard
import il.ronmad.speedruntimer.web.Success
import kotlinx.coroutines.*
import java.io.IOException

class GameInfoViewModel : ViewModel(), CoroutineScope by MainScope() {

    private val _leaderboards = MutableLiveData<List<SrcLeaderboard>>()
    val leaderboards: LiveData<List<SrcLeaderboard>>
        get() = _leaderboards

    private val _refreshSpinner = MutableLiveData<Boolean>()
    val refreshSpinner: LiveData<Boolean>
        get() = _refreshSpinner

    private val _toast = MutableLiveData<GameInfoToast>()
    val toast: LiveData<Event<GameInfoToast>> = Transformations.map(_toast) { Event(it) }

    fun refreshInfo(gameName: String) = launch {
        try {
            _refreshSpinner.value = true
            val result = withContext(Dispatchers.IO) {
                Src().fetchLeaderboardsForGame(gameName)
            }
            when (result) {
                is Success -> {
                    _leaderboards.postValue(result.value)
                    if (result.value.isEmpty()) {
                        _toast.value = ToastFetchEmpty
                    }
                }
                is Failure -> _toast.value = ToastFetchEmpty
            }
        } catch (e: IOException) {
            delay(1000)
            _toast.value = ToastFetchError
        } finally {
            _refreshSpinner.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancel()
    }
}

sealed class GameInfoToast(val message: String)
object ToastFetchEmpty : GameInfoToast("No info available for this game")
object ToastFetchError : GameInfoToast("Connection error")

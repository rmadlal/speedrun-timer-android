package il.ronmad.speedruntimer.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import il.ronmad.speedruntimer.*
import il.ronmad.speedruntimer.databinding.ActivityMainBinding
import il.ronmad.speedruntimer.fragments.GamesListFragment
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.gameExists
import il.ronmad.speedruntimer.ui.InstalledAppsViewModel
import io.realm.Realm
import io.realm.exceptions.RealmException

class MainActivity : AppCompatActivity() {

    internal lateinit var viewBinding: ActivityMainBinding

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var realm: Realm

    private var rateSnackbarShown: Boolean = false
    private var addGamesSnackbarShown: Boolean = false

    private lateinit var viewModel: InstalledAppsViewModel
    private var installedGames: List<String> = emptyList()


    // defintetly new and improved, not at all ripped off. and withoutt typos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setSupportActionBar(viewBinding.toolbar)
        viewBinding.appBarLayout.elevation = pixelToDp(4f).toFloat()

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        setupRealm()

        viewModel = ViewModelProvider(this).get(InstalledAppsViewModel::class.java).also {
            it.setupDone.observe(this) { done ->
                done?.handle()?.let {
                    setupInstalledGamesList()
                    setupSnackbars()
                }
            }
            it.setupInstalledAppsMap()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GamesListFragment(), TAG_GAMES_LIST_FRAGMENT)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        if (TimerService.IS_ACTIVE) {
            Dialogs.showCloseTimerOnResumeDialog(this) {
                val closeTimerIntent = Intent(getString(R.string.action_close_timer)).also {
                    it.putExtra(getString(R.string.extra_close_timer_from_onresume), true)
                }
                sendBroadcast(closeTimerIntent)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        sharedPrefs.edit()
            .putInt(getString(R.string.key_launch_counter), launchCounter)
            .putBoolean(getString(R.string.key_rate_snackbar_shown), rateSnackbarShown)
            .putBoolean(getString(R.string.key_add_games_snackbar_shown), addGamesSnackbarShown)
            .apply()
        FSTWidget.forceUpdateWidgets(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity_actions, menu)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            menu.findItem(R.id.menu_add_games).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.menu_help -> {
                startActivity(Intent(this, HelpActivity::class.java))
                true
            }
            R.id.menu_add_games -> {
                addInstalledGames()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRealm() {
        val savedData = sharedPrefs.getString(getString(R.string.key_games), "")!!
        if (savedData.isEmpty()) {
            realm = Realm.getDefaultInstance()
        } else {
            Realm.deleteRealm(Realm.getDefaultConfiguration()!!)
            realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction {
                    realm.createAllFromJson(Game::class.java, savedData)
                }
            } catch (e: RealmException) {
                realm.executeTransaction {
                    realm.createAllFromJson(Game::class.java, Util.migrateJson(savedData))
                }
            }

            sharedPrefs.edit()
                .remove(getString(R.string.key_games))
                .apply()
        }
    }

    private fun setupInstalledGamesList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            installedGames = app?.installedAppsMap.orEmpty().values
                .filter { it.category == ApplicationInfo.CATEGORY_GAME }
                .map { packageManager.getApplicationLabel(it).toString() }
        }
    }

    // Uses realm
    private fun setupSnackbars() {
        var toShowRateSnackbar = false
        rateSnackbarShown = sharedPrefs.getBoolean(getString(R.string.key_rate_snackbar_shown), false)
        if (!rateSnackbarShown && launchCounter == 0 && !realm.isEmpty) {
            val savedLaunchCounter = sharedPrefs.getInt(getString(R.string.key_launch_counter), 0)
            launchCounter = (savedLaunchCounter + 1).coerceAtMost(4)
            toShowRateSnackbar = launchCounter == 3
        }

        addGamesSnackbarShown = sharedPrefs.getBoolean(getString(R.string.key_add_games_snackbar_shown), false)
        val toShowAddGamesSnackbar = !(getAvailableInstalledGames().isEmpty() || addGamesSnackbarShown)

        if (toShowAddGamesSnackbar) {
            Handler(Looper.myLooper() ?: Looper.getMainLooper())
                .postDelayed(::showAddInstalledGamesSnackbar, 1000)
            addGamesSnackbarShown = true
        } else if (toShowRateSnackbar) {
            Handler(Looper.myLooper() ?: Looper.getMainLooper())
                .postDelayed(::showRateSnackbar, 1000)
            rateSnackbarShown = true
        }
    }

    private fun showRateSnackbar() {
        val snackbar = Snackbar.make(
            viewBinding.fabAdd, getString(R.string.rate_snackbar),
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(R.string.rate) {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName")
            )
            marketIntent.addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY
                        or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
            try {
                startActivity(marketIntent)
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            }
        }
        snackbar.show()
    }

    private fun showAddInstalledGamesSnackbar() {
        val snackbar = Snackbar.make(viewBinding.fabAdd, getString(R.string.add_games_snackbar), Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.add) { addInstalledGames() }
        snackbar.show()
    }

    // Uses realm
    private fun getAvailableInstalledGames() = installedGames.filter { !realm.gameExists(it) }

    // Uses realm
    private fun addInstalledGames() {
        val gameNames = getAvailableInstalledGames()
        if (gameNames.isEmpty()) {
            showToast(getString(R.string.no_games_to_add))
        } else {
            Dialogs.showAddInstalledGamesDialog(this, realm, gameNames) {
                (supportFragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT) as? GamesListFragment)
                    ?.refreshList()
                Snackbar.make(viewBinding.fabAdd, "Games added", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    companion object {

        private var launchCounter = 0
    }
}

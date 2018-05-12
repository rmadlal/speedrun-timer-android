package il.ronmad.speedruntimer

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import io.realm.Realm
import io.realm.exceptions.RealmException
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var receiver: BroadcastReceiver
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var realm: Realm

    private var rateSnackbarShown: Boolean = false
    private var addGamesSnackbarShown: Boolean = false

    internal var installedApps: List<ApplicationInfo> = listOf()
    private var installedGames: List<String> = listOf()


    // defintetly new and improved, not at all ripped off. and withoutt typos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        setupRealm()

        setupInstalledAppsLists()
        setupSnackbars()
        setupReceiver()
        setupFragments()
    }

    override fun onResume() {
        super.onResume()
        if (TimerService.IS_ACTIVE) {
            Dialogs.closeTimerOnResumeDialog(this).show()
        }
    }

    override fun onStop() {
        super.onStop()
        sharedPrefs.edit()
                .putInt(getString(R.string.key_launch_counter), launchCounter)
                .putBoolean(getString(R.string.key_rate_snackbar_shown), rateSnackbarShown)
                .putBoolean(getString(R.string.key_add_games_snackbar_shown), addGamesSnackbarShown)
                .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, TimerService::class.java))
        unregisterReceiver(receiver)
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
            R.id.menu_add_games -> {
                addInstalledGames()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRealm() {
        val savedData = sharedPrefs.getString(getString(R.string.key_games), "")
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

    private fun setupSnackbars() {
        var toShowRateSnackbar = false
        rateSnackbarShown = sharedPrefs.getBoolean(getString(R.string.key_rate_snackbar_shown), false)
        if (!rateSnackbarShown && launchCounter == 0 && !realm.isEmpty) {
            val savedLaunchCounter = sharedPrefs.getInt(getString(R.string.key_launch_counter), 0)
            launchCounter = Math.min(3, savedLaunchCounter) + 1
            toShowRateSnackbar = launchCounter == 3
        }

        addGamesSnackbarShown = sharedPrefs.getBoolean(getString(R.string.key_add_games_snackbar_shown), false)
        val toShowAddGamesSnackbar = !(getAvailableInstalledGames().isEmpty() || addGamesSnackbarShown)

        if (toShowAddGamesSnackbar) {
            Handler().postDelayed({ showAddInstalledGamesSnackbar() }, 1000)
            addGamesSnackbarShown = true
        } else if (toShowRateSnackbar) {
            Handler().postDelayed({ showRateSnackbar() }, 1000)
            rateSnackbarShown = true
        }
    }

    private fun setupReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return
                if (action == getString(R.string.action_close_timer)) {
                    if (Chronometer.started) {
                        sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                        Dialogs.timerActiveDialog(context).show()
                    } else {
                        stopService(Intent(context, TimerService::class.java))
                    }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(getString(R.string.action_close_timer))
        registerReceiver(receiver, intentFilter)
    }

    private fun setupFragments() {
        val gamesListFragment = supportFragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT) as? GamesListFragment
        if (gamesListFragment == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, GamesListFragment.newInstance(), TAG_GAMES_LIST_FRAGMENT)
                    .commit()
        }
    }

    private fun setupInstalledAppsLists() {
        val allInstalledApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        installedApps = allInstalledApps.filter {
            it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != packageName
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            installedGames = installedApps.filter { it.category == ApplicationInfo.CATEGORY_GAME }
                    .map { packageManager.getApplicationLabel(it).toString() }
        }
    }

    private fun showRateSnackbar() {
        val snackbar = Snackbar.make(fabAdd, getString(R.string.rate_snackbar),
                Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.rate) {
            val marketIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName"))
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                    or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        else
                            Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                    or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(marketIntent)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
            }
        }
        snackbar.show()
    }

    private fun showAddInstalledGamesSnackbar() {
        val snackbar = Snackbar.make(fabAdd, getString(R.string.add_games_snackbar), Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.add) { addInstalledGames() }
        snackbar.show()
    }

    private fun getAvailableInstalledGames(): List<String> {
        return installedGames.filter { !realm.gameExists(it) }
    }

    private fun addInstalledGames() {
        val gameNames = getAvailableInstalledGames()
        if (gameNames.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_games_to_add), Toast.LENGTH_SHORT).show()
        } else {
            Dialogs.addInstalledGamesDialog(this, realm, gameNames).show()
        }
    }


    companion object {

        private var launchCounter = 0
    }
}

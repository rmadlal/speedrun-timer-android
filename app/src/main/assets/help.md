### Table of Contents
1. [App structure and usage](#app-structure-and-usage)
	1. [Games list](#games-list)
	2. [Game screen](#game-screen)
	3. [Splits](#splits)
2. [The timer](#the-timer)
	1. [Features](#features)
	2. [Timer usage](#timer-usage)
	3. [Customization](#customization)
3. [Widget](#widget)
4. [Feedback](#feedback)

---

## <a name="app-structure-and-usage" />App structure and usage
### <a name="games-list" />Games list
When launching the app, you are introduced to your list of games.
* To add a game, click on the add button at the bottom corner and give it a name. Alternatively, on Android 8 (Oreo) or above, you can add your installed games from the menu.
* Long click (press and hold) on a game to edit its name or delete it, using the icons in the top bar.
* Clicking on a game will bring you to the game screen.

### <a name="game-screen" />Game screen
The game screen features a list of categories and an info tab which shows leaderboard information from [speedrun.com](https://www.speedrun.com/) for this game.
* Categories can be added using the add button at the bottom corner. A list of category suggestions according to [speedrun.com](https://www.speedrun.com/) will be shown under the text box (may take several seconds to show).
* Long click (press and hold) on a category to edit its name, personal best time and/or run count, or delete it, using the icons in the top bar.
* Clicking on a category will bring up a dialog from the bottom, which has two options: **Launch timer** and **View & edit splits**. These are detailed below.

### <a name="splits" />Splits
The splits (aka segments) for some category can be accessed via the **View & edit splits** button mentioned above. You can add, rename, edit, and rearrange your splits.
* Add a segment by clicking on the add button at the bottom corner. You will be asked to enter a name and you may also choose its position.
* Long click (press and hold) on a segment to edit its PB time, best time, name and/or position, or delete it, using the icons in the top bar. Editing segment times changes the overall PB time and Sum of Bests accordingly.
* You can import/export splits with splits.io, using the up and down icons in the top bar.
	To download, you need the run ID (example: https://splits.io/2z69 - the ID here is 2z69).
	When uploading, once the upload is finished, click **Claim** to open the splits.io link and claim the run there.

---

## <a name="the-timer" />The timer
Clicking on **Launch timer** will minimize the app and display the timer. If "Launch games" is enabled in the app settings (which can be accessed in the menu), the app will also attempt to launch the game itself. This setting can be found under **App behavior** within the settings.  
Note that a notification will be shown on your phone while the timer is displayed (required by Android).

### <a name="features" />Features
* The color of the timer changes, depending on your run: it'll be green if you're ahead of PB, red if behind, and blue if you finished a run and beat your PB. These colors can be customized in the app settings, under *Timer colors*.  
* If splits were set up, the name of the current split is displayed under the timer, and when splitting, delta will be shown next to the timer (how far ahead/behind you are). Both of these can be disabled in the settings, under *Display*.

### <a name="timer-usage" />Timer usage
* A short tap on the timer will:
	1. **start the timer** if it hasn't been started yet;
	2. **split** if splits were set up and the timer is running;
	3. **stop the timer** if on the final split or if splits were not set up and the timer is running.
* A long click (press and hold) on the timer will **reset** it, and if you beat your personal best, a dialog will pop up asking you if you'd like to save your new splits / best time.  
    Note: be sure to stop the timer first, and then long click to reset and save your time when getting a PB! Long-clicking while the timer is still running will instantly reset the timer (without saving)!
* Drag the timer around to change its position on the screen. The position will be saved (per game).
* Close the timer by clicking on **Close timer** in the app notification. Another way to close the timer is to simply open the app: a dialog will pop up, asking you to close your timer.  
	(A "drag down to dismiss" feature will hopefully be added soon, to close the timer more easily. Stay tuned for updates.)

### <a name="customization" />Customization
* **Comparison**: you may compare against personal best or best times. This can be set in the app settings, under *Timing settings*.
* **Countdown**: you may start the timer on a negative time, so that it starts after some time. This can be helpful for starting the game and the timer at the same time. This can also be set in *Timing settings*.
* Additional display settings can be found in the settings under *Display*. You may show or hide milliseconds, change the timer size, display a background behind the timer, etc.

---

## <a name="widget" />Widget
The app's widget is handy for those who currently speedrun one particular game/category for a while and want a quick way to start.  
* You may add a Floating Speedrun Timer widget by long-clicking anywhere on your phone's home screen and choosing "widgets". Find Floating Speedrun Timer and drag it into your home screen.
* A configuration window will pop up, where you must select a game and category, then hit **Add widget**.
* Now, simply tap on the widget to launch the timer for this game/category (and also launch the game itself if enabled in the app settings).  

You can add multiple widgets, each for a different game/category.

---

## <a name="feedback" />Feedback
Rate and review this app in its [Google Play Store page](https://play.google.com/store/apps/details?id=il.ronmad.speedruntimer).

Contact me by email at [ronmadlal@gmail.com](mailto:ronmadlal@gmail.com).  
I can also be reached in the wonderful [Mobile Speedrunning Community Discord server](https://discord.gg/WN9GVkX).
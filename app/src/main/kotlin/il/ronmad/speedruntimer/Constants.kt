package il.ronmad.speedruntimer

const val TAG_GAMES_LIST_FRAGMENT = "GamesListFragment"
const val TAG_GAME_FRAGMENT = "GameFragment"
const val TAG_SPLITS_LIST_FRAGMENT = "SplitsListFragment"
const val TAG_CATEGORY_BOTTOM_SHEET_DIALOG = "SplitsListFragment"
const val TAG_COUNTDOWN_PREFERENCE_FRAGMENT = "CountdownPreference"
const val SRC_API = "https://www.speedrun.com/api/v1/"
const val ARG_GAME_NAME = "game-name"
const val ARG_CATEGORY_NAME = "category-name"

enum class Comparison {
    PERSONAL_BEST,
    BEST_SEGMENTS
}

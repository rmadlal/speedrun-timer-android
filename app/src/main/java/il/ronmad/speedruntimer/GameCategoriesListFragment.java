package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

import static il.ronmad.speedruntimer.Util.gson;

public class GameCategoriesListFragment extends BaseListFragment {

    private static final String ARG_GAME_JSON = "game-json";
    private Game mGame;

    public GameCategoriesListFragment() {
        // Required empty public constructor
    }

    public static GameCategoriesListFragment newInstance(Game game) {
        GameCategoriesListFragment fragment = new GameCategoriesListFragment();
        Bundle args = new Bundle();

        args.putString(ARG_GAME_JSON, gson.toJson(game));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (getArguments() != null) {
            mGame = gson.fromJson(getArguments().getString(ARG_GAME_JSON), Game.class);
        }
        contextMenuResId = R.menu.category_list_fragment_context_menu;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CategoryAdapter adapter = new CategoryAdapter(getContext(), mGame);
        setAdapter(adapter);
    }

    public void resetData(Game game) {
        mGame = game;
        if (mActionMode != null) {
            mActionMode.finish();
        }
        ((CategoryAdapter)mListAdapter).update(game);
    }

    public void addCategory(String category) {
        ((CategoryAdapter)mListAdapter).add(category);
    }

    public void removeCategories(String[] categories) {
         ((CategoryAdapter)mListAdapter).removeAll(categories);
    }

    public void setBestTime(String category, long bestTime) {
        ((CategoryAdapter)mListAdapter).set(category, bestTime);
    }

    public Game getGame() {
        return mGame;
    }
}

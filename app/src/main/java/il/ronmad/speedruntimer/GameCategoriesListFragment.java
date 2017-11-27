package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GameCategoriesListFragment extends BaseListFragment {

    private static final String ARG_GAME_JSON = "game-json";

    private Game mGame;

    public GameCategoriesListFragment() {
        // Required empty public constructor
    }

    public static GameCategoriesListFragment newInstance(Game game) {
        GameCategoriesListFragment fragment = new GameCategoriesListFragment();
        Bundle args = new Bundle();

        Gson gson = new GsonBuilder().create();
        args.putString(ARG_GAME_JSON, gson.toJson(game));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Gson gson = new GsonBuilder().create();
            mGame = gson.fromJson(getArguments().getString(ARG_GAME_JSON), Game.class);
        }
        layoutResId = R.layout.category_list_layout;
        contextMenuResId = R.menu.category_list_fragment_context_menu;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mListAdapter == null) {
            CategoryAdapter adapter = new CategoryAdapter(getContext(), mGame, checkedItemPositions);
            setAdapter(adapter);
        }
    }

    public void updateData(Game game) {
        finishActionMode();
        mGame = game;
        ((CategoryAdapter)mListAdapter).update(game);
    }

    public Game getGame() {
        return mGame;
    }
}

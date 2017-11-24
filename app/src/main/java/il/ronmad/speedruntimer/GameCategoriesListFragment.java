package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

import static il.ronmad.speedruntimer.GameDatabase.currentGame;

public class GameCategoriesListFragment extends BaseListFragment {

    private Game game;

    public GameCategoriesListFragment() {
        // Required empty public constructor
    }

    public static GameCategoriesListFragment newInstance() {
        return new GameCategoriesListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        game = currentGame;
        layoutResId = R.layout.category_list_layout;
        contextMenuResId = R.menu.category_list_fragment_context_menu;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mListAdapter == null) {
            CategoryAdapter adapter = new CategoryAdapter(getContext(), game, checkedItemPositions);
            setAdapter(adapter);
        }
    }

    public void updateData(Game game) {
        finishActionMode();
        this.game = game;
        ((CategoryAdapter)mListAdapter).update(game);
    }

    public Game getGame() {
        return game;
    }
}

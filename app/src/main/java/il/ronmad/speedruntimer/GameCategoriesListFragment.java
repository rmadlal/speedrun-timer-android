package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class GameCategoriesListFragment extends BaseListFragment {

    private Game game;

    public GameCategoriesListFragment() {
        // Required empty public constructor
    }

    private void init(Game game) {
        layoutResId = R.layout.category_list_layout;
        contextMenuResId = R.menu.category_list_fragment_context_menu;
        this.game = game;
    }

    public static GameCategoriesListFragment newInstance(Game game) {
        GameCategoriesListFragment fragment = new GameCategoriesListFragment();
        fragment.init(game);
        return fragment;
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

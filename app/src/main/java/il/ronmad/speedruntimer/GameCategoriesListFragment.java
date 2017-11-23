package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

import static il.ronmad.speedruntimer.GameDatabase.currentGame;

public class GameCategoriesListFragment extends BaseListFragment {

    public GameCategoriesListFragment() {
        // Required empty public constructor
    }

    public static GameCategoriesListFragment newInstance() {
        return new GameCategoriesListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutResId = R.layout.category_list_layout;
        contextMenuResId = R.menu.category_list_fragment_context_menu;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mListAdapter == null) {
            CategoryAdapter adapter = new CategoryAdapter(getContext(), currentGame, checkedItemPositions);
            setAdapter(adapter);
        }
    }

    public void updateData() {
        finishActionMode();
        ((CategoryAdapter)mListAdapter).update();
    }
}

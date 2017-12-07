package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class GamesListFragment extends BaseListFragment {

    public GamesListFragment() {
        // Required empty public constructor
    }

    public static GamesListFragment newInstance() {
        return new GamesListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutResId = R.layout.games_list_layout;
        contextMenuResId = R.menu.games_list_fragment_context_menu;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        GamesAdapter adapter = new GamesAdapter(getContext(),
                realm.where(Game.class).findAll(), checkedItemPositions);
        setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((GamesAdapter) mListAdapter).update(realm.where(Game.class).findAll());
    }

    public void updateData() {
        finishActionMode();
        ((GamesAdapter) mListAdapter).update(realm.where(Game.class).findAll());
    }
}

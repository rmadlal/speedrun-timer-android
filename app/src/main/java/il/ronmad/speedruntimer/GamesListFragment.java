package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.List;

public class GamesListFragment extends BaseListFragment {

    private List<Game> games;

    public GamesListFragment() {
        // Required empty public constructor
    }

    private void init(List<Game> games) {
        layoutResId = R.layout.games_list_layout;
        contextMenuResId = R.menu.games_list_fragment_context_menu;
        this.games = games;
    }

    public static GamesListFragment newInstance(List<Game> games) {
        GamesListFragment fragment = new GamesListFragment();
        fragment.init(games);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mListAdapter == null) {
            GamesAdapter adapter = new GamesAdapter(getContext(), games, checkedItemPositions);
            setAdapter(adapter);
        }
    }

    public void updateData(List<Game> games) {
        finishActionMode();
        this.games = games;
        ((GamesAdapter) mListAdapter).update(games);
    }
}

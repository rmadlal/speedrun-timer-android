package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static il.ronmad.speedruntimer.GameDatabase.games;

public class GamesListFragment extends BaseListFragment {

    private List<String> mGameNames;

    public GamesListFragment() {
        // Required empty public constructor
    }

    public static GamesListFragment newInstance() {
        return new GamesListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGameNames = new ArrayList<>();
        for (Game game : games) {
            mGameNames.add(game.getName());
        }
        layoutResId = R.layout.games_list_layout;
        contextMenuResId = R.menu.games_list_fragment_context_menu;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mListAdapter == null) {
            GamesAdapter adapter = new GamesAdapter(getContext(), mGameNames, checkedItemPositions);
            setAdapter(adapter);
        }
    }

    public void update() {
        finishActionMode();
        mGameNames.clear();
        for (Game game : games) {
            mGameNames.add(game.getName());
        }
        mListAdapter.notifyDataSetChanged();
    }
}

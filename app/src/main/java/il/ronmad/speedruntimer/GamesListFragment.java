package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class GamesListFragment extends BaseListFragment {

    private GamesAdapter adapter;

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
        adapter = new GamesAdapter(getContext(), realm.where(Game.class).findAll());
        setAdapter(adapter);
    }

    protected void update() {
        finishActionMode();
        adapter.setData(realm.where(Game.class).findAll());
    }
}

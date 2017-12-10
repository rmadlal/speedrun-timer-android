package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

public class CategoryListFragment extends BaseListFragment {

    private static final String ARG_GAME_NAME = "game-name";

    private Game game;
    private CategoryAdapter adapter;

    public CategoryListFragment() {
        // Required empty public constructor
    }

    public static CategoryListFragment newInstance(String gameName) {
        CategoryListFragment fragment = new CategoryListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GAME_NAME, gameName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String gameName = getArguments().getString(ARG_GAME_NAME);
            game = realm.where(Game.class).equalTo("name", gameName).findFirst();
        }
        layoutResId = R.layout.category_list_layout;
        contextMenuResId = R.menu.category_list_fragment_context_menu;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new CategoryAdapter(getContext(), game.categories);
        setAdapter(adapter);
    }

    protected void update() {
        finishActionMode();
        adapter.setData(game.categories);
    }
}

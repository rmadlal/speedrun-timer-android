package il.ronmad.speedruntimer;

import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GamesListFragment extends BaseListFragment {

    private static final String ARG_GAME_NAMES = "game-names";
    private List<String> mGameNames;

    public GamesListFragment() {
        // Required empty public constructor
    }

    public static GamesListFragment newInstance(List<Game> games) {
        GamesListFragment fragment = new GamesListFragment();
        Bundle args = new Bundle();

        ArrayList<String> gameNames = new ArrayList<>();
        for (Game game : games) {
            gameNames.add(game.getName());
        }
        args.putStringArrayList(ARG_GAME_NAMES, gameNames);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mGameNames = getArguments().getStringArrayList(ARG_GAME_NAMES);
        } else {
            mGameNames = new ArrayList<>();
        }
        contextMenuResId = R.menu.games_list_fragment_context_menu;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        GamesAdapter adapter = new GamesAdapter(getContext(), mGameNames);
        setAdapter(adapter);
    }

    public void addGame(String newGame) {
        mGameNames.add(newGame);
        ((GamesAdapter)mListAdapter).add(newGame);
    }

    public void setGameName(int position, String newName) {
        mGameNames.set(position, newName);
        ((GamesAdapter)mListAdapter).set(position, newName);
    }

    public void removeGames(int[] positions) {
        String[] toRemove = new String[positions.length];
        for (int i = 0; i < positions.length; i++) {
            toRemove[i] = mGameNames.get(positions[i]);
        }
        mGameNames.removeAll(Arrays.asList(toRemove));
        ((GamesAdapter)mListAdapter).removeAll(toRemove);
    }
}

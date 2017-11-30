package il.ronmad.speedruntimer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class GamesAdapter extends BaseAdapter {

    private Context context;
    private List<Game> games;
    private List<Integer> checkedItemPositions;

    public GamesAdapter(Context context, List<Game> games, List<Integer> checkedItemPositions) {
        this.context = context;
        this.games = games;
        this.checkedItemPositions = checkedItemPositions;
    }

    @Override
    public int getCount() {
        return games.size();
    }

    @Override
    public Object getItem(int i) {
        return games.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View layout;
        if (view == null) {
            layout = LayoutInflater.from(context).inflate(R.layout.games_list_item, null);
        } else {
            layout = view;
        }
        TextView text = layout.findViewById(R.id.categoryName);

        Game game = (Game) getItem(i);
        text.setText(game.name);

        if (checkedItemPositions.contains(i)) {
            layout.setBackgroundResource(R.color.colorHighlightedListItem);
        } else {
            layout.setBackgroundResource(android.R.color.transparent);
        }

        return layout;
    }

    void update(List<Game> games) {
        this.games = games;
        notifyDataSetChanged();
    }
}

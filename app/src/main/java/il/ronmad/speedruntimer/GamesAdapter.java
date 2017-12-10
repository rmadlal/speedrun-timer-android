package il.ronmad.speedruntimer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class GamesAdapter extends MyBaseAdapter<Game> {

    public GamesAdapter(Context context, List<Game> games) {
        super(context, games);
        this.listItemResourceId = R.layout.games_list_item;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View listItemView = super.getView(i, view, viewGroup);

        TextView text = listItemView.findViewById(R.id.gameName);
        text.setText((getItem(i)).name);

        return listItemView;
    }
}

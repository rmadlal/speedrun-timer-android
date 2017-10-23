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
    private List<String> gameNames;

    public GamesAdapter(Context context, List<String> gameNames) {
        this.context = context;
        this.gameNames = gameNames;
    }

    @Override
    public int getCount() {
        return gameNames.size();
    }

    @Override
    public Object getItem(int i) {
        return gameNames.get(i);
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

        TextView text = (TextView) layout.findViewById(R.id.text1);
        text.setText((String) getItem(i));

        return layout;
    }
}

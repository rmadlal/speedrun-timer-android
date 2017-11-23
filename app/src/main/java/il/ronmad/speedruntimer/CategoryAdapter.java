package il.ronmad.speedruntimer;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static il.ronmad.speedruntimer.GameDatabase.currentGame;

public class CategoryAdapter extends BaseAdapter {

    private Context context;
    private List<String> categories;
    private List<Long> bestTimes;
    private Vector<Integer> checkedItemPositions;

    public CategoryAdapter(Context context, Game game, Vector<Integer> checkedItemPositions) {
        this.context = context;
        this.categories = new ArrayList<>(game.getCategoryNames());
        this.bestTimes = new ArrayList<>(game.getBestTimes());
        this.checkedItemPositions = checkedItemPositions;
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int i) {
        return categories.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View layout;
        if (view == null) {
            layout = LayoutInflater.from(context).inflate(R.layout.category_list_item, null);
        } else {
            layout = view;
        }
        TextView text1 = layout.findViewById(R.id.text1);
        TextView text3 = layout.findViewById(R.id.text3);

        text1.setText((String) getItem(i));
        text3.setText(bestTimes.get(i) > 0 ? Util.getFormattedTime(bestTimes.get(i)) : "None yet");
        text3.setTextColor(ContextCompat.getColor(context,
                bestTimes.get(i) > 0 ? R.color.colorAccent : android.R.color.primary_text_light));

        if (checkedItemPositions.contains(i)) {
            layout.setBackgroundResource(R.color.colorHighlightedListItem);
        } else {
            layout.setBackgroundResource(android.R.color.transparent);
        }

        return layout;
    }

    public void update() {
        categories = new ArrayList<>(currentGame.getCategoryNames());
        bestTimes = new ArrayList<>(currentGame.getBestTimes());
        notifyDataSetChanged();
    }
}

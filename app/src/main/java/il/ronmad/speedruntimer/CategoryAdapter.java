package il.ronmad.speedruntimer;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Vector;

public class CategoryAdapter extends BaseAdapter {

    private Context context;
    private List<Category> categories;
    private Vector<Integer> checkedItemPositions;

    public CategoryAdapter(Context context, Game game, Vector<Integer> checkedItemPositions) {
        this.context = context;
        this.categories = game.categories;
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
        TextView nameText = layout.findViewById(R.id.categoryName);
        TextView bestTimeText = layout.findViewById(R.id.pbTime);
        TextView runCountText = layout.findViewById(R.id.runsNum);

        Category category = (Category) getItem(i);
        nameText.setText(category.name);
        bestTimeText.setText(category.bestTime > 0 ? Util.getFormattedTime(category.bestTime) : "None yet");
        bestTimeText.setTextColor(ContextCompat.getColor(context,
                category.bestTime > 0 ? R.color.colorAccent : android.R.color.primary_text_light));
        runCountText.setText(String.valueOf(category.runCount));

        if (checkedItemPositions.contains(i)) {
            layout.setBackgroundResource(R.color.colorHighlightedListItem);
        } else {
            layout.setBackgroundResource(android.R.color.transparent);
        }

        return layout;
    }

    public void update(Game game) {
        categories = game.categories;
        notifyDataSetChanged();
    }
}

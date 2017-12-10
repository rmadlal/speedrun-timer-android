package il.ronmad.speedruntimer;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class CategoryAdapter extends MyBaseAdapter<Category> {

    public CategoryAdapter(Context context, List<Category> categories) {
        super(context, categories);
        this.listItemResourceId = R.layout.category_list_item;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View listItemView = super.getView(i, view, viewGroup);

        TextView nameText = listItemView.findViewById(R.id.categoryName);
        TextView bestTimeText = listItemView.findViewById(R.id.pbTime);
        TextView runCountText = listItemView.findViewById(R.id.runsNum);

        Category category = getItem(i);
        nameText.setText(category.name);
        bestTimeText.setText(category.bestTime > 0 ? Util.getFormattedTime(category.bestTime) : "None yet");
        bestTimeText.setTextColor(ContextCompat.getColor(context,
                category.bestTime > 0 ? R.color.colorAccent : android.R.color.primary_text_light));
        runCountText.setText(String.valueOf(category.runCount));

        return listItemView;
    }
}

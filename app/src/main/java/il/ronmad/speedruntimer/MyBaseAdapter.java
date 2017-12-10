package il.ronmad.speedruntimer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class MyBaseAdapter<T> extends BaseAdapter {

    protected Context context;
    protected List<T> data;
    List<T> checkedItems;
    protected int listItemResourceId;

    public MyBaseAdapter(Context context, List<T> data) {
        this.context = context;
        this.data = data;
        this.checkedItems = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public T getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View listItemView;
        if (view == null) {
            listItemView = LayoutInflater.from(context).inflate(listItemResourceId, viewGroup, false);
        } else {
            listItemView = view;
        }

        setItemBackground(listItemView, i);
        return listItemView;
    }

    void setData(List<T> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    void setItemChecked(int i, boolean checked) {
        T item = getItem(i);
        if (checked) {
            checkedItems.add(item);
        } else {
            checkedItems.remove(item);
        }
        notifyDataSetChanged();
    }

    boolean isItemChecked(int i) {
        return checkedItems.contains(getItem(i));
    }

    void clearSelections() {
        checkedItems.clear();
        notifyDataSetChanged();
    }

    private void setItemBackground(View listItemView, int i) {
        if (isItemChecked(i)) {
            listItemView.setBackgroundResource(R.color.colorHighlightedListItem);
        } else {
            listItemView.setBackgroundResource(android.R.color.transparent);
        }
    }
}

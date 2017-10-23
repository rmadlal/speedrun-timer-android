package il.ronmad.speedruntimer;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import il.ronmad.speedruntimer.MainActivity.ListAction;

public abstract class BaseListFragment extends ListFragment {

    protected BaseAdapter mListAdapter;
    protected ActionMode mActionMode;
    protected List<View> checkedItems;
    protected View clickedItem;
    protected int layoutResId;
    protected int contextMenuResId;

    protected OnListFragmentInteractionListener mListener;

    public BaseListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        checkedItems = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGamesListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.main_activity_actions, menu);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mActionMode != null) {
            setChecked(l, v, l.isItemChecked(position));
            if (checkedItems.size() == 0) {
                mActionMode.finish();
            } else {
                mActionMode.invalidate();
            }
        } else {
            setChecked(l, v, false);
            clickedItem = v;
            invokeListener(ListAction.CLICK);
        }
    }

    public interface OnListFragmentInteractionListener {
        void onGamesListFragmentInteraction(GamesListFragment gamesListFragment, ListAction action);
        void onCategoryListFragmentInteraction(GameCategoriesListFragment categoryListFragment, ListAction action);
    }

    public String[] getSelectedItemNames() {
        String[] items = new String[checkedItems.size()];
        int i = 0;
        for (View view : checkedItems) {
            items[i++] = (String) mListAdapter.getItem(getListView().getPositionForView(view));
        }
        return items;
    }

    public String getClickedItemName() {
        return (String) mListAdapter.getItem(getListView().getPositionForView(clickedItem));
    }

    protected void setAdapter(BaseAdapter adapter) {
        mListAdapter = adapter;
        setListAdapter(mListAdapter);
        ListView listView = getListView();
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mActionMode != null) {
                    return false;
                }

                mActionMode = getActivity().startActionMode(actionModeCallback);
                setChecked(getListView(), view, true);
                mActionMode.invalidate();
                return true;
            }
        });
    }

    protected void setChecked(ListView listView, View view, boolean checked) {
        listView.setItemChecked(listView.getPositionForView(view), checked);
        if (checked) {
            checkedItems.add(view);
            view.setBackgroundResource(R.color.colorHighlightedListItem);
        } else {
            checkedItems.remove(view);
            view.setBackgroundResource(android.R.color.transparent);
        }
    }

    protected void clearSelections() {
        while (!checkedItems.isEmpty()) {
            setChecked(getListView(), checkedItems.get(0), false);
        }
    }

    private void invokeListener(ListAction action) {
        if (this instanceof GamesListFragment) {
            mListener.onGamesListFragmentInteraction((GamesListFragment) this, action);
        } else if (this instanceof GameCategoriesListFragment) {
            mListener.onCategoryListFragmentInteraction((GameCategoriesListFragment) this, action);
        }
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(contextMenuResId, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            actionMode.setTitle(String.valueOf(checkedItems.size()));
            menu.findItem(R.id.menu_edit).setVisible(checkedItems.size() == 1);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_edit:
                    invokeListener(ListAction.EDIT);
                    return true;
                case R.id.menu_delete:
                    invokeListener(ListAction.DELETE);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            clearSelections();
            mActionMode = null;
        }
    };
}

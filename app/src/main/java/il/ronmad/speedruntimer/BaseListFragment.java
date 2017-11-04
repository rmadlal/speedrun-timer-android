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
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.Vector;

import il.ronmad.speedruntimer.MainActivity.ListAction;

public abstract class BaseListFragment extends ListFragment {

    protected BaseAdapter mListAdapter;
    protected ActionMode mActionMode;
    protected Vector<Integer> checkedItemPositions;
    protected int clickedItemPosition;
    protected int layoutResId;
    protected int contextMenuResId;

    protected OnListFragmentInteractionListener mListener;

    public BaseListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkedItemPositions = new Vector<>();
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupListView();
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
    public void onPause() {
        super.onPause();
        finishActionMode();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.main_activity_actions, menu);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mActionMode != null) {
            setChecked(position, l.isItemChecked(position));
            if (checkedItemPositions.isEmpty()) {
                mActionMode.finish();
            } else {
                mActionMode.invalidate();
            }
        } else {
            setChecked(position, false);
            clickedItemPosition = position;
            mListener.onListFragmentInteraction(ListAction.CLICK);
        }
        mListAdapter.notifyDataSetChanged();
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(ListAction action);
    }

    public String[] getSelectedItemNames() {
        String[] items = new String[checkedItemPositions.size()];
        int i = 0;
        for (int pos : checkedItemPositions) {
            items[i++] = (String) mListAdapter.getItem(pos);
        }
        return items;
    }

    public String getClickedItemName() {
        return (String) mListAdapter.getItem(clickedItemPosition);
    }

    protected void setAdapter(BaseAdapter adapter) {
        mListAdapter = adapter;
        setListAdapter(mListAdapter);
    }

    protected void setChecked(int position, boolean checked) {
        ListView listView = getListView();
        listView.setItemChecked(position, checked);
        if (checked) {
            checkedItemPositions.add(position);
        } else {
            checkedItemPositions.remove(Integer.valueOf(position));
        }
        mListAdapter.notifyDataSetChanged();
    }

    protected void clearSelections() {
        if (mListAdapter.isEmpty()) {
            checkedItemPositions.clear();
        }
        else while (!checkedItemPositions.isEmpty()) {
            setChecked(checkedItemPositions.get(0), false);
        }
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    private void setupListView() {
        ListView listView = getListView();
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            if (mActionMode != null) {
                return false;
            }
            mActionMode = getActivity().startActionMode(actionModeCallback);
            setChecked(i, true);
            mActionMode.invalidate();
            return true;
        });
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
            actionMode.setTitle(String.valueOf(checkedItemPositions.size()));
            menu.findItem(R.id.menu_edit).setVisible(checkedItemPositions.size() == 1);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_edit:
                    mListener.onListFragmentInteraction(ListAction.EDIT);
                    return true;
                case R.id.menu_delete:
                    mListener.onListFragmentInteraction(ListAction.DELETE);
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

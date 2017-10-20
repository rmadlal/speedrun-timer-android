package il.ronmad.speedruntimer;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public abstract class BaseListFragment extends ListFragment {

    protected BaseAdapter mListAdapter;
    protected ActionMode mActionMode;
    protected List<View> checkedItems;
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
            invokeListenerOnItemPositions(MainActivity.ListAction.CLICK, new int[]{ position });
        }

    }

    public interface OnListFragmentInteractionListener {
        void onGamesListFragmentInteraction(MainActivity.ListAction action, int[] positions);
        void onCategoryListFragmentInteraction(MainActivity.ListAction action, String[] categories);
    }

    protected void setAdapter(BaseAdapter adapter) {
        mListAdapter = adapter;
        setListAdapter(mListAdapter);
        ListView listView = getListView();
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemLongClickListener(longClickListener);
    }

    protected void setChecked(ListView listView, View view, boolean checked) {
        listView.setItemChecked(listView.getPositionForView(view), checked);
        if (checked) {
            checkedItems.add(view);
            view.setBackgroundResource(android.support.design.R.color.highlighted_text_material_light);
        } else {
            checkedItems.remove(view);
            view.setBackgroundResource(android.R.color.transparent);
        }
    }

    private void invokeListenerOnCheckedItems(MainActivity.ListAction action) {
        int[] positions = new int[checkedItems.size()];
        int i = 0;
        for (View view : checkedItems) {
            positions[i++] = getListView().getPositionForView(view);
        }
        invokeListenerOnItemPositions(action, positions);
    }

    private void invokeListenerOnItemPositions(MainActivity.ListAction action, int[] positions) {
        if (this instanceof GamesListFragment) {
            mListener.onGamesListFragmentInteraction(action, positions);
        } else if (this instanceof GameCategoriesListFragment) {
            String[] categories = new String[positions.length];
            for (int i = 0; i < positions.length; i++) {
                categories[i] = (String) mListAdapter.getItem(positions[i]);
            }
            mListener.onCategoryListFragmentInteraction(action, categories);
        }
    }

    private AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(final AdapterView<?> adapterView, View view, int i, long l) {
            if (mActionMode != null) {
                return false;
            }

            final ListView listView = getListView();
            mActionMode = getActivity().startActionMode(new ActionMode.Callback() {
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
                            invokeListenerOnCheckedItems(MainActivity.ListAction.EDIT);
                            actionMode.finish();
                            return true;
                        case R.id.menu_delete:
                            invokeListenerOnCheckedItems(MainActivity.ListAction.DELETE);
                            actionMode.finish();
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    while (!checkedItems.isEmpty()) {
                        setChecked(listView, checkedItems.get(0), false);
                    }
                    mActionMode = null;
                }
            });
            setChecked(listView, view, true);
            mActionMode.invalidate();
            return true;
        }
    };
}

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ListView;

import il.ronmad.speedruntimer.MainActivity.ListAction;
import io.realm.Realm;

public abstract class BaseListFragment extends ListFragment {

    protected Realm realm;
    protected MyBaseAdapter mListAdapter;
    protected ActionMode mActionMode;
    protected int clickedItemPosition;
    protected int layoutResId;
    protected int contextMenuResId;

    protected OnListFragmentInteractionListener mListener;

    private boolean viewsHaveBeenDestroyed;

    public BaseListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        // This stops animation on rotation as we have a retained instance.
        boolean shouldNotAnimate = enter && viewsHaveBeenDestroyed;
        viewsHaveBeenDestroyed = false;
        return shouldNotAnimate ? AnimationUtils.loadAnimation(getActivity(), R.anim.none)
                : super.onCreateAnimation(transit, enter, nextAnim);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewsHaveBeenDestroyed = true;
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
    public void onResume() {
        super.onResume();
        update();
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
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mActionMode != null) {
            mListAdapter.setItemChecked(position, !mListAdapter.isItemChecked(position));
            if (mListAdapter.checkedItems.isEmpty()) {
                mActionMode.finish();
            } else {
                mActionMode.invalidate();
            }
        } else {
            clickedItemPosition = position;
            mListener.onListFragmentInteraction(ListAction.CLICK);
        }
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(ListAction action);
    }

    protected abstract void update();

    public Object[] getSelectedItems() {
        return mListAdapter.checkedItems.toArray();
    }

    public Object getClickedItem() {
        return mListAdapter.getItem(clickedItemPosition);
    }

    protected void setAdapter(MyBaseAdapter adapter) {
        mListAdapter = adapter;
        setListAdapter(mListAdapter);
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
            if (getActivity() != null) {
                mActionMode = getActivity().startActionMode(actionModeCallback);
                mListAdapter.setItemChecked(i, true);
                mActionMode.invalidate();
                return true;
            }
            return false;
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
            actionMode.setTitle(String.valueOf(mListAdapter.checkedItems.size()));
            menu.findItem(R.id.menu_edit).setVisible(mListAdapter.checkedItems.size() == 1);
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
            mListAdapter.clearSelections();
            mActionMode = null;
        }
    };
}

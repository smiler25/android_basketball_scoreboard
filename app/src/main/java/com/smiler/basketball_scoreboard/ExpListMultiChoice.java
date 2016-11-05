package com.smiler.basketball_scoreboard;

import android.app.Activity;
import android.view.ActionMode;
import android.widget.AbsListView;
import android.widget.ExpandableListView;

import com.smiler.basketball_scoreboard.results.ResultsExpListAdapter;

public class ExpListMultiChoice extends BaseMultiChoice {

    private ResultsExpListAdapter adapter;

    public ExpListMultiChoice(AbsListView listView, Activity activity, CABListener listener) {
        super(listView, activity, listener);
        adapter = (ResultsExpListAdapter)((ExpandableListView)listView).getExpandableListAdapter();
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long adapterId, boolean checked) {
        if (checked) {
            addSelectedId((int) adapter.getGroupId(position));
        } else {
            removeSelectedId((int) adapter.getGroupId(position));
        }
        super.onItemCheckedStateChanged(mode, position, adapterId, checked);
        adapter.toggleSelection(position, checked);
    }
}
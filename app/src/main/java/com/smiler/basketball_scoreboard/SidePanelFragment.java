package com.smiler.basketball_scoreboard;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.smiler.basketball_scoreboard.elements.EditPlayerDialog;
import com.smiler.basketball_scoreboard.elements.ListDialog;
import com.smiler.basketball_scoreboard.elements.SidePanelRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SidePanelFragment extends Fragment implements View.OnClickListener {

    SidePanelListener listener;
    private TableLayout table;
    private TreeMap<Integer, SidePanelRow> rows = new TreeMap<>();
    private List<Integer> playersNumbers = new ArrayList<>();
    private TreeSet<SidePanelRow> activePlayers = new TreeSet<>();
    private SidePanelRow captainPlayer;
    private ToggleButton panelSelect;
    private boolean left = true;

    public static SidePanelFragment newInstance(boolean left) {
        SidePanelFragment f = new SidePanelFragment();
        Bundle args = new Bundle();
        args.putBoolean("left", left);
        f.setArguments(args);
        return f;
    }

    public SidePanelFragment() {}

    public interface SidePanelListener {
        void onSidePanelClose(boolean left);
        void onSidePanelActiveSelected(TreeSet<SidePanelRow> rows, boolean left);
        void onSidePanelNoActive(boolean left);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (SidePanelListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement SidePanelListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        left = args.getBoolean("left", true);
        int layout_id, table_layout_id, close_bu_id, clear_bu_id, add_bu_id, add_auto_bu_id, toggle_bu_id;
        if (left) {
            layout_id = R.layout.side_panel_full_left;
            close_bu_id = R.id.left_panel_close;
            table_layout_id = R.id.left_panel_table;
            add_bu_id = R.id.left_panel_add;
            add_auto_bu_id = R.id.left_panel_add_auto;
            toggle_bu_id = R.id.left_panel_select_active;
            clear_bu_id = R.id.left_panel_clear;
        } else {
            layout_id = R.layout.side_panel_full_right;
            close_bu_id = R.id.right_panel_close;
            table_layout_id = R.id.right_panel_table;
            add_bu_id = R.id.right_panel_add;
            add_auto_bu_id = R.id.right_panel_add_auto;
            toggle_bu_id = R.id.right_panel_select_active;
            clear_bu_id = R.id.right_panel_clear;
        }

        View v = inflater.inflate(layout_id, container, false);
        table = (TableLayout) v.findViewById(table_layout_id);
        addHeader();
        v.findViewById(close_bu_id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!panelSelect.isChecked()) {
                    listener.onSidePanelClose(left);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.side_panel_confirm), Toast.LENGTH_LONG).show();
                }
            }
        });
        v.findViewById(add_bu_id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkAddAvailable()) {
                    return;
                }
                EditPlayerDialog.newInstance(left).show(getFragmentManager(), EditPlayerDialog.TAG);
            }
        });
        View addAutoView = v.findViewById(add_auto_bu_id);
        addAutoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRowsAuto();
            }
        });
        v.findViewById(clear_bu_id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });
        addAutoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                restoreCurrentData();
                return true;
            }
        });
        panelSelect = (ToggleButton) v.findViewById(toggle_bu_id);
        panelSelect.setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof SidePanelRow) {
            if (((SidePanelRow) v).toggleSelected()) {
                activePlayers.add((SidePanelRow) v);
            } else {
                activePlayers.remove(v);
            }
        } else {
            switch (v.getId()) {
                case R.id.left_panel_select_active:
                case R.id.right_panel_select_active:
                    handleSelection();
                    break;
            }
        }
    }

    private void handleSelection() {
        if (panelSelect.isChecked() || activePlayers.size() <= 5) {
            View.OnClickListener l = panelSelect.isChecked() ? this : null;
            for (SidePanelRow row : rows.values()) { row.setOnClickListener(l); }
            if (!panelSelect.isChecked()) {
                listener.onSidePanelActiveSelected(activePlayers, left);
            }
        } else {
            panelSelect.setChecked(true);
            Toast.makeText(getActivity(),
                    String.format((activePlayers.size() < 5)
                            ? getResources().getString(R.string.side_panel_few) : getResources().getString(R.string.side_panel_many),
                            activePlayers.size()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void addHeader() {
        table.addView(new SidePanelRow(getActivity().getApplicationContext(), left));
    }

    public boolean editRow(int id, int number, String name, boolean captain) {
        SidePanelRow row = rows.get(id);
        int old_number = row.getNumber();
        row.edit(number, name, captain);
        if (old_number != number) {
            playersNumbers.remove(Integer.valueOf(old_number));
            playersNumbers.add(number);
        }
        return true;
    }

    public boolean deleteRow(int id) {
        SidePanelRow row = rows.get(id);
        table.removeView(row);
        playersNumbers.remove(Integer.valueOf(row.getNumber()));
        activePlayers.remove(row);
        rows.remove(row.getId());
        return true;
    }

    public SidePanelRow addRow(int number, String name, boolean captain) {
        if (!checkAddAvailable()) {return null;}
        SidePanelRow row = new SidePanelRow(getActivity(), number, name, captain, left);
        if (captain) {
            if (captainPlayer != null) { captainPlayer.cancelCaptain(); }
            captainPlayer = row;
        }
        playersNumbers.add(number);
        table.addView(row);
        rows.put(row.getId(), row);
        return row;
    }

    private void addRowsAuto() {
        if (!checkAddAvailable()) {return;}
        int count = playersNumbers.size();
        int number = 1;
        String name = getResources().getString(R.string.side_panel_player_name);
        while (count < Constants.MAX_PLAYERS) {
            while (playersNumbers.contains(number)) { number++; }
            SidePanelRow row = new SidePanelRow(getActivity(), number, String.format(name, number), false, left);
            playersNumbers.add(number);
            table.addView(row);
            rows.put(row.getId(), row);
            count++;
        }
    }

    public int checkNewPlayer(int number, boolean captain) {
        int status = 0;
        if (!numberAvailable(number)) { status = 1; }
        if (!(!captain || captainNotAssigned())) { status |= 2; }
        return status;
    }

    public boolean captainNotAssigned() {
        return captainPlayer == null;
    }

    public boolean numberAvailable(int number) {
        return !(playersNumbers.contains(number));
    }

    public TreeMap<Integer, SidePanelRow> getInactivePlayers() {
        TreeMap<Integer, SidePanelRow> res = new TreeMap<>();
        for (TreeMap.Entry<Integer, SidePanelRow> entry : rows.entrySet()) {
            if (!activePlayers.contains(entry.getValue())) {
                res.put(entry.getValue().getNumber(), entry.getValue());
            }
        }
        return res;
    }

    public TreeMap<Integer, SidePanelRow> getAllPlayers() {
        return rows;
    }

    public boolean selectionConfirmed() {
        return !panelSelect.isChecked();
    }

    private boolean checkAddAvailable() {
        if (playersNumbers.size() < Constants.MAX_PLAYERS) {
            return true;
        }
        Toast.makeText(getActivity(), getResources().getString(R.string.side_panel_players_limit), Toast.LENGTH_LONG).show();
        return false;
    }

    public void substitute(SidePanelRow in, SidePanelRow out){
        if (out != null) {
            out.toggleSelected();
            activePlayers.remove(out);
        }
        in.toggleSelected();
        activePlayers.add(in);
    }

    public void clear() {
        Fragment frag = getFragmentManager().findFragmentByTag(ListDialog.TAG);
        if (frag != null && frag.isAdded()) {
            return;
        }
        ListDialog.newInstance("clear_panel", left).show(getFragmentManager(), ListDialog.TAG);
    }

    public void clear(boolean delete) {
        if (delete) {
            rows.clear();
            playersNumbers.clear();
            activePlayers.clear();
            if (table != null) {
                table.removeAllViews();
                addHeader();
            }
            listener.onSidePanelNoActive(left);
        } else {
            for (Map.Entry<Integer, SidePanelRow> entry : rows.entrySet()) {
                entry.getValue().clear();
            }
        }
    }

    public boolean saveCurrentData(SharedPreferences statePref) {
        SharedPreferences.Editor editor = statePref.edit();
        Set<String> activePlayersNumbers = new TreeSet<>();
        for (SidePanelRow row : activePlayers) {
            activePlayersNumbers.add(Integer.toString(row.getNumber()));
        }
        editor.putStringSet((left) ? Constants.STATE_HOME_ACTIVE_PLAYERS : Constants.STATE_GUEST_ACTIVE_PLAYERS, activePlayersNumbers);
        editor.apply();
        DbHelper dbHelper = DbHelper.getInstance(MainActivity.getContext());
        String team = Boolean.toString(left);
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv;
            for (Map.Entry<Integer, SidePanelRow> entry : rows.entrySet()) {
                cv = new ContentValues();
                SidePanelRow row = entry.getValue();
                cv.put(DbScheme.ResultsPlayersTable.COLUMN_NAME_GAME_ID, -1);
                cv.put(DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_TEAM, team);
                cv.put(DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_NUMBER, row.getNumber());
                cv.put(DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_NAME, row.getName());
                cv.put(DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_POINTS, row.getPoints());
                cv.put(DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_FOULS, row.getFouls());
                cv.put(DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_CAPTAIN, (row.getCaptain()) ? 1 : 0);
                db.insertWithOnConflict(DbScheme.ResultsPlayersTable.TABLE_NAME_GAME_PLAYERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } finally {
            dbHelper.close();
        }
        return true;
    }

    public static void clearCurrentData() {
        DbHelper dbHelper = DbHelper.getInstance(MainActivity.getContext());
        SQLiteDatabase db = dbHelper.open();
        try {
            db.delete(DbScheme.ResultsPlayersTable.TABLE_NAME_GAME_PLAYERS,
                    DbScheme.ResultsPlayersTable.COLUMN_NAME_GAME_ID + " = ?",
                    new String[]{Integer.toString(-1)});
        } finally {
            dbHelper.close();
        }
    }

    public void restoreCurrentData() {
        clear(true);
        Set<String> activePlayersNumbers = new TreeSet<>();
        SharedPreferences statePref = getActivity().getPreferences(Context.MODE_PRIVATE);
        activePlayersNumbers = statePref.getStringSet((left) ? Constants.STATE_HOME_ACTIVE_PLAYERS
                                                             : Constants.STATE_GUEST_ACTIVE_PLAYERS, activePlayersNumbers);
        activePlayers = new TreeSet<>();
        DbHelper dbHelper = DbHelper.getInstance(getActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.open();
        try {
            String[] columns = new String[] {
                    DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_NUMBER,
                    DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_NAME,
                    DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_CAPTAIN,
                    DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_POINTS,
                    DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_FOULS,
            };
            String query = DbScheme.ResultsPlayersTable.COLUMN_NAME_GAME_ID + " = ? AND " +
                    DbScheme.ResultsPlayersTable.COLUMN_NAME_PLAYER_TEAM + " = ?";
            Cursor c = db.query(
                    DbScheme.ResultsPlayersTable.TABLE_NAME_GAME_PLAYERS,
                    columns,
                    query,
                    new String[]{Integer.toString(-1), Boolean.toString(left)},
                    null, null, null
            );
            if (c.getCount() > 0) {
                c.moveToFirst();
                do {
                    SidePanelRow row = addRow(c.getInt(0), c.getString(1), c.getInt(2) == 1);
                    row.changePoints(c.getInt(3));
                    row.changeFouls(c.getInt(4));
                    if (activePlayersNumbers.contains(Integer.toString(row.getNumber()))) {
                        activePlayers.add(row);
                        row.toggleSelected();
                    }
                } while (c.moveToNext());
                listener.onSidePanelActiveSelected(activePlayers, left);
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.side_panel_no_save_data), Toast.LENGTH_LONG).show();
            }
            c.close();
        } finally {
            dbHelper.close();
        }
    }

}
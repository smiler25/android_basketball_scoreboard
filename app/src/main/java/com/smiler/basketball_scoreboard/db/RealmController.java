package com.smiler.basketball_scoreboard.db;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.v4.app.Fragment;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;


public class RealmController {

    private static RealmController instance;
    private final Realm realm;

    public RealmController(Application application) {
        Realm.init(application);
        RealmConfiguration realmConfig = new RealmConfiguration.Builder()
                .name("main.realm")
                .schemaVersion(0)
                .build();

        realm = Realm.getInstance(realmConfig);
    }

    public RealmController(Context context) {
        Realm.init(context);
        RealmConfiguration realmConfig = new RealmConfiguration.Builder()
                .name("main.realm")
                .schemaVersion(0)
                .build();
        realm = Realm.getInstance(realmConfig);
    }

    public static RealmController with(Fragment fragment) {
        if (instance == null) {
            instance = new RealmController(fragment.getActivity().getApplication());
        }
        return instance;
    }

    public static RealmController with(Activity activity) {
        if (instance == null) {
            instance = new RealmController(activity.getApplication());
        }
        return instance;
    }

    public static RealmController with(Application application) {
        if (instance == null) {
            instance = new RealmController(application);
        }
        return instance;
    }

    public static RealmController with(Context context) {
        if (instance == null) {
            instance = new RealmController(context);
        }
        return instance;
    }

    public static RealmController getInstance() {
        return instance;
    }

    public Realm getRealm() {
        return realm;
    }

//    public void refresh() {
//        realm.refresh();
//    }

    public RealmResults<Results> getResults() {
        return realm.where(Results.class).findAll().sort("date", Sort.DESCENDING);
    }

    public Results getResult(int id) {
        return realm.where(Results.class).equalTo("id", id).findFirst();
    }

    public RealmResults<PlayersResults> getPlayers(int gameId) {
        return realm.where(PlayersResults.class).equalTo("game.id", gameId).findAll();
    }

//    public RealmResults<Results> queryResults() {
//        return realm.where(Results.class)
//                .contains("author", "Author 0")
//                .or()
//                .contains("title", "Realm")
//                .findAll();
//
//    }

    public void deleteResult(final int id) {
        final Results result = realm.where(Results.class).equalTo("id", id).findFirst();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                result.deleteFromRealm();
            }
        });
    }

    public void deleteResults(final Integer[] ids) {
        final RealmResults<Results> results = realm.where(Results.class).in("id", ids).findAll();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                results.deleteAllFromRealm();
            }
        });
    }

    public String getShareString(int id) {
        return realm.where(Results.class).equalTo("id", id).findFirst().getShareString();
    }
}
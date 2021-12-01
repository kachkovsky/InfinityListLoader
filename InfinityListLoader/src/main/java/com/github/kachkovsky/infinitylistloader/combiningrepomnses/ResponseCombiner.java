package com.github.kachkovsky.infinitylistloader.combiningrepomnses;

import android.content.Context;
import android.util.Log;

import com.github.kachkovsky.infinitylistloader.DataSource;

import java.util.ArrayList;
import java.util.List;

public abstract class ResponseCombiner<T> {
    private static final String LOG_TAG = "ResponseCombiner";

    protected List<Part<T>> data = new ArrayList<>();

    protected Context context;

    public ResponseCombiner(Context context) {
        this.context = context;
    }

    public boolean addPart(int index, DataSource dataSource, List<T> response, boolean removePartsAfterIndex) {
        if (index == data.size()) {
            data.add(new Part<>(response, dataSource));
            return mergeResults(index, response);
        } else if (index < data.size()) {
            //update data only from remote sources
            if (DataSource.REMOTE.equals(dataSource)) {
                data.set(index, new Part<>(response, dataSource));
                if (removePartsAfterIndex) {
                    while (index + 1 < data.size()) {
                        data.remove(index + 1);
                    }
                }
                return mergeResults(index, response);
            } else {
                return false;
            }
        } else {
            Log.w(LOG_TAG, "List very small to add this part");
            return false;
        }
    }

    public void clearData() {
        data.clear();
    }

    public abstract boolean mergeResults(int index, List<T> response);

    public abstract List<T> getResultList();

}

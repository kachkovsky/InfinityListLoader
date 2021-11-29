package com.github.kachkovsky.infinitylistloader.combiningrepomnses;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class DefaultResponseCombiner<T> extends ResponseCombiner<T> {

    private List<T> list;

    public DefaultResponseCombiner(Context context) {
        super(context);
    }

    @Override
    public boolean mergeResults(int index, List<T> response) {
        list = new ArrayList<>();
        for (Part<T> part : data) {
            list.addAll(part.getData());
        }
        return true;
    }

    @Override
    public List<T> getResultList() {
        return list;
    }

    @Override
    public void clearData() {
        super.clearData();
        list.clear();
    }
}

package com.github.kachkovsky.infinitylistloader.combiningrepomnses;

import com.github.kachkovsky.infinitylistloader.DataSource;

import java.util.List;

public class ListResult<T, E> {

    private final List<T> resultList;
    private final E errorMessage;
    private final boolean isFinished;
    private final DataSource lastDataSource;

    public ListResult(List<T> resultList, DataSource lastDataSource, boolean isFinished, E errorMessage) {
        this.resultList = resultList;
        this.lastDataSource = lastDataSource;
        this.isFinished = isFinished;
        this.errorMessage = errorMessage;
    }

    public DataSource getLastDataSource() {
        return lastDataSource;
    }

    public List<T> getResultList() {
        return resultList;
    }

    public E getErrorMessage() {
        return errorMessage;
    }

    public boolean isFinished() {
        return isFinished;
    }
}

package com.github.kachkovsky.infinitylistloader;

import java.util.List;

public class RequestResult<T, E> {

    private final boolean successful;
    private final boolean listFinished;
    private final List<T> responseList;
    private final E errorMessage;

    public RequestResult(List<T> responseList, boolean successful, boolean listFinished,  E errorMessage) {
        this.successful = successful;
        this.listFinished = listFinished;
        this.responseList = responseList;
        this.errorMessage = errorMessage;
    }

    public boolean isListFinished() {
        return listFinished;
    }

    public List<T> getResponseList() {
        return responseList;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public E getErrorMessage() {
        return errorMessage;
    }
}

package com.github.kachkovsky.infinitylistloader;

import android.os.Handler;
import android.os.Message;

import com.github.kachkovsky.infinitylistloader.combiningrepomnses.ListResult;
import com.github.kachkovsky.infinitylistloader.combiningrepomnses.ResponseCombiner;
import com.github.kachkovsky.infinitylistloader.threading.WorkerThreadAndMainHandler;

public class NetworkOnlyLoader<T, E> extends BaseLoader<T, E> {

    private static final int START_PART_TO_LOAD = 1;

    protected final SourceLoader<T, E> networkLoader;
    protected final ResponseCombiner<T> responseCombiner;
    protected Position networkPosition;
    protected int workerPartToLoad = START_PART_TO_LOAD;
    public NetworkOnlyLoader(InfinityListLoader<T, E> mainLoader, WorkerThreadAndMainHandler worker, int threadNumber, ResponseCombiner<T> responseCombiner, SourceLoader<T, E> networkLoader) {
        super(mainLoader, worker, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return true;
            }
        }, threadNumber);
        this.responseCombiner = responseCombiner;
        this.networkLoader = networkLoader;
    }

    @Override
    public void incrementPartToLoadIfNeeded() {
        if (!networkPosition.isRequestInProgress() && networkPosition.getIndexToRequest() == workerPartToLoad) {
            workerPartToLoad++;
        }
    }

    @Override
    public void loadPart() {
        doRequest(networkLoader, networkPosition);
    }

    @Override
    public void clearLoaded() {
        responseCombiner.clearData();
        workerPartToLoad = START_PART_TO_LOAD;
    }

    @Override
    public void restart() {
        loadFinished = false;
        networkPosition = new Position(DataSource.REMOTE);
        networkPosition.setRestartWaiting(networkPosition.isRequestInProgress());
        loadPart();
    }

    @Override
    protected boolean needToLoadForPosition(Position position) {
        return position.getIndexToRequest() < workerPartToLoad;
    }

    @Override
    protected void onRequestResult(Position position, RequestResult<T, E> requestResult) {
        position.setRequestInProgress(false);
        if (position.isRestartWaiting()) {
            worker.getWorkerHandler().sendEmptyMessage(InfinityListLoader.MESSAGE_LOAD_PART_IF_CAN);
        } else {
            if (position == networkPosition) {
                int index = position.getIndexToRequest();
                if (requestResult.isSuccessful()) {
                    position.incrementIndexToRequest();
                }
                if (requestResult.isSuccessful()) {
                    responseCombiner.addPart(index, position.getDataSource(), requestResult.getResponseList(), false);
                }
                loadFinished = requestResult.isListFinished();
                setListResult(new ListResult<>(responseCombiner.getResultList(), position.getDataSource(), requestResult.isListFinished(), requestResult.getErrorMessage()));

            }
            if (requestResult.isSuccessful() && !loadFinished) {
                worker.getWorkerHandler().sendEmptyMessage(InfinityListLoader.MESSAGE_LOAD_PART_IF_CAN);
            }
        }
    }
}

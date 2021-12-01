package com.github.kachkovsky.infinitylistloader;

import com.github.kachkovsky.infinitylistloader.combiningrepomnses.ListResult;
import com.github.kachkovsky.infinitylistloader.combiningrepomnses.ResponseCombiner;
import com.github.kachkovsky.infinitylistloader.threading.WorkerThreadAndMainHandler;

public class CacheOnErrorLoader<T, E> extends NetworkOnlyLoader<T, E> {

    private SourceLoader<T, E> cacheLoader;

    public CacheOnErrorLoader(InfinityListLoader<T, E> mainLoader, WorkerThreadAndMainHandler worker, int threadNumber, ResponseCombiner<T> responseCombiner, SourceLoader<T, E> networkLoader, SourceLoader<T, E> cacheLoader) {
        super(mainLoader, worker, threadNumber, responseCombiner, networkLoader);
        this.cacheLoader = cacheLoader;
    }

    @Override
    protected void onRequestResult(Position position, RequestResult<T, E> requestResult) {
        if (requestResult.isSuccessful() || requestResult.isListFinished() || position.isRestartWaiting()) {
            super.onRequestResult(position, requestResult);
        } else {
            setListResult(new ListResult<>(responseCombiner.getResultList(), position.getDataSource(), requestResult.isListFinished(), requestResult.getErrorMessage()));

            networkHandler.post(() -> {
                cacheLoader.load(position.getIndexToRequest(), (RequestResult<T, E> requestResult1) -> {
                    worker.getWorkerHandler().post(() -> {
                        onRequestResultCached(position, requestResult1);
                    });
                });
            });
        }
    }

    private void onRequestResultCached(Position position, RequestResult<T, E> requestResult) {
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
                    responseCombiner.addPart(index, DataSource.LOCAL, requestResult.getResponseList(), false);
                }
                loadFinished = requestResult.isListFinished();
                setListResult(new ListResult<>(responseCombiner.getResultList(), DataSource.LOCAL, requestResult.isListFinished(), requestResult.getErrorMessage()));

            }
            if (requestResult.isSuccessful() && !loadFinished) {
                worker.getWorkerHandler().sendEmptyMessage(InfinityListLoader.MESSAGE_LOAD_PART_IF_CAN);
            }
        }
    }
}

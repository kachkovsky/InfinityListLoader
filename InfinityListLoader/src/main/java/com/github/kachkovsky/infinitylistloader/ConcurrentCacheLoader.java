package com.github.kachkovsky.infinitylistloader;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.github.kachkovsky.infinitylistloader.combiningrepomnses.ListResult;
import com.github.kachkovsky.infinitylistloader.combiningrepomnses.ResponseCombiner;
import com.github.kachkovsky.infinitylistloader.threading.WorkerThreadAndMainHandler;

class ConcurrentCacheLoader<T, E> extends BaseLoader<T, E> {

    private static final int START_PART_TO_LOAD = 1;

    private final SourceLoader<T, E> cacheLoader;
    private final SourceLoader<T, E> networkLoader;
    private final HandlerThread cacheThread;
    private final ResponseCombiner<T> responseCombiner;
    private Handler cacheHandler;
    private Position cachePosition = new Position(DataSource.LOCAL);
    private Position networkPosition;
    private int workerPartToLoad = START_PART_TO_LOAD;
    private boolean cacheEnabled = true;

    public ConcurrentCacheLoader(InfinityListLoader<T, E> mainLoader, WorkerThreadAndMainHandler worker, int threadNumber, ResponseCombiner<T> responseCombiner, SourceLoader<T, E> networkLoader, SourceLoader<T, E> cacheLoader) {
        super(mainLoader, worker, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return true;
            }
        }, threadNumber);
        this.responseCombiner = responseCombiner;
        this.networkLoader = networkLoader;
        this.cacheLoader = cacheLoader;
        cacheThread = new HandlerThread("InfinityListLoaderCacheThread" + threadNumber);
    }

    @Override
    protected void initAndStart() {
        super.initAndStart();
        cacheThread.start();
        cacheHandler = new Handler(cacheThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return true;
            }
        });
    }

    @Override
    public void incrementPartToLoadIfNeeded() {
        boolean needIncrement = false;
        if (networkPosition.getIndexToRequest() >= cachePosition.getIndexToRequest()) {
            if (!networkPosition.isRequestInProgress() && networkPosition.getIndexToRequest() == workerPartToLoad) {
                needIncrement = true;
            }
        }
        if (cachePosition.getIndexToRequest() >= networkPosition.getIndexToRequest()) {
            if (cacheEnabled && !cachePosition.isRequestInProgress() && cachePosition.getIndexToRequest() == workerPartToLoad) {
                needIncrement = true;
            }
        }
        if (needIncrement) {
            workerPartToLoad++;
        }
    }

    @Override
    public void loadPart() {
        if (cacheEnabled) {
            doRequestIfNeeded(cachePosition);
        }
        doRequestIfNeeded(networkPosition);
    }

    @Override
    protected boolean needToLoadForPosition(Position position) {
        return position.getIndexToRequest() < workerPartToLoad;
    }

    private void doRequestIfNeeded(Position position) {
        if (DataSource.REMOTE.equals(position.getDataSource())) {
            doRequest(networkLoader, position);
        } else {
            if (needToLoadForPosition(position) && !position.isRequestInProgress() && cacheEnabled) {
                position.setRequestInProgress(true);
                cacheHandler.post(() -> {
                    cacheLoader.load(position.getIndexToRequest(), (RequestResult<T, E> requestResult) -> {
                        worker.getWorkerHandler().post(() -> {
                            onRequestResult(position, requestResult);
                        });
                    });
                });
            }
        }
    }

    @Override
    public void clearLoaded() {
        responseCombiner.clearData();
        cachePosition = new Position(DataSource.LOCAL);
        cacheEnabled = true;
        workerPartToLoad = START_PART_TO_LOAD;
    }

    @Override
    public void restart() {
        loadFinished = false;
        networkPosition = new Position(DataSource.REMOTE);
        cachePosition.setRestartWaiting(cachePosition.isRequestInProgress());
        networkPosition.setRestartWaiting(networkPosition.isRequestInProgress());
        loadPart();
    }

    @Override
    protected void onRequestResult(Position position, RequestResult<T, E> requestResult) {
        position.setRequestInProgress(false);
        if (position.isRestartWaiting()) {
            worker.getWorkerHandler().sendEmptyMessage(InfinityListLoader.MESSAGE_LOAD_PART_IF_CAN);
        } else {
            if ((position == cachePosition && cacheEnabled) || position == networkPosition) {
                int index = position.getIndexToRequest();
                if (requestResult.isSuccessful()) {
                    position.incrementIndexToRequest();
                    if (requestResult.isListFinished()) {
                        cacheEnabled = false;
                    }
                } else if (DataSource.LOCAL.equals(position.getDataSource())) {
                    cacheEnabled = false;
                }
                if (!(loadFinished && position == cachePosition)) {
                    boolean finished = requestResult.isListFinished() && DataSource.REMOTE.equals(position.getDataSource());
                    if (requestResult.isSuccessful()) {
                        responseCombiner.addPart(index, position.getDataSource(), requestResult.getResponseList(), finished);
                    }
                    loadFinished = finished;
                    E errorMessage = (position == cachePosition) ? null : requestResult.getErrorMessage();
                    setListResult(new ListResult<>(responseCombiner.getResultList(), position.getDataSource(), requestResult.isListFinished(), errorMessage));
                }
            }
            if (requestResult.isSuccessful() && !loadFinished) {
                worker.getWorkerHandler().sendEmptyMessage(InfinityListLoader.MESSAGE_LOAD_PART_IF_CAN);
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        cacheThread.quit();
    }
}

package com.github.kachkovsky.infinitylistloader;

import android.os.Handler;
import android.os.HandlerThread;

import com.github.kachkovsky.infinitylistloader.combiningrepomnses.ListResult;
import com.github.kachkovsky.infinitylistloader.threading.WorkerThreadAndMainHandler;

abstract class BaseLoader<T, E> {

    protected HandlerThread networkThread;
    protected Handler networkHandler;

    protected boolean loadFinished;
    protected WorkerThreadAndMainHandler worker;
    protected Handler.Callback callback;
    protected InfinityListLoader<T, E> mainLoader;

    public BaseLoader(InfinityListLoader<T, E> mainLoader, WorkerThreadAndMainHandler worker, Handler.Callback callback, int threadNumber) {
        networkThread = new HandlerThread("InfinityListLoaderNetworkThread" + threadNumber);
        this.mainLoader = mainLoader;
        this.worker = worker;
        this.callback = callback;
    }

    public abstract void incrementPartToLoadIfNeeded();

    public abstract void loadPart();

    public abstract void clearLoaded();

    public abstract void restart();

    protected void initAndStart() {
        networkThread.start();
        networkHandler = new Handler(networkThread.getLooper(), callback);
    }

    protected abstract boolean needToLoadForPosition(Position position);

    protected void doRequest(SourceLoader<T, E> loader, final Position position) {
        if (needToLoadForPosition(position) && !position.isRequestInProgress()) {
            position.setRequestInProgress(true);
            networkHandler.post(() -> {
                loader.load(position.getIndexToRequest(), (RequestResult<T, E> requestResult) -> {
                    worker.getWorkerHandler().post(() -> {
                        onRequestResult(position, requestResult);
                    });
                });
            });
        }
    }

    protected abstract void onRequestResult(Position position, RequestResult<T, E> requestResult);

    protected void setListResult(ListResult<T, E> result) {
        worker.getMainHandler().post(() -> {
            mainLoader.onSetListResult(result);
        });
    }


    public boolean isLoadFinished() {
        return loadFinished;
    }

    public void dispose() {
        networkThread.quit();
    }

    protected static class Position {

        private final DataSource dataSource;
        private int indexToRequest;
        private boolean requestInProgress;
        private boolean restartWaiting;

        Position(DataSource dataSource) {
            this.dataSource = dataSource;
            indexToRequest = 0;
            requestInProgress = false;
            restartWaiting = false;
        }

        public int getIndexToRequest() {
            return indexToRequest;
        }

        public void incrementIndexToRequest() {
            indexToRequest++;
        }

        public boolean isRequestInProgress() {
            return requestInProgress;
        }

        public void setRequestInProgress(boolean requestInProgress) {
            this.requestInProgress = requestInProgress;
        }

        public DataSource getDataSource() {
            return dataSource;
        }

        public boolean isRestartWaiting() {
            return restartWaiting;
        }

        public void setRestartWaiting(boolean restartWaiting) {
            this.restartWaiting = restartWaiting;
        }
    }
}

package com.github.kachkovsky.infinitylistloader;

import com.github.kachkovsky.infinitylistloader.combiningrepomnses.ListResult;
import com.github.kachkovsky.infinitylistloader.combiningrepomnses.ResponseCombiner;
import com.github.kachkovsky.infinitylistloader.threading.WorkerThreadAndMainHandler;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class InfinityListLoader<T, E> extends ConcurrentRepository {

    static final int MESSAGE_NEXT_PART = 1;
    static final int MESSAGE_LOAD_PART_IF_CAN = 2;
    static final int MESSAGE_RESTART = 8;
    static final int MESSAGE_RESTART_AND_CLEAR_LOADED = 16;
    private static final AtomicInteger threadNumber = new AtomicInteger();

    protected WorkerThreadAndMainHandler worker;
    private boolean loading = false;
    private BaseLoader<T, E> loader;
    private ListResult<T, E> result;

    InfinityListLoader(int threadNumber) {
        result = new ListResult<>(new ArrayList<>(), null, false, null);
        worker = new WorkerThreadAndMainHandler();
        worker.initAndStart("InfinityListWorkerThread" + threadNumber, msg -> {
            switch (msg.what) {
                case MESSAGE_NEXT_PART:
                    if (!loader.isLoadFinished()) {
                        loader.incrementPartToLoadIfNeeded();
                    }
                case MESSAGE_LOAD_PART_IF_CAN:
                    if (!loader.isLoadFinished()) {
                        loader.loadPart();
                    }
                    break;
                case MESSAGE_RESTART_AND_CLEAR_LOADED:
                    loader.clearLoaded();
                case MESSAGE_RESTART:
                    loader.restart();
                    break;
            }
            return true;
        });

    }

    public static <T, E> InfinityListLoader<T, E> createConcurrentCacheLoader(ResponseCombiner<T> responseCombiner, SourceLoader<T, E> networkLoader, SourceLoader<T, E> cacheLoader) {
        int i = threadNumber.addAndGet(1);
        InfinityListLoader<T, E> loader = new InfinityListLoader<>(i);
        ConcurrentCacheLoader<T, E> ccl = new ConcurrentCacheLoader<>(loader, loader.worker, i, responseCombiner, networkLoader, cacheLoader);
        loader.setupLoader(ccl);
        return loader;
    }

    public static <T, E> InfinityListLoader<T, E> createNetworkOnlyLoader(ResponseCombiner<T> responseCombiner, SourceLoader<T, E> networkLoader) {
        int i = threadNumber.addAndGet(1);
        InfinityListLoader<T, E> loader = new InfinityListLoader<>(i);
        NetworkOnlyLoader<T, E> nol = new NetworkOnlyLoader<>(loader, loader.worker, i, responseCombiner, networkLoader);
        loader.setupLoader(nol);
        return loader;
    }

    public static <T, E> InfinityListLoader<T, E> createCacheOnErrorLoader(ResponseCombiner<T> responseCombiner, SourceLoader<T, E> networkLoader, SourceLoader<T, E> cacheLoader) {
        int i = threadNumber.addAndGet(1);
        InfinityListLoader<T, E> loader = new InfinityListLoader<>(i);
        CacheOnErrorLoader<T, E> ccl = new CacheOnErrorLoader<>(loader, loader.worker, i, responseCombiner, networkLoader, cacheLoader);
        loader.setupLoader(ccl);
        return loader;
    }

    void setupLoader(BaseLoader<T, E> loader) {
        this.loader = loader;
        this.loader.initAndStart();
    }

    /**
     * Load next part
     *
     * @return true if the load of the next part starts
     */
    public boolean loadNextPart() {
        boolean loading = this.loading;
        if (!this.loading) {
            this.loading = true;
            worker.getWorkerHandler().sendEmptyMessage(MESSAGE_NEXT_PART);
        }
        return loading;
    }

    public void updateLoadedParts() {
        worker.getWorkerHandler().sendEmptyMessage(MESSAGE_RESTART);
    }

    public void clearLoadedAndStartLoad() {
        worker.getWorkerHandler().sendEmptyMessage(MESSAGE_RESTART_AND_CLEAR_LOADED);
    }

    void onSetListResult(ListResult<T, E> result) {
        this.result = result;
        loading = false;
        notifyObservers();
    }

    public ListResult<T, E> getResult() {
        return result;
    }

    public void dispose() {
        loader.dispose();
        worker.dispose();
    }
}

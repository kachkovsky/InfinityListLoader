package com.github.kachkovsky.infinitylistloader.threading;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class WorkerThreadAndMainHandler {

    private Handler mainHandler;
    private HandlerThread workerThread;
    private Handler workerHandler;

    public void initAndStart(String workerThreadName, Handler.Callback workerCallback) {
        mainHandler = new Handler(Looper.getMainLooper());
        workerThread = new HandlerThread(workerThreadName);
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper(), workerCallback);
    }

    public void dispose() {
        workerThread.quit();
        mainHandler.removeCallbacksAndMessages(null);
    }

    public Handler getMainHandler() {
        return mainHandler;
    }

    public Handler getWorkerHandler() {
        return workerHandler;
    }
}

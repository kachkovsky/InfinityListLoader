package com.github.kachkovsky.infinitylistloader;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ConcurrentRepository  {

    private final List<Updatable> mObservers = new CopyOnWriteArrayList<>();

    public synchronized void addUpdatable(Updatable updatable) {
        if (!mObservers.contains(updatable)) {
            mObservers.add(updatable);
        }
    }

    public void removeUpdatable(Updatable updatable) {
        mObservers.remove(updatable);
    }

    protected void notifyObservers() {
        for (Updatable observer : mObservers) {
            observer.update();
        }
    }

    public interface Updatable {
        void update();
    }
}

package com.github.kachkovsky.infinitylistloader;

public interface SourceLoader<T, E> {
    void load(int partIndex, RRConsumer<RequestResult<T, E>> callback);

    interface RRConsumer<T> {
        void accept(T t);
    }
}

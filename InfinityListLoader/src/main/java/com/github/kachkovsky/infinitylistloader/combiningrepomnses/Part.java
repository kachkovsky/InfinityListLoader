package com.github.kachkovsky.infinitylistloader.combiningrepomnses;

import com.github.kachkovsky.infinitylistloader.DataSource;

import java.util.List;

public class Part<T> {
    private List<T> data;
    private DataSource dataSource;

    public Part(List<T> data, DataSource dataSource) {
        this.data = data;
        this.dataSource = dataSource;
    }

    public List<T> getData() {
        return data;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}

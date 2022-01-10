package com.book.mmbookstore.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class ExecutorTasks {

    private final ExecutorService executors;

    public ExecutorTasks() {
        this.executors = Executors.newSingleThreadExecutor();
    }

    private void startBackground() {
        onPreExecute();
        executors.execute(() -> {
            doInBackground();
            new Handler(Looper.getMainLooper()).post(this::onPostExecute);
        });
    }

    public void execute() {
        startBackground();
    }

    public void shutdown() {
        executors.shutdown();
    }

    public boolean isShutdown() {
        return executors.isShutdown();
    }

    public abstract void onPreExecute();

    public abstract void doInBackground();

    public abstract void onPostExecute();

}
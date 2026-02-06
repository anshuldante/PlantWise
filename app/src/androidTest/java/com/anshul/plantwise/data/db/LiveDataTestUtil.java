package com.anshul.plantwise.data.db;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class to get values from LiveData in tests.
 */
public class LiveDataTestUtil {

    /**
     * Get the value from a LiveData object. We're waiting for LiveData to emit,
     * for a maximum of 2 seconds.
     */
    public static <T> T getValue(final LiveData<T> liveData) throws InterruptedException {
        final AtomicReference<T> data = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T value) {
                data.set(value);
                latch.countDown();
                liveData.removeObserver(this);
            }
        };

        liveData.observeForever(observer);

        if (!latch.await(2, TimeUnit.SECONDS)) {
            liveData.removeObserver(observer);
            throw new RuntimeException("LiveData value was never set within timeout");
        }

        return data.get();
    }
}

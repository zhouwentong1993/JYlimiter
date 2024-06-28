package com.wentong.window;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 滑动窗口基础结构
 */
public class LeapArray<T> {

    @Getter
    private final int buckets;
    @Getter
    private final int totalTimesInMills;
    private final int perBucketsTimes;
    private final AtomicReferenceArray<WindowWrap<T>> array;

    private final Lock updateLock = new ReentrantLock();

    public LeapArray(int buckets, int totalTimesInMills) {
        this.buckets = buckets;
        this.totalTimesInMills = totalTimesInMills;
        this.perBucketsTimes = totalTimesInMills / buckets;
        this.array = new AtomicReferenceArray<>(buckets);
    }

    public WindowWrap<T> currentWindow() {
        long now = System.currentTimeMillis();
        // 定位到 bucket 下标，也就是数组下标
        int idx = (int) ((now / perBucketsTimes) % (array.length()));
        long startIndex = now - now % perBucketsTimes;
        WindowWrap<T> source = array.get(idx);
        while (true) {
            if (source == null) {
                WindowWrap<T> windowWrap = new WindowWrap<T>(now, null);
                boolean success = array.compareAndSet(idx, null, windowWrap);
                if (!success) {
                    Thread.yield();
                } else {
                    return windowWrap;
                }
            } else if (source.getWindowStart() == startIndex) { // 代表正好在 bucket 中
                return source;
            } else if (source.getWindowStart() < startIndex) { // 代表已经过期了
                if (updateLock.tryLock()) {
                    source.setWindowStart(startIndex);
                    source.setData(null);
                } else {
                    Thread.yield();
                }
            }
        }
    }

    public List<T> values() {
        currentWindow();
        long now = System.currentTimeMillis();
        int length = array.length();
        List<T> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            WindowWrap<T> tWindowWrap = array.get(i);
            if (tWindowWrap != null && notDeprecated(now, tWindowWrap)) {
                list.add(tWindowWrap.getData());
            }
        }
        return list;
    }

    private boolean notDeprecated(long now, WindowWrap<T> tWindowWrap) {
        return now - tWindowWrap.getWindowStart() > perBucketsTimes;
    }

}

package com.wentong.window;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 每一个窗口的描述信息
 */
@Data
@AllArgsConstructor
public class WindowWrap<T> {

    private long windowStart;
    private T data;

}

package com.xiexy.base.utils;

import java.io.Closeable;
import java.io.IOException;

public final class Closeables
{
    private Closeables()
    {
    }
    // 其中close()方法是关闭流并且释放与其相关的任何方法，如果流已被关闭，那么调用此方法没有效果。
    public static void closeQuietly(Closeable closeable)
    {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        }
        catch (IOException ignored) {
        }
    }
}
package com.xiexy.base.impl;

public interface LogMonitor
{
    void corruption(long bytes, String reason);

    void corruption(long bytes, Throwable reason);
}

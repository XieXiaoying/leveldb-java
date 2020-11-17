package com.xiexy.base.impl;

import com.google.common.collect.PeekingIterator;

import java.util.Map;

public interface SeekingIterator<K, V> extends PeekingIterator<Map.Entry<K, V>>
{
    /**
     * 将迭代器重置到block的起始位置
     */
    void seekToFirst();

    /**
     * 下一个元素的key大于等于当前指定的key
     */
    void seek(K targetKey);
}
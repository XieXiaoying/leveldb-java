package com.xiexy.base.impl;

import com.google.common.collect.PeekingIterator;

import java.util.Map;

public interface SeekingIterator<K, V> extends PeekingIterator<Map.Entry<K, V>>
{
    /**
     * 下一个元素的key大于等于当前指定的key
     * Repositions the iterator so the beginning of this block.
     */
    void seekToFirst();

    /**
     * 下一个元素的key大于等于当前指定的key
     */
    void seek(K targetKey);
}
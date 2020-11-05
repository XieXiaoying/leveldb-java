package com.xiexy.base.impl;

import java.util.Map;

public interface SeekingIterable<K, V> extends Iterable<Map.Entry<K, V>> {
    @Override
    SeekingIterator<K, V> iterator();
}
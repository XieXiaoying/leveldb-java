package com.xiexy.base.utils;

import com.xiexy.base.impl.SeekingIterator;

import java.util.Map;
import java.util.NoSuchElementException;

public abstract class AbstractSeekingIterator<K, V>
        implements SeekingIterator<K, V>
{
    private Map.Entry<K, V> nextElement;

    @Override
    public final void seekToFirst()
    {
        nextElement = null;
        seekToFirstInternal();
    }

    @Override
    public final void seek(K targetKey)
    {
        nextElement = null;
        seekInternal(targetKey);
    }

    @Override
    public final boolean hasNext()
    {
        if (nextElement == null) {
            nextElement = getNextElement();
        }
        return nextElement != null;
    }

    @Override
    public final Map.Entry<K, V> next()
    {
        if (nextElement == null) {
            nextElement = getNextElement();
            if (nextElement == null) {
                throw new NoSuchElementException();
            }
        }

        Map.Entry<K, V> result = nextElement;
        nextElement = null;
        return result;
    }

    @Override
    public final Map.Entry<K, V> peek()
    {
        if (nextElement == null) {
            nextElement = getNextElement();
            if (nextElement == null) {
                throw new NoSuchElementException();
            }
        }

        return nextElement;
    }

    @Override
    public final void remove()
    {
        throw new UnsupportedOperationException();
    }

    protected abstract void seekToFirstInternal();

    protected abstract void seekInternal(K targetKey);

    protected abstract Map.Entry<K, V> getNextElement();
}

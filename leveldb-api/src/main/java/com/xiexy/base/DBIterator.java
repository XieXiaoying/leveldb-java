package com.xiexy.base;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;

public interface DBIterator
        extends Iterator<Map.Entry<byte[], byte[]>>, Closeable
{
    /**
     * Repositions the iterator so the key of the next BlockElement
     * returned greater than or equal to the specified targetKey.
     */
    void seek(byte[] key);

    /**
     * Repositions the iterator so is is at the beginning of the Database.
     */
    void seekToFirst();

    /**
     * Returns the next element in the iteration, without advancing the iteration.
     */
    Map.Entry<byte[], byte[]> peekNext();

    /**
     * @return true if there is a previous entry in the iteration.
     */
    boolean hasPrev();

    /**
     * @return the previous element in the iteration and rewinds the iteration.
     */
    Map.Entry<byte[], byte[]> prev();

    /**
     * @return the previous element in the iteration, without rewinding the iteration.
     */
    Map.Entry<byte[], byte[]> peekPrev();

    /**
     * Repositions the iterator so it is at the end of of the Database.
     */
    void seekToLast();
}

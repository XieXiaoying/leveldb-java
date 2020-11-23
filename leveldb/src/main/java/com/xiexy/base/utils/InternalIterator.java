package com.xiexy.base.utils;

import com.xiexy.base.impl.InternalKey;
import com.xiexy.base.impl.SeekingIterator;
import com.xiexy.base.include.Slice;

/**
 * <p>A common interface for internal iterators.</p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface InternalIterator
        extends SeekingIterator<InternalKey, Slice>
{
}

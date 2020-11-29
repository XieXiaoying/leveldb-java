package com.xiexy.base.utils;

import com.xiexy.base.impl.InternalKey;
import com.xiexy.base.impl.SeekingIterator;
import com.xiexy.base.include.Slice;


public interface InternalIterator
        extends SeekingIterator<InternalKey, Slice>
{
}

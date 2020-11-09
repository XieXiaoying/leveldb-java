package com.xiexy.base.table;

import com.xiexy.base.include.Slice;

import java.util.Comparator;

public interface UserComparator
        extends Comparator<Slice>
{
    String name();

    Slice findShortestSeparator(Slice start, Slice limit);

    Slice findShortSuccessor(Slice key);
}
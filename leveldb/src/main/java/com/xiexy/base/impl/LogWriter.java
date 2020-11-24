package com.xiexy.base.impl;

import com.xiexy.base.include.Slice;

import java.io.File;
import java.io.IOException;

public interface LogWriter
{
    boolean isClosed();

    void close()
            throws IOException;

    void delete()
            throws IOException;

    File getFile();

    long getFileNumber();

    // Writes a stream of chunks such that no chunk is split across a block boundary
    void addRecord(Slice record, boolean force)
            throws IOException;
}

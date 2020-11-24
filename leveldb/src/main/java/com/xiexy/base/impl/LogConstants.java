package com.xiexy.base.impl;

import static com.xiexy.base.utils.DataUnit.INT_UNIT;
import static com.xiexy.base.utils.DataUnit.BYTE_UNIT;
import static com.xiexy.base.utils.DataUnit.SHORT_UNIT;

public final class LogConstants
{
    // todo find new home for these

    public static final int BLOCK_SIZE = 32768;

    // Header is checksum (4 bytes), type (1 byte), length (2 bytes).
    public static final int HEADER_SIZE = INT_UNIT + BYTE_UNIT + SHORT_UNIT;

    private LogConstants()
    {
    }
}
package com.xiexy.base.impl;

import static com.xiexy.base.utils.DataUnit.INT_UNIT;
import static com.xiexy.base.utils.DataUnit.BYTE_UNIT;
import static com.xiexy.base.utils.DataUnit.SHORT_UNIT;

/**
 * log的格式如下
 * | crc32 | length | log type | data |
 * 其中，crc32占4 byte，length占1byte，type占4byte，这三个作为log头，共占据7byte
 */
public final class LogConstants
{
    // todo find new home for these

    public static final int BLOCK_SIZE = 32768;

    // 占7byte的log头
    public static final int HEADER_SIZE = INT_UNIT + BYTE_UNIT + SHORT_UNIT;

    private LogConstants()
    {
    }
}
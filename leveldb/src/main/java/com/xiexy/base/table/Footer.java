package com.xiexy.base.table;

import com.xiexy.base.db.Slices;
import com.xiexy.base.include.Slice;
import com.xiexy.base.include.SliceInput;
import com.xiexy.base.include.SliceOutput;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static com.xiexy.base.utils.DataUnit.LONG_UNIT;
import static com.xiexy.base.table.BlockHandle.readBlockHandle;
import static com.xiexy.base.table.BlockHandle.writeBlockHandleTo;

/**
 * Footer位于文件的最后，大小固定，其格式如下所示。
 *
 * 成员metaindexBlockHandle指出了meta index block的起始位置和大小；成员indexBlockHandle指出了index block的起始地址和大小；
 * 这两个字段都是BlockHandle对象，可以理解为索引的索引，通过Footer可以直接定位到metaindex和index block。再后面是一个填充区和魔数
 */
public class Footer
{
    // 预算出Footer需要的空间，包括2个BlockHandle的空间 + 填充区（数据不够MAX_ENCODED_LENGTH * 2时，需要填充完整） + 魔数
    public static final int ENCODED_LENGTH = (BlockHandle.MAX_ENCODED_LENGTH * 2) + LONG_UNIT;

    private final BlockHandle metaindexBlockHandle;
    private final BlockHandle indexBlockHandle;

    Footer(BlockHandle metaindexBlockHandle, BlockHandle indexBlockHandle)
    {
        this.metaindexBlockHandle = metaindexBlockHandle;
        this.indexBlockHandle = indexBlockHandle;
    }

    public BlockHandle getMetaindexBlockHandle()
    {
        return metaindexBlockHandle;
    }

    public BlockHandle getIndexBlockHandle()
    {
        return indexBlockHandle;
    }

    public static Footer readFooter(Slice slice)
    {
        requireNonNull(slice, "slice is null");
        checkArgument(slice.length() == ENCODED_LENGTH,
                "Expected slice.size to be %s but was %s", ENCODED_LENGTH, slice.length());

        SliceInput sliceInput = slice.input();

        // 读取metaindexBlockHandle和indexBlockHandle
        BlockHandle metaindexBlockHandle = readBlockHandle(sliceInput);
        BlockHandle indexBlockHandle = readBlockHandle(sliceInput);

        // 跳过填充区域
        sliceInput.setPosition(ENCODED_LENGTH - LONG_UNIT);

        // 验证魔数
        long magicNumber = sliceInput.readUnsignedInt() | (sliceInput.readUnsignedInt() << 32);
        checkArgument(magicNumber == TableBuilder.TABLE_MAGIC_NUMBER,
                "File is not a table (bad magic number)");

        return new Footer(metaindexBlockHandle, indexBlockHandle);
    }

    public static Slice writeFooter(Footer footer)
    {
        Slice slice = Slices.allocate(ENCODED_LENGTH);
        writeFooter(footer, slice.output());
        return slice;
    }

    public static void writeFooter(Footer footer, SliceOutput sliceOutput)
    {
        // startingWriteIndex指向padding开始的地方
        int startingWriteIndex = sliceOutput.size();

        // 写 metaindex 和 index handles
        writeBlockHandleTo(footer.getMetaindexBlockHandle(), sliceOutput);
        writeBlockHandleTo(footer.getIndexBlockHandle(), sliceOutput);

        // 写填充区 padding
        sliceOutput.writeZero(ENCODED_LENGTH - LONG_UNIT - (sliceOutput.size() - startingWriteIndex));

        // 写入魔数
        sliceOutput.writeInt((int) TableBuilder.TABLE_MAGIC_NUMBER);
        sliceOutput.writeInt((int) (TableBuilder.TABLE_MAGIC_NUMBER >>> 32));
    }
}

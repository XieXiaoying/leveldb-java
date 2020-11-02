package com.xiexy.base.table;

import com.google.common.primitives.Ints;
import com.xiexy.base.include.DynamicSliceOutput;
import com.xiexy.base.include.Slice;
import com.xiexy.base.utils.Coding;
import com.xiexy.base.utils.IntVector;
import static com.xiexy.base.utils.DataUnit.INT_UNIT;

import java.util.Comparator;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class BlockBuilder {
    private final int blockRestartInterval;
    // 重启点
    private final IntVector restartPositions;
    private final Comparator<Slice> comparator;
    // 重启后生成的entry数
    private int entryCount;
    private int restartBlockEntryCount;
    // 是否构建完成
    private boolean finished;
    // block的内容
    private final DynamicSliceOutput block;
    // 记录最后添加的key
    private Slice lastKey;

    public BlockBuilder(int estimatedSize, int blockRestartInterval, Comparator<Slice> comparator)
    {
        checkArgument(estimatedSize >= 0, "estimatedSize is negative");
        checkArgument(blockRestartInterval >= 0, "blockRestartInterval is negative");
        requireNonNull(comparator, "comparator is null");

        this.block = new DynamicSliceOutput(estimatedSize);
        this.blockRestartInterval = blockRestartInterval;
        this.comparator = comparator;

        restartPositions = new IntVector(32);
        // 第一个重启点必须是0
        restartPositions.add(0);
    }
    // 重设内容，通常在Finish之后调用已构建新的block
    public void reset()
    {
        block.reset();
        entryCount = 0;
        restartPositions.clear();
        // 第一个重启点必须是0
        restartPositions.add(0);
        restartBlockEntryCount = 0;
        lastKey = null;
        finished = false;
    }
    public int getEntryCount()
    {
        return entryCount;
    }
    // 没有entry则返回true
    public boolean isEmpty()
    {
        return entryCount == 0;
    }

    // 返回正在构建block的未压缩大小—估计值
    public int currentSizeEstimate()
    {
        // no need to estimate if closed
        if (finished) {
            return block.size();
        }

        // no records is just a single int
        if (block.size() == 0) {
            return INT_UNIT;
        }

        return block.size() +                              // raw data buffer
                restartPositions.size() * INT_UNIT +    // restart positions
                INT_UNIT;                               // restart position size
    }

    public void add(BlockEntry blockEntry)
    {
        requireNonNull(blockEntry, "blockEntry is null");
        add(blockEntry.getKey(), blockEntry.getValue());
    }
    //添加k/v，要求：Reset()之后没有调用过Finish()；Key > 任何已加入的key
    public void add(Slice key, Slice value)
    {
        requireNonNull(key, "key is null");
        requireNonNull(value, "value is null");
        checkState(!finished, "block is finished");
        checkPositionIndex(restartBlockEntryCount, blockRestartInterval);

        checkArgument(lastKey == null || comparator.compare(key, lastKey) > 0, "key must be greater than last key");

        int sharedKeyBytes = 0;
        if (restartBlockEntryCount < blockRestartInterval) {
            sharedKeyBytes = calculateSharedBytes(key, lastKey);
        }
        else {
            // restart prefix compression
            restartPositions.add(block.size());
            restartBlockEntryCount = 0;
        }

        int nonSharedKeyBytes = key.length() - sharedKeyBytes;

        // write "<shared><non_shared><value_size>"
        Coding.encodeInt(sharedKeyBytes, block);
        Coding.encodeInt(nonSharedKeyBytes, block);
        Coding.encodeInt(value.length(), block);

        // write non-shared key bytes
        block.writeBytes(key, sharedKeyBytes, nonSharedKeyBytes);

        // write value bytes
        block.writeBytes(value, 0, value.length());

        // update last key
        lastKey = key;

        // update state
        entryCount++;
        restartBlockEntryCount++;
    }

    public static int calculateSharedBytes(Slice leftKey, Slice rightKey)
    {
        int sharedKeyBytes = 0;

        if (leftKey != null && rightKey != null) {
            int minSharedKeyBytes = Ints.min(leftKey.length(), rightKey.length());
            while (sharedKeyBytes < minSharedKeyBytes && leftKey.getByte(sharedKeyBytes) == rightKey.getByte(sharedKeyBytes)) {
                sharedKeyBytes++;
            }
        }

        return sharedKeyBytes;
    }
    /**
     * 结束构建block，并返回指向block内容的指针
     * @return Slice的生存周期：Builder的生存周期，or直到Reset()被调用
     */
    public Slice finish()
    {
        if (!finished) {
            finished = true;

            if (entryCount > 0) {
                restartPositions.write(block);
                block.writeInt(restartPositions.size());
            }
            else {
                block.writeInt(0);
            }
        }
        return block.slice();
    }
}

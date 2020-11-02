package com.xiexy.base.table;

import com.xiexy.base.db.Slices;
import com.xiexy.base.impl.SeekingIterator;
import com.xiexy.base.include.Slice;
import com.xiexy.base.include.SliceInput;
import com.xiexy.base.include.SliceOutput;
import com.xiexy.base.utils.Coding;

import java.util.Comparator;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.*;
import static com.xiexy.base.utils.DataUnit.INT_UNIT;
import static java.util.Objects.requireNonNull;

public class BlockIterator
        implements SeekingIterator<Slice, Slice>
{
    private final SliceInput data;
    private final Slice restartPositions;
    private final int restartCount;
    private final Comparator<Slice> comparator;

    private BlockEntry nextEntry;

    public BlockIterator(Slice data, Slice restartPositions, Comparator<Slice> comparator)
    {
        requireNonNull(data, "data is null");
        requireNonNull(restartPositions, "restartPositions is null");
        checkArgument(restartPositions.length() % INT_UNIT == 0, "restartPositions.readableBytes() must be a multiple of %s", INT_UNIT);
        requireNonNull(comparator, "comparator is null");

        this.data = data.input();

        this.restartPositions = restartPositions.slice();
        // 重启点的长度 / int的长度，得到重启点的个数
        restartCount = this.restartPositions.length() / INT_UNIT;

        this.comparator = comparator;
        // 将迭代器至于起始位置
        seekToFirst();
    }

    @Override
    public boolean hasNext()
    {
        return nextEntry != null;
    }

    @Override
    public BlockEntry peek()
    {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return nextEntry;
    }

    @Override
    public BlockEntry next()
    {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        BlockEntry entry = nextEntry;

        if (!data.isReadable()) {
            nextEntry = null;
        }
        else {
            // 在当前位置读取Entry
            nextEntry = readEntry(data, nextEntry);
        }

        return entry;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * 通过设置重启点的偏移量来决定访问的数据
     */
    @Override
    public void seekToFirst()
    {
        if (restartCount > 0) {
            seekToRestartPosition(0);
        }
    }

    /**
     * Repositions the iterator so the key of the next BlockElement returned greater than or equal to the specified targetKey.
     */
    @Override
    public void seek(Slice targetKey)
    {
        if (restartCount == 0) {
            return;
        }

        int left = 0;
        int right = restartCount - 1;

        // binary search restart positions to find the restart position immediately before the targetKey
        while (left < right) {
            int mid = (left + right + 1) / 2;

            seekToRestartPosition(mid);

            if (comparator.compare(nextEntry.getKey(), targetKey) < 0) {
                // key at mid is smaller than targetKey.  Therefore all restart
                // blocks before mid are uninteresting.
                left = mid;
            }
            else {
                // key at mid is greater than or equal to targetKey.  Therefore
                // all restart blocks at or after mid are uninteresting.
                right = mid - 1;
            }
        }

        // linear search (within restart block) for first key greater than or equal to targetKey
        for (seekToRestartPosition(left); nextEntry != null; next()) {
            if (comparator.compare(peek().getKey(), targetKey) >= 0) {
                break;
            }
        }

    }

    /**
     * Seeks to and reads the entry at the specified restart position.
     * <p/>
     * After this method, nextEntry will contain the next entry to return, and the previousEntry will be null.
     */
    private void seekToRestartPosition(int restartPosition)
    {
        checkPositionIndex(restartPosition, restartCount, "restartPosition");

        // seek data readIndex to the beginning of the restart block
        int offset = restartPositions.getInt(restartPosition * INT_UNIT);
        data.setPosition(offset);

        // clear the entries to assure key is not prefixed
        nextEntry = null;

        // read the entry
        nextEntry = readEntry(data, null);
    }

    /**
     * Reads the entry at the current data readIndex.
     * After this method, data readIndex is positioned at the beginning of the next entry
     * or at the end of data if there was not a next entry.
     *
     * @return true if an entry was read
     */
    private static BlockEntry readEntry(SliceInput data, BlockEntry previousEntry)
    {
        requireNonNull(data, "data is null");

        // 读取Block当前key的共享前缀长度， 前缀之后的字符串长度，值的长度
        int sharedKeyLength = Coding.decodeInt(data);
        int nonSharedKeyLength = Coding.decodeInt(data);
        int valueLength = Coding.decodeInt(data);

        // read key
        final Slice key;
        if (sharedKeyLength > 0) {
            key = Slices.allocate(sharedKeyLength + nonSharedKeyLength);
            SliceOutput sliceOutput = key.output();
            checkState(previousEntry != null, "Entry has a shared key but no previous entry was provided");
            // 第0个
            sliceOutput.writeBytes(previousEntry.getKey(), 0, sharedKeyLength);
            sliceOutput.writeBytes(data, nonSharedKeyLength);
        }
        else {
            key = data.readSlice(nonSharedKeyLength);
        }
        // read value
        Slice value = data.readSlice(valueLength);

        return new BlockEntry(key, value);
    }
}

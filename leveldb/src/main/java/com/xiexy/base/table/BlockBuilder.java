package com.xiexy.base.table;

import com.google.common.primitives.Ints;
import com.xiexy.base.include.Slice;
import com.xiexy.base.utils.IntVector;

import java.util.Comparator;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class BlockBuilder {
    private final int blockRestartInterval;
    private final IntVector restartPositions;
    private final Comparator<Slice> comparator;

    private int entryCount;
    private int restartBlockEntryCount;

    private boolean finished;
    private final DynamicSliceOutput block;
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
        restartPositions.add(0);  // first restart point must be 0
    }

    public void reset()
    {
        block.reset();
        entryCount = 0;
        restartPositions.clear();
        restartPositions.add(0); // first restart point must be 0
        restartBlockEntryCount = 0;
        lastKey = null;
        finished = false;
    }

    public int getEntryCount()
    {
        return entryCount;
    }

    public boolean isEmpty()
    {
        return entryCount == 0;
    }

    public int currentSizeEstimate()
    {
        // no need to estimate if closed
        if (finished) {
            return block.size();
        }

        // no records is just a single int
        if (block.size() == 0) {
            return SIZE_OF_INT;
        }

        return block.size() +                              // raw data buffer
                restartPositions.size() * SIZE_OF_INT +    // restart positions
                SIZE_OF_INT;                               // restart position size
    }

    public void add(BlockEntry blockEntry)
    {
        requireNonNull(blockEntry, "blockEntry is null");
        add(blockEntry.getKey(), blockEntry.getValue());
    }

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
        VariableLengthQuantity.writeVariableLengthInt(sharedKeyBytes, block);
        VariableLengthQuantity.writeVariableLengthInt(nonSharedKeyBytes, block);
        VariableLengthQuantity.writeVariableLengthInt(value.length(), block);

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

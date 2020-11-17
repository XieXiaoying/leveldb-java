package com.xiexy.base.utils;

import com.xiexy.base.include.Slice;
import com.xiexy.base.table.Block;
import com.xiexy.base.table.BlockIterator;
import com.xiexy.base.table.Table;

import java.util.Map;

public final class TableIterator
        extends AbstractSeekingIterator<Slice, Slice>
{
    private final Table table;
    /**
     * 各种Block的存储格式都是相同的，但是各自block data存储的k/v又互不相同，于是我们就需要一个途径，
     * 能够在使用同一个方式遍历不同的block时，又能解析这些k/v。
     */
    private final BlockIterator blockIterator;
    /**
     * 遍历block data的迭代器
     */
    private BlockIterator current;

    public TableIterator(Table table, BlockIterator blockIterator)
    {
        this.table = table;
        this.blockIterator = blockIterator;
        current = null;
    }

    @Override
    protected void seekToFirstInternal()
    {
        // 重置index到data block的起始位置
        blockIterator.seekToFirst();
        current = null;
    }

    @Override
    protected void seekInternal(Slice targetKey)
    {
        // 这里并不是精确的定位，而是在Table中找到第一个>=指定key的k/v对
        blockIterator.seek(targetKey);

        // 如果iterator没有next，那么key不包含在iterator中
        if (blockIterator.hasNext()) {
            // 找到current的迭代器
            current = getNextBlock();
            current.seek(targetKey);
        }
        else {
            current = null;
        }
    }

    @Override
    protected Map.Entry<Slice, Slice> getNextElement()
    {
        boolean currentHasNext = false;
        while (true) {
            if (current != null) {
                currentHasNext = current.hasNext();
            }
            if (!(currentHasNext)) {
                if (blockIterator.hasNext()) {
                    current = getNextBlock();
                }
                else {
                    break;
                }
            }
            else {
                break;
            }
        }
        if (currentHasNext) {
            return current.next();
        }
        else {
            // set current to empty iterator to avoid extra calls to user iterators
            current = null;
            return null;
        }
    }

    private BlockIterator getNextBlock()
    {
        Slice blockHandle = blockIterator.next().getValue();
        Block dataBlock = table.openBlock(blockHandle);
        return dataBlock.iterator();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ConcatenatingIterator");
        sb.append("{blockIterator=").append(blockIterator);
        sb.append(", current=").append(current);
        sb.append('}');
        return sb.toString();
    }
}

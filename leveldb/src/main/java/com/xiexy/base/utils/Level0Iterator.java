package com.xiexy.base.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.xiexy.base.impl.FileMetaData;
import com.xiexy.base.impl.InternalKey;
import com.xiexy.base.impl.SeekingIterator;
import com.xiexy.base.impl.TableCache;
import com.xiexy.base.include.Slice;

import java.util.*;

public final class Level0Iterator
        extends AbstractSeekingIterator<InternalKey, Slice>
        implements InternalIterator
{
    private final List<InternalTableIterator> inputs;
    private final PriorityQueue<ComparableIterator> priorityQueue;
    private final Comparator<InternalKey> comparator;

    public Level0Iterator(TableCache tableCache, List<FileMetaData> files, Comparator<InternalKey> comparator)
    {
        ImmutableList.Builder<InternalTableIterator> builder = ImmutableList.builder();
        for (FileMetaData file : files) {
            builder.add(tableCache.newIterator(file));
        }
        this.inputs = builder.build();
        this.comparator = comparator;

        this.priorityQueue = new PriorityQueue<>(Iterables.size(inputs) + 1);
        resetPriorityQueue(comparator);
    }

    public Level0Iterator(List<InternalTableIterator> inputs, Comparator<InternalKey> comparator)
    {
        this.inputs = inputs;
        this.comparator = comparator;

        this.priorityQueue = new PriorityQueue<>(Iterables.size(inputs));
        resetPriorityQueue(comparator);
    }

    @Override
    protected void seekToFirstInternal()
    {
        for (InternalTableIterator input : inputs) {
            input.seekToFirst();
        }
        resetPriorityQueue(comparator);
    }

    @Override
    protected void seekInternal(InternalKey targetKey)
    {
        for (InternalTableIterator input : inputs) {
            input.seek(targetKey);
        }
        resetPriorityQueue(comparator);
    }

    private void resetPriorityQueue(Comparator<InternalKey> comparator)
    {
        int i = 0;
        for (InternalTableIterator input : inputs) {
            if (input.hasNext()) {
                priorityQueue.add(new ComparableIterator(input, comparator, i++, input.next()));
            }
        }
    }

    @Override
    protected Map.Entry<InternalKey, Slice> getNextElement()
    {
        Map.Entry<InternalKey, Slice> result = null;
        ComparableIterator nextIterator = priorityQueue.poll();
        if (nextIterator != null) {
            result = nextIterator.next();
            if (nextIterator.hasNext()) {
                priorityQueue.add(nextIterator);
            }
        }
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MergingIterator");
        sb.append("{inputs=").append(Iterables.toString(inputs));
        sb.append(", comparator=").append(comparator);
        sb.append('}');
        return sb.toString();
    }

    private static class ComparableIterator
            implements Iterator<Map.Entry<InternalKey, Slice>>, Comparable<ComparableIterator>
    {
        private final SeekingIterator<InternalKey, Slice> iterator;
        private final Comparator<InternalKey> comparator;
        private final int ordinal;
        private Map.Entry<InternalKey, Slice> nextElement;

        private ComparableIterator(SeekingIterator<InternalKey, Slice> iterator, Comparator<InternalKey> comparator, int ordinal, Map.Entry<InternalKey, Slice> nextElement)
        {
            this.iterator = iterator;
            this.comparator = comparator;
            this.ordinal = ordinal;
            this.nextElement = nextElement;
        }

        @Override
        public boolean hasNext()
        {
            return nextElement != null;
        }

        @Override
        public Map.Entry<InternalKey, Slice> next()
        {
            if (nextElement == null) {
                throw new NoSuchElementException();
            }

            Map.Entry<InternalKey, Slice> result = nextElement;
            if (iterator.hasNext()) {
                nextElement = iterator.next();
            }
            else {
                nextElement = null;
            }
            return result;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ComparableIterator comparableIterator = (ComparableIterator) o;

            if (ordinal != comparableIterator.ordinal) {
                return false;
            }
            if (nextElement != null ? !nextElement.equals(comparableIterator.nextElement) : comparableIterator.nextElement != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = ordinal;
            result = 31 * result + (nextElement != null ? nextElement.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(ComparableIterator that)
        {
            int result = comparator.compare(this.nextElement.getKey(), that.nextElement.getKey());
            if (result == 0) {
                result = Integer.compare(this.ordinal, that.ordinal);
            }
            return result;
        }
    }
}

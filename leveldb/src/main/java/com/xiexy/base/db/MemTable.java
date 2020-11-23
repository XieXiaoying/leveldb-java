/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiexy.base.db;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.xiexy.base.impl.*;
import com.xiexy.base.include.Slice;
import com.xiexy.base.utils.InternalIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.xiexy.base.utils.DataUnit.LONG_UNIT;
import static java.util.Objects.requireNonNull;

/**
 * ConcurrentSkipListMap是MemTable的核心数据结构，memtable的KV数据都存储在ConcurrentSkipListMap中
 */
public class MemTable
        implements SeekingIterable<InternalKey, Slice>
{
    private final ConcurrentSkipListMap<InternalKey, Slice> table;
    private final AtomicLong approximateMemoryUsage = new AtomicLong();

    public MemTable(InternalKeyComparator internalKeyComparator)
    {
        table = new ConcurrentSkipListMap<>(internalKeyComparator);
    }

    public boolean isEmpty()
    {
        return table.isEmpty();
    }

    public long approximateMemoryUsage()
    {
        return approximateMemoryUsage.get();
    }

    public void add(long sequenceNumber, ValueType valueType, Slice key, Slice value)
    {
        requireNonNull(valueType, "valueType is null");
        requireNonNull(key, "key is null");

        InternalKey internalKey = new InternalKey(key, sequenceNumber, valueType);
        table.put(internalKey, value);

        // 将在函数的参数中传递的值添加到先前的值,并返回数据类型为long的新更新值。
        approximateMemoryUsage.addAndGet(key.length() + LONG_UNIT + value.length());
    }

    public LookupResult get(LookupKey key)
    {
        requireNonNull(key, "key is null");

        InternalKey internalKey = key.getInternalKey();
        // 返回与该键至少大于或等于给定键,如果不存在这样的键的键 - 值映射,则返回null相关联。
        Map.Entry<InternalKey, Slice> entry = table.ceilingEntry(internalKey);
        if (entry == null) {
            return null;
        }

        InternalKey entryKey = entry.getKey();
        if (entryKey.getUserKey().equals(key.getUserKey())) {
            if (entryKey.getValueType() == ValueType.DELETION) {
                return LookupResult.deleted(key);
            }
            else {
                return LookupResult.ok(key, entry.getValue());
            }
        }
        return null;
    }

    /**
     * 可以遍历访问table的内部数据，很好的设计思想，这种方式隐藏了table的内部实现。
     * 外部调用者必须保证使用Iterator访问Memtable的时候该Memtable是live的。
     * @return 返回一个迭代器
     */
    @Override
    public MemTableIterator iterator()
    {
        return new MemTableIterator();
    }

    public class MemTableIterator
            implements InternalIterator
    {
        // PeekingIterator是自定义的迭代器，是对顶层迭代器Iterator的封装。
        private PeekingIterator<Map.Entry<InternalKey, Slice>> iterator;

        public MemTableIterator()
        {
            iterator = Iterators.peekingIterator(table.entrySet().iterator());
        }

        @Override
        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        @Override
        public void seekToFirst()
        {
            iterator = Iterators.peekingIterator(table.entrySet().iterator());
        }

        @Override
        public void seek(InternalKey targetKey)
        {
            iterator = Iterators.peekingIterator(table.tailMap(targetKey).entrySet().iterator());
        }

        @Override
        public InternalEntry peek()
        {
            Map.Entry<InternalKey, Slice> entry = iterator.peek();
            return new InternalEntry(entry.getKey(), entry.getValue());
        }

        @Override
        public InternalEntry next()
        {
            Map.Entry<InternalKey, Slice> entry = iterator.next();
            return new InternalEntry(entry.getKey(), entry.getValue());
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}

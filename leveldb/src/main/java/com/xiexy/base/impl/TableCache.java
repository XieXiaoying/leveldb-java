package com.xiexy.base.impl;

import com.google.common.cache.*;
import com.xiexy.base.include.Slice;
import com.xiexy.base.table.FileChannelTable;
import com.xiexy.base.table.MMapTable;
import com.xiexy.base.table.Table;
import com.xiexy.base.table.UserComparator;
import com.xiexy.base.utils.Closeables;
import com.xiexy.base.utils.Finalizer;
import com.xiexy.base.utils.InternalTableIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;

/**
 * TableCache缓存的是Table对象，每个DB一个
 * 它内部使用一个LRUCache缓存所有的table对象，实际上其内容是文件编号{file number, TableAndFile}。
 *
 */
public class TableCache
{
    private final LoadingCache<Long, TableAndFile> cache;
    private final Finalizer<Table> finalizer = new Finalizer<>(1);

    public TableCache(final File databaseDir, int tableCacheSize, final UserComparator userComparator, final boolean verifyChecksums)
    {
        requireNonNull(databaseDir, "databaseName is null");

        cache = CacheBuilder.newBuilder()
                .maximumSize(tableCacheSize)
                .removalListener(new RemovalListener<Long, TableAndFile>()
                {
                    @Override
                    public void onRemoval(RemovalNotification<Long, TableAndFile> notification)
                    {
                        Table table = notification.getValue().getTable();
                        finalizer.addCleanup(table, table.closer());
                    }
                })
                .build(new CacheLoader<Long, TableAndFile>()
                {
                    @Override
                    public TableAndFile load(Long fileNumber)
                            throws IOException
                    {
                        return new TableAndFile(databaseDir, fileNumber, userComparator, verifyChecksums);
                    }
                });
    }

    public InternalTableIterator newIterator(FileMetaData file)
    {
        return newIterator(file.getNumber());
    }

    public InternalTableIterator newIterator(long number)
    {
        return new InternalTableIterator(getTable(number).iterator());
    }

    public long getApproximateOffsetOf(FileMetaData file, Slice key)
    {
        return getTable(file.getNumber()).getApproximateOffsetOf(key);
    }

    private Table getTable(long number)
    {
        Table table;
        try {
            table = cache.get(number).getTable();
        }
        catch (ExecutionException e) {
            Throwable cause = e;
            if (e.getCause() != null) {
                cause = e.getCause();
            }
            throw new RuntimeException("Could not open table " + number, cause);
        }
        return table;
    }

    public void close()
    {
        cache.invalidateAll();
        finalizer.destroy();
    }

    public void evict(long number)
    {
        cache.invalidate(number);
    }

    private static final class TableAndFile
    {
        private final Table table;

        private TableAndFile(File databaseDir, long fileNumber, UserComparator userComparator, boolean verifyChecksums)
                throws IOException
        {
            String tableFileName = Filename.tableFileName(fileNumber);
            File tableFile = new File(databaseDir, tableFileName);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(tableFile);
                FileChannel fileChannel = fis.getChannel();
                if (LevelDBFactory.USE_MMAP) {
                    table = new MMapTable(tableFile.getAbsolutePath(), fileChannel, userComparator, verifyChecksums);
                    // We can close the channel and input stream as the mapping does not need them
                    Closeables.closeQuietly(fis);
                }
                else {
                    table = new FileChannelTable(tableFile.getAbsolutePath(), fileChannel, userComparator, verifyChecksums);
                }
            }
            catch (IOException ioe) {
                Closeables.closeQuietly(fis);
                throw ioe;
            }
        }

        public Table getTable()
        {
            return table;
        }
    }
}

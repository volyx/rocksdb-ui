package io.github.volyx;

import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


public class RocksDbWrapper {
	private static final Logger logger = LoggerFactory.getLogger(RocksDbWrapper.class);
	private final Map<String, RocksDB> openDBs = new ConcurrentHashMap<>();

	public RocksDB openDatabase(String dbPath, Vector<Vector<Object>> rows) {
		synchronized (openDBs) {
			if (openDBs.containsKey(dbPath)) {
				return openDBs.get(dbPath);
			} else {
				Options opts = new Options();
				opts.setCreateIfMissing(true);

				RocksDB rdb;
				try {
					rdb = RocksDB.open(opts, dbPath);
					openDBs.put(dbPath, rdb);

					final ColumnFamilyHandle columnFamily = rdb.createColumnFamily(new ColumnFamilyDescriptor("test".getBytes()));

					for (int i = 0; i < 10; i++) {
						rdb.put(columnFamily, ("a" + i).getBytes(StandardCharsets.UTF_8), "b".getBytes(StandardCharsets.UTF_8));

					}
					try (RocksIterator rocksIterator = rdb.newIterator(columnFamily)) {
						rocksIterator.seekToFirst();
						while (rocksIterator.isValid()) {
							final Vector<Object> v = new Vector<>();
							v.add(new String(rocksIterator.key(), StandardCharsets.UTF_8));
							v.add(new String(rocksIterator.value(), StandardCharsets.UTF_8));
							rows.add(v);
							rocksIterator.next();
						}
					}

					return rdb;
				} catch (RocksDBException e) {
					String msg = "Failed to open database: " + e.getMessage();
					logger.error(msg);
				}
				// If user doesn't call options dispose explicitly, then
				// this options instance will be GC'd automatically.
				// opts.close();
			}
		}

		return null;
	}
}

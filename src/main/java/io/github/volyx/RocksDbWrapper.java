package io.github.volyx;

import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;


public class RocksDbWrapper {
	private static final Logger logger = LoggerFactory.getLogger(RocksDbWrapper.class);
	private final boolean insertTestData = Boolean.getBoolean("insert.data");

	{
		if (insertTestData) {
			final String folder = System.getProperty("insert.folder");
			Objects.requireNonNull(folder);
			System.out.println("insert test data " + Paths.get(folder).toAbsolutePath());
			if (Paths.get(folder).toFile().list().length == 0) {
				try (Options opts = new Options().setCreateIfMissing(true); RocksDB rdb = RocksDB.open(opts, folder)) {
					//					openDBs.put(Paths.get(folder), rdb);

					for (String column : Arrays.asList("a", "b", "c")) {
						try (ColumnFamilyHandle columnFamily = rdb.createColumnFamily(new ColumnFamilyDescriptor(column.getBytes()));) {
							for (int i = 0; i < 10; i++) {
								final String key = column + i;
								rdb.put(columnFamily, key.getBytes(StandardCharsets.UTF_8), ("__" + key).getBytes(StandardCharsets.UTF_8));
							}
						}
					}

				} catch (RocksDBException e) {
					String msg = "Failed to open database: " + e.getMessage();
					logger.error(msg);
					throw new RuntimeException(e);
				}
			} else {
				System.err.println("folder " + folder + " not empty");
			}

		}
	}

	public Map<String, String> openDatabase(String dbPath, String columnFamily) {
		Map<String, String> keyValues = new HashMap<>();
		try (final ColumnFamilyOptions cfOpts = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {

			final List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();
			int index = 0;
			int i = 0;
			for (String c : listColumnFamilies(dbPath)) {
				cfDescriptors.add(new ColumnFamilyDescriptor(c.getBytes(), cfOpts));
				if (c.equals(columnFamily)) {
					index = i;
				}
				i++;
			}

			// a list which will hold the handles for the column families once the db is opened
			final List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<>();

			try (DBOptions options = new DBOptions().setCreateIfMissing(false).setCreateMissingColumnFamilies(false);
				 RocksDB rdb = RocksDB.open(options, dbPath, cfDescriptors, columnFamilyHandleList)) {

				try (RocksIterator rocksIterator = rdb.newIterator(columnFamilyHandleList.get(index))) {
					rocksIterator.seekToFirst();
					while (rocksIterator.isValid()) {
						keyValues.put(
								new String(rocksIterator.key(), StandardCharsets.UTF_8),
								new String(rocksIterator.value(), StandardCharsets.UTF_8)
						);
						rocksIterator.next();
					}

				} finally {
					// NOTE frees the column family handles before freeing the db
					for (final ColumnFamilyHandle columnFamilyHandle :
							columnFamilyHandleList) {
						columnFamilyHandle.close();
					}
				}

				return keyValues;
			} catch (RocksDBException e) {
				String msg = "Failed to open database: " + e.getMessage();
				logger.error(msg);
				throw new RuntimeException(e);
			}
		}

	}

	public List<String> listColumnFamilies(String dbPath) {
		List<String> columnFamilies = new ArrayList<>();


		try (Options opts = new Options().setCreateIfMissing(false)) {
			for (byte[] c : RocksDB.listColumnFamilies(opts, dbPath)) {
				columnFamilies.add(new String(c, StandardCharsets.UTF_8));
			}


		} catch (RocksDBException e) {
			String msg = "Failed to open database: " + e.getMessage();
			logger.error(msg);
			throw new RuntimeException(e);
		}
		return columnFamilies;
	}

}

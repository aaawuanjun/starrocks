// This file is licensed under the Elastic License 2.0. Copyright 2021-present, StarRocks Inc.

package com.starrocks.connector.hive;

import com.starrocks.connector.hive.HiveMetaClient;
import com.starrocks.connector.hive.HiveMetaStoreThriftClient;
import com.starrocks.connector.hive.HiveMetastoreApiConverter;
import com.starrocks.connector.hive.TextFileFormatDesc;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaHookLoader;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.RetryingMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HiveMetaClientTest {
    @Test
    public void testClientPool(@Mocked HiveMetaStoreThriftClient metaStoreClient) throws Exception {
        new Expectations() {
            {
                metaStoreClient.getTable(anyString, anyString);
                result = new Table();
                minTimes = 0;
            }
        };

        final int[] clientNum = {0};

        new MockUp<RetryingMetaStoreClient>() {
            @Mock
            public IMetaStoreClient getProxy(Configuration hiveConf, HiveMetaHookLoader hookLoader,
                                             ConcurrentHashMap<String, Long> metaCallTimeMap, String mscClassName,
                                             boolean allowEmbedded) throws MetaException {
                clientNum[0]++;
                return metaStoreClient;
            }
        };

        HiveConf hiveConf = new HiveConf();
        hiveConf.set(MetastoreConf.ConfVars.THRIFT_URIS.getHiveName(), "thrift://127.0.0.1:9030");
        HiveMetaClient client = new HiveMetaClient(hiveConf);
        // NOTE: this is HiveMetaClient.MAX_HMS_CONNECTION_POOL_SIZE
        int poolSize = 32;

        // call client method concurrently,
        // and make sure the number of hive clients will not exceed poolSize
        for (int i = 0; i < 10; i++) {
            ExecutorService es = Executors.newCachedThreadPool();
            for (int j = 0; j < poolSize; j++) {
                es.execute(() -> {
                    try {
                        client.getTable("db", "tbl");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assert.fail(e.getMessage());
                    }
                });
            }
            es.shutdown();
            es.awaitTermination(1, TimeUnit.HOURS);
        }

        System.out.println("called times is " + clientNum[0]);

        Assert.assertTrue(clientNum[0] >= 1 && clientNum[0] <= poolSize);
    }

    @Test
    public void testGetTextFileFormatDesc() {
        // Check is using default delimiter
        TextFileFormatDesc emptyDesc = HiveMetastoreApiConverter.toTextFileFormatDesc(new HashMap<>());
        Assert.assertEquals("\001", emptyDesc.getFieldDelim());
        Assert.assertEquals("\n", emptyDesc.getLineDelim());
        Assert.assertEquals("\002", emptyDesc.getCollectionDelim());
        Assert.assertEquals("\003", emptyDesc.getMapkeyDelim());

        // Check is using custom delimiter
        StorageDescriptor customSd = new StorageDescriptor();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("field.delim", ",");
        parameters.put("line.delim", "\004");
        parameters.put("collection.delim", "\006");
        parameters.put("mapkey.delim", ":");
        TextFileFormatDesc customDesc = HiveMetastoreApiConverter.toTextFileFormatDesc(parameters);
        Assert.assertEquals(",", customDesc.getFieldDelim());
        Assert.assertEquals("\004", customDesc.getLineDelim());
        Assert.assertEquals("\006", customDesc.getCollectionDelim());
        Assert.assertEquals(":", customDesc.getMapkeyDelim());
    }
}


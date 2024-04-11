package com.demo.flink.streaming;

import codelikethewind.CallRecord;
import codelikethewind.CallRecordMarshallingContext;
import codelikethewind.CallRecordSchemaImpl;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class DataGridSink extends RichAsyncFunction<String, Void> {

    private ConfigurationBuilder builder;
    private RemoteCacheManager cacheManager;
    private RemoteCache<Long, CallRecord> cache;

    private String cacheName;

    int count = 0;

    public DataGridSink(String cacheName){
        this.cacheName = cacheName;
    }

    @Override
    public void open(Configuration parameters) throws Exception {

        builder = new ConfigurationBuilder();
        builder.addServer()
                .host("datagrid")
                .port(ConfigurationProperties.DEFAULT_HOTROD_PORT)
                .clientIntelligence(ClientIntelligence.BASIC)
                .create();

        if (cacheManager == null){
            URI callRecordCacheConfig;
            try {
                callRecordCacheConfig = getClass().getClassLoader().getResource("callRecordCacheConfig.xml").toURI();
            } catch (URISyntaxException ex) {
                System.out.println(ex);
                throw new RuntimeException(ex);
            }

            builder.remoteCache("call_records").configurationURI(callRecordCacheConfig);
            builder.addContextInitializer(new CallRecordSchemaImpl());

            cacheManager = new RemoteCacheManager(builder.build());
            CallRecordMarshallingContext.initSerializationContext(cacheManager);
            cache = cacheManager.getCache(cacheName);

        }

    }

    @Override
    public void close() throws Exception {
        cacheManager.close();
        cacheManager = null;
        cache = null;
    }

    @Override
    public void asyncInvoke(String input, ResultFuture<Void> resultFuture) throws Exception {

        String[] call = input.split(",");

        CallRecord e = new CallRecord();

        e.id = count;
        e.location = call[0];
        e.signalStrength = call[1];
        e.network = call[2];
        e.timestamp = new Date();

        CompletableFuture putResult =  cache.putAsync(Long.valueOf(e.id), e);

        putResult.whenComplete((result, throwable) -> {if(throwable!=null){
            System.out.println("Callback from datagrid failed:" + throwable);
            resultFuture.complete(new ArrayList<>());
        }
        else{
            System.out.println("Persisting to cache");
            resultFuture.complete(new ArrayList(Collections.singleton(result)));
        }});

        count++;
    }
}

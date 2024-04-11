package codelikethewind;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.util.Objects;

public class CallRecordMarshallingContext {

    public static void initSerializationContext(RemoteCacheManager cacheManager) {

        System.out.println("---- CallRecordMarshallingContext - initialize the serialization context for CallRecord class ----");
        Objects.requireNonNull(cacheManager);

        // Retrieve metadata cache
        RemoteCache<String, String> metadataCache =
                cacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

        GeneratedSchema schema = new CallRecordSchemaImpl();
        // Define the new schema on the server too
        metadataCache.put(schema.getProtoFileName(), schema.getProtoFile());
    }
}

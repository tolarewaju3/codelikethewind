package codelikethewind;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
        includeClasses = {
                CallRecord.class
        },
        schemaFileName = "callrecord.proto",
        schemaFilePath = "proto/",
        schemaPackageName = "org.codelikethewind")
public interface CallRecordSchema extends GeneratedSchema {
}

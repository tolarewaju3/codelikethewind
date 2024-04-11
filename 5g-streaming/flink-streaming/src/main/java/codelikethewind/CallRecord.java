package codelikethewind;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;

@Indexed
public class CallRecord {

    @Basic
    @ProtoField(number = 1, defaultValue = "1")
    public
    int id;

    @Basic
    @ProtoField(number = 2)
    public
    String location;

    @Basic
    @ProtoField(number = 3)
    public
    Date timestamp;

    @Basic
    @ProtoField(number = 4)
    public
    String signalStrength;

    @Basic
    @ProtoField(number = 5)
    public
    String network;

}

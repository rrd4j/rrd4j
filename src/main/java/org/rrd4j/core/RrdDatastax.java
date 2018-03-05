package org.rrd4j.core;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.nio.ByteBuffer;

@Table(keyspace = "rrd4j", name = "rrd",
       readConsistency = "QUORUM",
       writeConsistency = "QUORUM",
       caseSensitiveKeyspace = false,
       caseSensitiveTable = false)
public class RrdDatastax {
    @PartitionKey
    @Column(name = "path")
    private String path;

    private ByteBuffer rrd;


    public String getPath() {
        return path;
    }

    public RrdDatastax setPath(String path) {
        this.path = path;
        return this;
    }

    public ByteBuffer getRrd() {
        return rrd;
    }

    public RrdDatastax setRrd(ByteBuffer rrd) {
        this.rrd = rrd;
        return this;
    }
}

package org.rrd4j.core;

import java.io.IOException;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDbTest {

    @AfterClass
    public static void restore() throws InterruptedException {
        RrdBackendFactory.setActiveFactories(RrdBackendFactory.getFactory(RrdBackendFactory.DEFAULTFACTORY));
    }

    @Test
    public void testLifeCycle() throws IOException, InterruptedException {
        try (MongoClient mongoClient = new MongoClient(Arrays.asList(new ServerAddress("localhost")),
                new MongoClientOptions.Builder()
                .serverSelectionTimeout(2000)
                .minConnectionsPerHost(0)
                .build())) {
            MongoDatabase mongodb = mongoClient.getDatabase("mydb");
            MongoCollection<DBObject> collection = mongodb.getCollection("test", DBObject.class);
            RrdBackendFactory factory = new RrdMongoDBBackendFactory(mongoClient, collection, false);
            RrdBackendFactory.setActiveFactories(factory);
            RrdDef def = new RrdDef(factory.getUri("therrd"));
            def.setStep(2);
            def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
            def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
            try (RrdDb db = new RrdDb(def)) {
                Assert.assertEquals(
                        "mongodb://localhost:27017/mydb/test/therrd",
                        db.getUri().toString());
                db.createSample().setAndUpdate("NOW:1");
            };
            try (RrdDb db = new RrdDb("mongodb://localhost:27017/mydb/test/therrd")) {
                Assert.assertEquals(
                        "mongodb://localhost:27017/mydb/test/therrd",
                        db.getUri().toString());
            };
        }
    }

}

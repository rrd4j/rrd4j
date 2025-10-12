package org.rrd4j.core;

import java.io.IOException;
import java.util.Arrays;

import org.bson.UuidRepresentation;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

import com.mongodb.DBObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class RrdDbNewMongoDbTest {

    private static RrdBackendFactory previousBackend;

    @BeforeClass
    public static void setBackendBefore() {
        previousBackend = RrdBackendFactory.getDefaultFactory();
    }

    @AfterClass
    public static void setBackendAfter() {
        RrdBackendFactory.setActiveFactories(previousBackend);
    }

    @Test//(timeout = 5000)
    public void testLifeCycle() throws IOException {
        MongoClientSettings settings = MongoClientSettings.builder()
                                               .uuidRepresentation( UuidRepresentation.JAVA_LEGACY )
                                               .applyToClusterSettings( builder -> builder.hosts( Arrays.asList( new ServerAddress( "localhost", 27017 ) ) ) )
                                               .build();

        ;

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            MongoDatabase database = mongoClient.getDatabase( "mydb" );
            MongoCollection<DBObject> collection = database.getCollection( "test", DBObject.class );
            RrdBackendFactory factory = new RrdMongoDBNewBackendFactory(mongoClient, collection);
            RrdBackendFactory.setActiveFactories(factory);
            RrdDef def = new RrdDef(factory.getUri("therrd"));
            def.setStep(2);
            def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
            def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
            try (RrdDb db = RrdDb.getBuilder().setRrdDef(def).build()) {
                Assert.assertEquals(
                        "mongodb://localhost:27017/mydb/test/therrd",
                        db.getUri().toString());
                db.createSample().setAndUpdate("NOW:1");
            }
            try (RrdDb db = RrdDb.getBuilder().setPath("mongodb://localhost:27017/mydb/test/therrd").build()) {
                Assert.assertEquals(
                        "mongodb://localhost:27017/mydb/test/therrd",
                        db.getUri().toString());
                Assert.assertNotNull(db.getBytes());
            }
        }
    }

}

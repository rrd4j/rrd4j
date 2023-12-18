package org.rrd4j.core;

import static org.easymock.EasyMock.partialMockBuilder;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

public class RrdBackendTest {
    
    @Test
    public void testBackendClose() throws IOException {
        // given
        String fileName = "variabletest.rrd";
        
        RrdBackend rrdBackendMock = partialMockBuilder(RrdMemoryBackend.class)
                .withConstructor(fileName, new AtomicReference<ByteBuffer>())
                .addMockedMethod("close")
                .createMock();
        RrdBackendFactory backendFactory = new RrdMemoryBackendFactory() {
            @Override
            protected RrdBackend open(String id, boolean readOnly) {
                return rrdBackendMock;
            }
            @Override
            public String getName() {
                return "memory";        // to prevent NPE upon factory construction
            }
        };
        RrdDef def = createRrdDef(backendFactory, fileName);

        rrdBackendMock.close();                  // record the interaction we want to verify 
        
        replay(rrdBackendMock);         // switch EasyMock from recording to actual execution

        // when
        try (RrdDb db = RrdDb.getBuilder()
                .setRrdDef(def)
                .setBackendFactory(backendFactory)
                .build()) {
            // a simple assertion just not to leave try block empty
            Assert.assertEquals("Invalid step", 300L, db.getHeader().getStep());
        }       // this line closes the database
        
        // then
        verify(rrdBackendMock);     // checks that `close()` method (1) was called and (2) called exactly once
    }

    private static RrdDef createRrdDef(RrdBackendFactory backendFactory, String fileName) {
        long start = Util.getTimestamp(2023, 12, 6);
        RrdDef def = new RrdDef(backendFactory.getUri(fileName), start, 300);
        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
        return def;
    }

}
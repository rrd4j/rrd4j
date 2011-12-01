package org.rrd4j.core;

import org.junit.Test;

/**
 * @author Mathias Bogaert
 */
public class RrdDefTest {
    @Test(expected = IllegalArgumentException.class)
    public void testRrdToolsDefEmpty() {
        RrdDef def = new RrdDef("test");
        def.addDatasource("");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRrdToolsDefNull() {
        RrdDef def = new RrdDef("test");
        String s = null;
        def.addDatasource(s);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRrdToolsDefInvalid() {
        RrdDef def = new RrdDef("test");
        def.addDatasource(":");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRrdToolsDefInvalid1() {
        RrdDef def = new RrdDef("test");
        def.addDatasource("::::");
    }
}

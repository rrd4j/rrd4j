package org.rrd4j.graph;

import org.junit.Assert;
import org.junit.Test;

public class CommentTextTest {
    @Test
    public void testNormalText() {
        CommentText text = new CommentText("test test");
        text.resolveText(null, null, null);

        Assert.assertEquals(text.resolvedText, "test test");
    }

    @Test
    public void testGluedText() {
        CommentText text = new CommentText("test \\g");
        text.resolveText(null, null, null);

        Assert.assertEquals(text.resolvedText, "test");
    }
}

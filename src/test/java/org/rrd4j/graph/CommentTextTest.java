package org.rrd4j.graph;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CommentTextTest {
    @Test
    public void testNormalText() throws IOException {
        CommentText text = new CommentText("test test");
        text.resolveText(null, null, null);

        Assert.assertEquals(text.resolvedText, "test test");
    }

    @Test
    public void testGluedText() throws IOException {
        CommentText text = new CommentText("test \\g");
        text.resolveText(null, null, null);

        Assert.assertEquals(text.resolvedText, "test");
    }
}

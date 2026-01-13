package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.runtime.Story;
import org.junit.Assert;
import org.junit.Test;

public class TunnelSpecTest {

    /**
     * "- Test for tunnel onwards divert override"
     */
    @Test
    public void testTunnelOnwardsDivertOverride() throws Exception {

        Compiler compiler = new Compiler();

        String json =
                compiler.compile(TestUtils.readFileAsString("inkfiles/tunnels/tunnel-onwards-divert-override.ink"));
        Story story = new Story(json);

        Assert.assertEquals("This is A\nNow in B.\n", story.continueMaximally());
    }
}

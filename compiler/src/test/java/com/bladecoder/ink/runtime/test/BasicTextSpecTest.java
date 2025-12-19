package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.runtime.Story;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class BasicTextSpecTest {

    /**
     *      The more simple ink file, one line of text.
     */
    @Test
    public void oneline() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.getJsonString("inkfiles/basictext/oneline.ink"));
        System.out.println("JSON: " + json);
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("Line.", text.get(0));
    }

    /**
     *      Two lines of text.
     */
    @Test
    public void twolines() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.getJsonString("inkfiles/basictext/twolines.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(2, text.size());
        Assert.assertEquals("Line.", text.get(0));
        Assert.assertEquals("Other line.", text.get(1));
    }
}

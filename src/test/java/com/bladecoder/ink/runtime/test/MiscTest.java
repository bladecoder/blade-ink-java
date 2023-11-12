package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.runtime.Story;
import org.junit.Assert;
import org.junit.Test;

public class MiscTest {

    /**
     * Issue: https://github.com/bladecoder/blade-ink/issues/15
     */
    @Test
    public void issue15() throws Exception {
        String json = TestUtils.getJsonString("inkfiles/misc/issue15.ink.json");
        Story story = new Story(json);

        Assert.assertEquals("This is a test\n", story.Continue());

        while (story.canContinue()) {
            //			System.out.println(story.buildStringOfHierarchy());
            String line = story.Continue();

            if (line.startsWith("SET_X:")) {
                story.getVariablesState().set("x", 100);
            } else {
                Assert.assertEquals("X is set\n", line);
            }
        }
    }

    @Test
    public void testNewlinesWithStringEval() throws Exception {

        String json = TestUtils.getJsonString("inkfiles/misc/newlines_with_string_eval.ink.json");
        Story story = new Story(json);

        Assert.assertEquals("A\n3\nB\n", story.continueMaximally());
    }
}

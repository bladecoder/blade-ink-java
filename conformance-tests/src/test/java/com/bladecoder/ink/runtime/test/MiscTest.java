package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.runtime.Story;
import org.junit.Assert;
import org.junit.Test;

public class MiscTest {

    /**
     * Issue: https://github.com/bladecoder/blade-ink/issues/15
     */
    @Test
    public void issue15() throws Exception {
        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/misc/issue15.ink"));
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
    public void escapeHashCompiles() throws Exception {
        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/misc/escape-hash.ink"));
        Story story = new Story(json);

        Assert.assertEquals("Bug with escape character #\n", story.Continue());
    }
}

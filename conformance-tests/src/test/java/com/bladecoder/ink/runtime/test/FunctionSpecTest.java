package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.runtime.Story;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class FunctionSpecTest {

    /**
     * "- return a value from a function in a variable expression"
     */
    @Test
    public void funcBasic() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/function/func-basic.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value of x is 4.4.", text.get(0));
    }

    /**
     * "- return a value from a function with no parameters"
     */
    @Test
    public void funcNone() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/function/func-none.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value of x is 3.8.", text.get(0));
    }

    /**
     * "- handle conditionals in the function"
     */
    @Test
    public void funcInline() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/function/func-inline.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value of x is 4.4.", text.get(0));
    }

    /**
     * "- be able to set a variable as a command"
     */
    @Test
    public void setVarFunc() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/function/setvar-func.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is 6.", text.get(0));
    }

    /**
     * "- handle conditionals and setting of variables (test 1)"
     */
    @Test
    public void complexFunc1() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/function/complex-func1.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The values are 6 and 10.", text.get(0));
    }

    /**
     * "- handle conditionals and setting of variables (test 2)"
     */
    @Test
    public void complexFunc2() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/function/complex-func2.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The values are -1 and 0 and 1.", text.get(0));
    }

    /**
     * "- handle conditionals and setting of variables (test 3)"
     */
    @Test
    public void complexFunc3() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/function/complex-func3.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(1, text.size());
        Assert.assertEquals(
                "\"I will pay you 120 reales if you get the goods to their destination. The goods will take up 20 cargo spaces.\"",
                text.get(0));
    }

    /**
     * "- random function"
     */
    @Test
    public void rnd() throws Exception {
        List<String> text = new ArrayList<String>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/function/rnd-func.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(4, text.size());
        Assert.assertEquals("Rolling dice 1: 4.", text.get(0));
        Assert.assertEquals("Rolling dice 2: 1.", text.get(1));
        Assert.assertEquals("Rolling dice 3: 1.", text.get(2));
        Assert.assertEquals("Rolling dice 4: 5.", text.get(3));
    }

    /**
     * "- TestEvaluatingFunctionVariableStateBug"
     */
    @Test
    public void evaluatingFunctionVariableStateBug() throws Exception {

        Compiler compiler = new Compiler();

        String json = compiler.compile(
                TestUtils.readFileAsString("inkfiles/function/evaluating-function-variablestate-bug.ink"));
        Story story = new Story(json);

        Assert.assertEquals("Start\n", story.Continue());
        Assert.assertEquals("In tunnel.\n", story.Continue());

        Object funcResult = story.evaluateFunction("function_to_evaluate");
        Assert.assertEquals("RIGHT", (String) funcResult);

        Assert.assertEquals("End\n", story.Continue());
    }
}

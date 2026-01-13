package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.runtime.Profiler;
import com.bladecoder.ink.runtime.Story;
import com.bladecoder.ink.runtime.Story.*;
import com.bladecoder.ink.runtime.StoryException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class RuntimeSpecTest {

    /**
     * Test external function call.
     */
    @Test
    public void externalFunction() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/external-function-2-arg.ink"));
        final Story story = new Story(json);

        story.bindExternalFunction("externalFunction", new ExternalFunction<Integer>() {

            @Override
            public Integer call(Object[] args) throws Exception {
                int x = story.tryCoerce(args[0], Integer.class);
                int y = story.tryCoerce(args[1], Integer.class);
                return x - y;
            }
        });

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is -1.", text.get(0));
    }

    /**
     * Test external function zero arguments call.
     */
    @Test
    public void externalFunctionZeroArguments() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/external-function-0-arg.ink"));
        final Story story = new Story(json);

        story.bindExternalFunction("externalFunction", new ExternalFunction0<String>() {

            @Override
            protected String call() {
                return "Hello world";
            }
        });

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is Hello world.", text.get(0));
    }

    /**
     * Test external function one argument call.
     */
    @Test
    public void externalFunctionOneArgument() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/external-function-1-arg.ink"));
        final Story story = new Story(json);

        story.bindExternalFunction("externalFunction", new ExternalFunction1<Integer, Boolean>() {

            @Override
            protected Boolean call(Integer arg) {
                return arg != 1;
            }
        });

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is false.", text.get(0));
    }

    /**
     * Test external function one argument call. Overrides coerce method.
     */
    @Test
    public void externalFunctionOneArgumentCoerceOverride() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/external-function-1-arg.ink"));
        final Story story = new Story(json);

        story.bindExternalFunction("externalFunction", new ExternalFunction1<Boolean, Boolean>() {

            @Override
            protected Boolean coerceArg(Object arg) throws Exception {
                return story.tryCoerce(arg, Boolean.class);
            }

            @Override
            protected Boolean call(Boolean arg) {
                return !arg;
            }
        });

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is false.", text.get(0));
    }

    /**
     * Test external function two arguments call.
     */
    @Test
    public void externalFunctionTwoArguments() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/external-function-2-arg.ink"));
        final Story story = new Story(json);

        story.bindExternalFunction("externalFunction", new ExternalFunction2<Integer, Float, Integer>() {

            @Override
            protected Integer call(Integer x, Float y) {
                return (int) (x - y);
            }
        });

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is -1.", text.get(0));
    }

    /**
     * Test external function two arguments call. Overrides coerce methods.
     */
    @Test
    public void externalFunctionTwoArgumentsCoerceOverride() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/external-function-2-arg.ink"));
        final Story story = new Story(json);

        story.bindExternalFunction("externalFunction", new ExternalFunction2<Integer, Integer, Integer>() {

            @Override
            protected Integer coerceArg0(Object arg) throws Exception {
                return story.tryCoerce(arg, Integer.class);
            }

            @Override
            protected Integer coerceArg1(Object arg) throws Exception {
                return story.tryCoerce(arg, Integer.class);
            }

            @Override
            protected Integer call(Integer x, Integer y) {
                return x - y;
            }
        });

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is -1.", text.get(0));
    }

    /**
     * Test external function three arguments call.
     */
    @Test
    public void externalFunctionThreeArguments() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/external-function-3-arg.ink"));
        final Story story = new Story(json);

        story.bindExternalFunction("externalFunction", new ExternalFunction3<Integer, Integer, Integer, Integer>() {

            @Override
            protected Integer call(Integer x, Integer y, Integer z) {
                return x + y + z;
            }
        });

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is 6.", text.get(0));
    }

    /**
     * Test external function three arguments call. Overrides coerce methods.
     */
    @Test
    public void externalFunctionThreeArgumentsCoerceOverride() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/external-function-3-arg.ink"));
        final Story story = new Story(json);

        story.bindExternalFunction("externalFunction", new ExternalFunction3<Integer, Integer, Integer, Integer>() {

            @Override
            protected Integer coerceArg0(Object arg) throws Exception {
                return story.tryCoerce(arg, Integer.class);
            }

            @Override
            protected Integer coerceArg1(Object arg) throws Exception {
                return story.tryCoerce(arg, Integer.class);
            }

            @Override
            protected Integer coerceArg2(Object arg) throws Exception {
                return story.tryCoerce(arg, Integer.class);
            }

            @Override
            protected Integer call(Integer x, Integer y, Integer z) {
                return x + y + z;
            }
        });

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is 6.", text.get(0));
    }

    /**
     * Test external function fallback.
     */
    @Test
    public void externalFunctionFallback() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/external-function-2-arg.ink"));
        Story story = new Story(json);

        story.setAllowExternalFunctionFallbacks(true);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The value is 7.0.", text.get(0));
    }

    private static int variableObserversExpectedValue = 5;

    /**
     * Test variable observers.
     */
    @Test
    public void variableObservers() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/variable-observers.ink"));
        Story story = new Story(json);

        story.observeVariable("x", new VariableObserver() {

            @Override
            public void call(String variableName, Object newValue) {
                if (!"x".equals(variableName)) Assert.fail();
                try {
                    if ((int) newValue != variableObserversExpectedValue) Assert.fail();

                    variableObserversExpectedValue = 10;
                } catch (Exception e) {
                    Assert.fail();
                }
            }
        });

        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);
        TestUtils.nextAll(story, text);

        Assert.assertEquals(10, variableObserversExpectedValue);
    }

    /**
     * Test set/get variables from code.
     */
    @Test
    public void setAndGetVariable() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/set-get-variables.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(10, (int) story.getVariablesState().get("x"));

        story.getVariablesState().set("x", 15);

        Assert.assertEquals(15, (int) story.getVariablesState().get("x"));

        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals("OK", text.get(0));
    }

    /**
     * Test non existant variable.
     */
    @Test
    public void testSetNonExistantVariable() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/set-get-variables.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        try {
            story.getVariablesState().set("y", "earth");
            Assert.fail("Setting non existant variable.");
        } catch (StoryException e) {

        }

        Assert.assertEquals(10, (int) story.getVariablesState().get("x"));

        story.getVariablesState().set("x", 15);

        Assert.assertEquals(15, (int) story.getVariablesState().get("x"));

        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals("OK", text.get(0));
    }

    /**
     * Jump to knot from code.
     */
    @Test
    public void jumpKnot() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/jump-knot.ink"));
        Story story = new Story(json);

        story.choosePathString("two");
        TestUtils.nextAll(story, text);
        Assert.assertEquals("Two", text.get(0));

        text.clear();
        story.choosePathString("three");
        TestUtils.nextAll(story, text);
        Assert.assertEquals("Three", text.get(0));

        text.clear();
        story.choosePathString("one");
        TestUtils.nextAll(story, text);
        Assert.assertEquals("One", text.get(0));

        text.clear();
        story.choosePathString("two");
        TestUtils.nextAll(story, text);
        Assert.assertEquals("Two", text.get(0));
    }

    /**
     * Test the Profiler.
     */
    @Test
    public void profiler() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/jump-knot.ink"));
        Story story = new Story(json);

        Profiler profiler = story.startProfiling();

        story.choosePathString("two");
        TestUtils.nextAll(story, text);

        story.choosePathString("three");
        TestUtils.nextAll(story, text);

        story.choosePathString("one");
        TestUtils.nextAll(story, text);

        story.choosePathString("two");
        TestUtils.nextAll(story, text);

        String reportStr = profiler.report();

        story.endProfiling();

        System.out.println("PROFILER REPORT: " + reportStr);
    }

    /**
     * Jump to stitch from code.
     */
    @Test
    public void jumpStitch() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/jump-stitch.ink"));
        Story story = new Story(json);

        story.choosePathString("two.sthree");
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("Two.3", text.get(0));

        text.clear();
        story.choosePathString("one.stwo");
        TestUtils.nextAll(story, text);
        Assert.assertEquals("One.2", text.get(0));

        text.clear();
        story.choosePathString("one.sone");
        TestUtils.nextAll(story, text);
        Assert.assertEquals("One.1", text.get(0));

        text.clear();
        story.choosePathString("two.stwo");
        TestUtils.nextAll(story, text);
        Assert.assertEquals("Two.2", text.get(0));
    }

    /**
     * Read the visit counts from code. The .ink file must be compiled with the '-c'
     * flag in inklecate.
     */
    @Test
    public void readVisitCounts() throws Exception {
        List<String> text = new ArrayList<>();

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/read-visit-counts.ink"));
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(4, story.getState().visitCountAtPathString("two.s2"));
        Assert.assertEquals(5, story.getState().visitCountAtPathString("two"));
    }

    @Test
    public void testLoadSave() throws Exception {
        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/runtime/load-save.ink"));
        Story story = new Story(json);

        List<String> text = new ArrayList<>();

        TestUtils.nextAll(story, text);

        Assert.assertEquals(1, text.size());
        Assert.assertEquals("We arrived into London at 9.45pm exactly.", text.get(0));

        //		String choicesText = getChoicesText(story);
        //		assertThat(choicesText, is(
        //				"0:\"There is not a moment to lose!\"\n1:\"Monsieur, let us savour this moment!\"\n2:We hurried home\n"));

        // save the game state
        String saveString = story.getState().toJson();

        // recreate game and load state
        story = new Story(json);
        story.getState().loadJson(saveString);

        story.chooseChoiceIndex(0);

        TestUtils.nextAll(story, text);
        Assert.assertEquals("\"There is not a moment to lose!\" I declared.", text.get(1));
        Assert.assertEquals("We hurried home to Savile Row as fast as we could.", text.get(2));

        // check that we are at the end
        Assert.assertEquals(false, story.canContinue());
        Assert.assertEquals(0, story.getCurrentChoices().size());
    }
}

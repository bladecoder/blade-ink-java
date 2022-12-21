package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.runtime.Story;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class VariableTextSpecTest {
    /**
     *      "- step through each element and repeat the final element"
     */
    @Test
    public void sequence() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/variabletext/sequence.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"Three!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"Two!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"One!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals(
                "The radio hissed into life. There was the white noise racket of an explosion.", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals(
                "The radio hissed into life. There was the white noise racket of an explosion.", text.get(0));
    }

    /**
     *       "- cycle through the element repeatedly"
     */
    @Test
    public void cycle() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/variabletext/cycle.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"Three!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"Two!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"One!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"Three!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"Two!\"", text.get(0));
    }

    /**
     *       "- step through each element and return no text once the list is exhausted"
     */
    @Test
    public void once() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/variabletext/once.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"Three!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"Two!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"One!\"", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life.", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life.", text.get(0));
    }

    /**
     *       "- allow for empty text elements in the list"
     */
    @Test
    public void emptyElements() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/variabletext/empty-elements.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life.", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life.", text.get(0));
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals("The radio hissed into life. \"One!\"", text.get(0));
    }

    /**
     *       "- be usable in a choice test"
     */
    @Test
    public void listInChoice() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/variabletext/list-in-choice.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(
                "\"Hello, Master!\"", story.getCurrentChoices().get(0).getText());
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(
                "\"Hello, Monsieur!\"", story.getCurrentChoices().get(0).getText());
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals("\"Hello, you!\"", story.getCurrentChoices().get(0).getText());
    }

    /**
     *      "- return the text string in the sequence if the condition is a valid value"
     *
     *      FIXME: "Value evaluated lists" not supported in C# ref. engine.
     */
    @Test
    public void one() throws Exception {
        //		List<String> text = new ArrayList<String>();
        //
        //		String json = TestUtils.getJsonString("inkfiles/variabletext/one.ink.json");
        //		Story story = new Story(json);
        //
        //		TestUtils.nextAll(story, text);
        //		Assert.assertEquals(1, text.size());
        //		Assert.assertEquals("We needed to find one apple.", text.get(0));
    }

    /**
     *      "- return the text string in the sequence if the condition is a valid value"
     *      FIXME: "Value evaluated lists" not supported in C# ref. engine.
     */
    @Test
    public void minusOne() throws Exception {
        //		List<String> text = new ArrayList<String>();
        //
        //		String json = TestUtils.getJsonString("inkfiles/variabletext/minus-one.ink.json");
        //		Story story = new Story(json);
        //
        //		TestUtils.nextAll(story, text);
        //		Assert.assertEquals(1, text.size());
        //		Assert.assertEquals("We needed to find nothing.", text.get(0));
    }

    /**
     *      "- return the text string in the sequence if the condition is a valid value"
     *      FIXME: "Value evaluated lists" not supported in C# ref. engine.
     */
    @Test
    public void ten() throws Exception {
        //		List<String> text = new ArrayList<String>();
        //
        //		String json = TestUtils.getJsonString("inkfiles/variabletext/ten.ink.json");
        //		Story story = new Story(json);
        //
        //		TestUtils.nextAll(story, text);
        //		Assert.assertEquals(1, text.size());
        //		Assert.assertEquals("We needed to find many oranges.", text.get(0));
    }
}

package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.runtime.Story;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class GatherSpecTest {

    /**
     *      "- gather the flow back together again"
     */
    @Test
    public void gatherBasic() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/gather/gather-basic.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(1);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(3, text.size());
        Assert.assertEquals("\"Nothing, Monsieur!\" I replied.", text.get(0));
        Assert.assertEquals("\"Very good, then.\"", text.get(1));
        Assert.assertEquals("With that Monsieur Fogg left the room.", text.get(2));
    }

    /**
     *      "- form chains of content with multiple gathers"
     */
    @Test
    public void gatherChain() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/gather/gather-chain.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        Assert.assertEquals(3, story.getCurrentChoices().size());
        story.chooseChoiceIndex(1);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(1, text.size());
        Assert.assertEquals(
                "I did not pause for breath but kept on running. The road could not be much further! Mackie would have the engine running, and then I'd be safe.",
                text.get(0));
        Assert.assertEquals(2, story.getCurrentChoices().size());
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(2, text.size());
        Assert.assertEquals("I reached the road and looked about. And would you believe it?", text.get(0));
        Assert.assertEquals("The road was empty. Mackie was nowhere to be seen.", text.get(1));
    }

    /**
     *     "- allow nested options to pop out to a higher level gather"
     */
    @Test
    public void nestedFlow() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/gather/nested-flow.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(2);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(2, text.size());
        Assert.assertEquals("\"Myself!\"", text.get(0));
        Assert.assertEquals(
                "Mrs. Christie lowered her manuscript a moment. The rest of the writing group sat, open-mouthed.",
                text.get(1));
    }

    /**
     *     "- gather the flow back together again from arbitrarily deep options
     */
    @Test
    public void deepNesting() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/gather/deep-nesting.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(2, text.size());
        Assert.assertEquals("\"...Tell us a tale Captain!\"", text.get(0));
        Assert.assertEquals("To a man, the crew began to yawn.", text.get(1));
    }

    /**
     *     "- offer a compact way to weave and blend text and options (Example 1)"
     */
    @Test
    public void complexFlow1() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/gather/complex-flow.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(1);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals("... but I said nothing and we passed the day in silence.", text.get(0));
    }

    /**
     *     "- offer a compact way to weave and blend text and options (Example 2)"
     */
    @Test
    public void complexFlow2() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/gather/complex-flow.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(3, text.size());
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(2, text.size());
        story.chooseChoiceIndex(1);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(2, text.size());
        story.chooseChoiceIndex(1);

        text.clear();
        TestUtils.nextAll(story, text);
        Assert.assertEquals(3, text.size());
    }
}

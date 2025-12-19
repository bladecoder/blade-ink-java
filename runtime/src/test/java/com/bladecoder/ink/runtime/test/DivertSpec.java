package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.runtime.Story;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class DivertSpec {

    /**
     *     "- divert text from one knot/stitch to another"
     */
    @Test
    public void simpleDivert() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/divert/simple-divert.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(2, text.size());
        Assert.assertEquals("We arrived into London at 9.45pm exactly.", text.get(0));
        Assert.assertEquals("We hurried home to Savile Row as fast as we could.", text.get(1));
    }

    /**
     *    "- divert from one line of text to new content invisibly"
     */
    @Test
    public void invisibleDivert() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/divert/invisible-divert.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);

        Assert.assertEquals(1, text.size());
        Assert.assertEquals("We hurried home to Savile Row as fast as we could.", text.get(0));
    }

    /**
     *      "- branch directly from choices"
     */
    @Test
    public void divertOnChoice() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/divert/divert-on-choice.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);

        Assert.assertEquals(1, text.size());
        Assert.assertEquals("You open the gate, and step out onto the path.", text.get(0));
    }

    /**
     *    "- be usable to branch and join text seamlessly (example 1)"
     */
    @Test
    public void complexBranching1() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/divert/complex-branching.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(0);

        text.clear();
        TestUtils.nextAll(story, text);

        Assert.assertEquals(2, text.size());
        Assert.assertEquals("\"There is not a moment to lose!\" I declared.", text.get(0));
        Assert.assertEquals("We hurried home to Savile Row as fast as we could.", text.get(1));
    }

    /**
     *    "- be usable to branch and join text seamlessly (example 2)"
     */
    @Test
    public void complexBranching2() throws Exception {
        List<String> text = new ArrayList<String>();

        String json = TestUtils.getJsonString("inkfiles/divert/complex-branching.ink.json");
        Story story = new Story(json);

        TestUtils.nextAll(story, text);
        story.chooseChoiceIndex(1);

        text.clear();
        TestUtils.nextAll(story, text);

        Assert.assertEquals(3, text.size());
        Assert.assertEquals("\"Monsieur, let us savour this moment!\" I declared.", text.get(0));
        Assert.assertEquals("My master clouted me firmly around the head and dragged me out of the door.", text.get(1));
        Assert.assertEquals("He insisted that we hurried home to Savile Row as fast as we could.", text.get(2));
    }
}

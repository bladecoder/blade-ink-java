package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.runtime.Story;
import org.junit.Assert;
import org.junit.Test;

public class TagSpecTest {

    /**
     * "- basic test for tags"
     */
    @Test
    public void testTags() throws Exception {

        String json = TestUtils.getJsonString("inkfiles/tags/tags.ink.json");
        Story story = new Story(json);

        String[] globalTags = {"author: Joe", "title: My Great Story"};
        String[] knotTags = {"knot tag"};
        String[] knotTagWhenContinuedTwice = {"end of knot tag"};
        String[] stitchTags = {"stitch tag"};

        Assert.assertArrayEquals(globalTags, story.getGlobalTags().toArray());

        Assert.assertEquals("This is the content\n", story.Continue());

        Assert.assertArrayEquals(globalTags, story.getCurrentTags().toArray());

        Assert.assertArrayEquals(knotTags, story.tagsForContentAtPath("knot").toArray());
        Assert.assertArrayEquals(stitchTags, story.tagsForContentAtPath("knot.stitch").toArray());

        story.choosePathString("knot");
        Assert.assertEquals("Knot content\n", story.Continue());
        Assert.assertArrayEquals(knotTags, story.getCurrentTags().toArray());
        Assert.assertEquals("", story.Continue());
        Assert.assertArrayEquals(knotTagWhenContinuedTwice, story.getCurrentTags().toArray());
    }

    @Test
    public void testTagsInSeq() throws Exception {

        String json = TestUtils.getJsonString("inkfiles/tags/tagsInSeq.ink.json");
        Story story = new Story(json);

        Assert.assertEquals("A red sequence.\n", story.Continue());
        Assert.assertArrayEquals(new String[]{"red"}, story.getCurrentTags().toArray());

        Assert.assertEquals("A white sequence.\n", story.Continue());
        Assert.assertArrayEquals(new String[]{"white"}, story.getCurrentTags().toArray());
    }

    @Test
    public void testTagsInChoice() throws Exception {

        String json = TestUtils.getJsonString("inkfiles/tags/tagsInChoice.ink.json");
        Story story = new Story(json);

        story.Continue();
        Assert.assertEquals(0, story.getCurrentTags().size());
        Assert.assertEquals(1, story.getCurrentChoices().size());
        Assert.assertArrayEquals(new String[]{"one", "two"}, story.getCurrentChoices().get(0).getTags().toArray());

        story.chooseChoiceIndex(0);

        Assert.assertEquals("one three", story.Continue());
        Assert.assertArrayEquals(new String[]{"one", "three"}, story.getCurrentTags().toArray());
    }

    @Test
    public void testTagsDynamicContent() throws Exception {

        String json = TestUtils.getJsonString("inkfiles/tags/tagsDynamicContent.ink.json");
        Story story = new Story(json);

        Assert.assertEquals("tag\n", story.Continue());
        Assert.assertArrayEquals(new String[]{"pic8red.jpg"}, story.getCurrentTags().toArray());
    }
}

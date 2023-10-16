package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.runtime.Story;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TagSpecForV20Test {

    /**
     * "- basic test for tags"
     */
    @Test
    public void testTags() throws Exception {

        String json = TestUtils.getJsonString("inkfiles/tags/v20/tags.ink.json");
        Story story = new Story(json);

        String[] globalTags = {"author: Joe", "title: My Great Story"};
        String[] knotTags = {"knot tag"};
        String[] knotTagWhenContinuedTwice = {"end of knot tag"};
        String[] stitchTags = {"stitch tag"};

        Assert.assertArrayEquals(globalTags, story.getGlobalTags().toArray());

        Assert.assertEquals("This is the content\n", story.Continue());

        Assert.assertArrayEquals(globalTags, story.getCurrentTags().toArray());

        Assert.assertArrayEquals(knotTags, story.tagsForContentAtPath("knot").toArray());
        Assert.assertArrayEquals(
            stitchTags, story.tagsForContentAtPath("knot.stitch").toArray());

        story.choosePathString("knot");
        Assert.assertEquals("Knot content\n", story.Continue());
        Assert.assertArrayEquals(knotTags, story.getCurrentTags().toArray());
        Assert.assertEquals("", story.Continue());
        Assert.assertArrayEquals(
            knotTagWhenContinuedTwice, story.getCurrentTags().toArray());
    }

    @Test
    public void testTagsInChoice() throws Exception {

        String json = TestUtils.getJsonString("inkfiles/tags/v20/tagOnChoice.ink.json");
        Story story = new Story(json);

        story.Continue();
        story.chooseChoiceIndex(0);

        String txt = story.Continue();
        List<String> tags = story.getCurrentTags();

        Assert.assertEquals("Hello", txt);
        Assert.assertEquals(1, tags.size());
        Assert.assertEquals("hey", tags.get(0));
    }

}

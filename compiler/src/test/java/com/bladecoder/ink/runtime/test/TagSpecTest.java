package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.runtime.Story;
import org.junit.Assert;
import org.junit.Test;

public class TagSpecTest {

    /**
     * "- basic test for tags"
     */
    @Test
    public void testTags() throws Exception {

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/tags/tags.ink"));
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
    public void testTagsInSeq() throws Exception {

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/tags/tagsInSeq.ink"));
        Story story = new Story(json);

        Assert.assertEquals("A red sequence.\n", story.Continue());
        Assert.assertArrayEquals(new String[] {"red"}, story.getCurrentTags().toArray());

        Assert.assertEquals("A white sequence.\n", story.Continue());
        Assert.assertArrayEquals(new String[] {"white"}, story.getCurrentTags().toArray());
    }

    @Test
    public void testTagsInChoice() throws Exception {

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/tags/tagsInChoice.ink"));
        Story story = new Story(json);

        story.Continue();
        Assert.assertEquals(0, story.getCurrentTags().size());
        Assert.assertEquals(1, story.getCurrentChoices().size());
        Assert.assertArrayEquals(
                new String[] {"one", "two"},
                story.getCurrentChoices().get(0).getTags().toArray());

        story.chooseChoiceIndex(0);

        Assert.assertEquals("one three", story.Continue());
        Assert.assertArrayEquals(
                new String[] {"one", "three"}, story.getCurrentTags().toArray());
    }

    @Test
    public void testTagsInChoiceDynamicContent() throws Exception {

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/tags/tagsInChoiceDynamic.ink"));
        Story story = new Story(json);

        story.Continue();
        Assert.assertEquals(0, story.getCurrentTags().size());
        Assert.assertEquals(3, story.getCurrentChoices().size());
        Assert.assertArrayEquals(
                new String[] {"tag Name"},
                story.getCurrentChoices().get(0).getTags().toArray());
        Assert.assertArrayEquals(
                new String[] {"tag 1 Name 2 3 4"},
                story.getCurrentChoices().get(1).getTags().toArray());
        Assert.assertArrayEquals(
                new String[] {"Name tag 1 2 3 4"},
                story.getCurrentChoices().get(2).getTags().toArray());
    }

    @Test
    public void testTagsDynamicContent() throws Exception {

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/tags/tagsDynamicContent.ink"));
        Story story = new Story(json);

        Assert.assertEquals("tag\n", story.Continue());
        Assert.assertArrayEquals(
                new String[] {"pic8red.jpg"}, story.getCurrentTags().toArray());
    }

    @Test
    public void testTagsInLines() throws Exception {

        Compiler compiler = new Compiler();

        String json = compiler.compile(TestUtils.readFileAsString("inkfiles/tags/tagsInLines.ink"));
        Story story = new Story(json);

        Assert.assertEquals("í\n", story.Continue());
        Assert.assertEquals("a\n", story.Continue());
    }
}

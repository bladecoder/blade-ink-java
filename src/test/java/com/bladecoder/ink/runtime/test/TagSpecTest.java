package com.bladecoder.ink.runtime.test;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class TagSpecTest {
	
	/**
	 *     "- basic test for tags" 
	 */
	@Test
	public void testTags() throws Exception {
		
		String json = TestUtils.getJsonString("inkfiles/tags/tags.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		String[] globalTags = {"author: Joe", "title: My Great Story"};
		String[] knotTags = {"knot tag"};
		String[] stitchTags = {"stitch tag"};
		
		Assert.assertArrayEquals(globalTags, story.globalTags().toArray());
		
		Assert.assertEquals("This is the content\n", story.Continue ());
		
		Assert.assertArrayEquals(globalTags, story.getCurrentTags().toArray());
				
		Assert.assertArrayEquals(knotTags, story.tagsForContentAtPath("knot").toArray());
		Assert.assertArrayEquals(stitchTags, story.tagsForContentAtPath ("knot.stitch").toArray());
	}
	
}

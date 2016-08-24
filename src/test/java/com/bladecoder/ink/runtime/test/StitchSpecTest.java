package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class StitchSpecTest {
	
	/**
	 *     "- be automatically started with if there is no content in a knot" 
	 */
	@Test
	public void autoStitch() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/stitch/auto-stitch.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I settled my master.", text.get(0));
	}
	
	/**
	 *     "- be automatically diverted to if there is no other content in a knot" 
	 */
	@Test
	public void autoStitch2() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/stitch/auto-stitch.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(1);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I settled my master.", text.get(0));
	}
	
	/**
	 *     "- not be diverted to if the knot has content" 
	 */
	@Test
	public void manualStitch() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/stitch/manual-stitch.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("How shall we travel?", text.get(0));
		story.chooseChoiceIndex(1);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I put myself in third.", text.get(0));
	}
	
	/**
	 *     "- be usable locally without the full name"
	 */
	@Test
	public void manualStitch2() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/stitch/manual-stitch.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("How shall we travel?", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I settled my master.", text.get(0));
	}	
}

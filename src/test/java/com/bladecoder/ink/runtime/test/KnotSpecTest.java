package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class KnotSpecTest {

	/**
	 *      "A single line of plain text in an ink file"
	 */
	@Test
	public void testSingleLine() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/knot/single-line.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Hello, world!", text.get(0));
	}
	
	/**
	 *      "Multiple lines of plain text in an ink file"
	 */
	@Test
	public void testMultiLine() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/knot/multi-line.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(3, text.size());
		Assert.assertEquals("Hello, world!", text.get(0));
		Assert.assertEquals("Hello?", text.get(1));
		Assert.assertEquals("Hello, are you there?", text.get(2));
	}
	
	/**
	 *      "- strip empty lines of output"
	 */
	@Test
	public void stripEmptyLines() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/knot/strip-empty-lines.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(3, text.size());
		Assert.assertEquals("Hello, world!", text.get(0));
		Assert.assertEquals("Hello?", text.get(1));
		Assert.assertEquals("Hello, are you there?", text.get(2));
	}
	
	/**
	 *      "- handle string parameters in a divert"
	 */
	@Test
	public void paramStrings() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/knot/param-strings.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(2);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("\"I accuse myself!\" Poirot declared.", text.get(0));
	}
	
	/**
	 *      "- handle passing integer as parameters in a divert" 
	 */
	@Test
	public void paramInts() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/knot/param-ints.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(1);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("You give 2 dollars.", text.get(0));
	}

	/**
	 *      "- handle passing floats as parameters in a divert" 
	 *      
	 *      FIXME: INKLECATE BUG?
	 */
	@Test
	public void paramFloats() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/knot/param-floats.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(1);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("You give 2.5 dollars.", text.get(0));
	}
	
	/**
	 *      "- handle passing variables as parameters in a divert" 
	 */
	@Test
	public void paramVars() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/knot/param-vars.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(1);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("You give 2 dollars.", text.get(0));
	}
	
	/**
	 *      "- handle passing multiple values as parameters in a divert" 
	 */
	@Test
	public void paramMulti() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/knot/param-multi.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("You give 1 or 2 dollars. Hmm.", text.get(0));
	}	
	
	/**
	 *      "- should support recursive calls with parameters on a knot" 
	 */
	@Test
	public void paramRecurse() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/knot/param-recurse.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());
		Assert.assertEquals("\"The result is 120!\" you announce.", text.get(0));
	}
}

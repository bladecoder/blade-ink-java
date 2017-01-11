package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class GlueSpecTest {
	
	/**
	 *      "- bind text together across multiple lines of text"
	 */
	@Test
	public void simpleGlue() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/glue/simple-glue.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Some content with glue.", text.get(0));
	}
	
	/**
	 *      "- bind text together across multiple lines of text"
	 */
	@Test
	public void glueWithDivert() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/glue/glue-with-divert.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("We hurried home to Savile Row as fast as we could.", text.get(0));
	}
	
	/**
	 *      "- bind text together across multiple lines of text"
	 */
	@Test
	public void testLeftRightGlueMatching() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/glue/left-right-glue-matching.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());
		Assert.assertEquals("A line.", text.get(0));
		Assert.assertEquals("Another line.", text.get(1));
	}
	
	/**
	 *      "- bind text together across multiple lines of text"
	 */
	@Test
	public void testBugfix1() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/glue/testbugfix1.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());
		Assert.assertEquals("A", text.get(0));
		Assert.assertEquals("C", text.get(1));
	}
	
	/**
	 *      "- bind text together across multiple lines of text"
	 */
	@Test
	public void testBugfix2() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/glue/testbugfix2.ink.json");
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());
		// Assert.assertEquals("A ", text.get(0)); // TODO nextAll is trimming!
		Assert.assertEquals("X", text.get(1));
	}
}

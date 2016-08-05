package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ContentTest {

	@Test
	public void testSingleLine() throws Exception {
		List<String> errors = new ArrayList<String>();
		
		List<String> text = TestUtils.runStory("inkfiles/content/single-line.ink.json", null, errors);
		
		Assert.assertEquals(0, errors.size());
		Assert.assertEquals("Hello, world!\n", TestUtils.joinText(text));
	}
	
	@Test
	public void testMultiLine() throws Exception {
		List<String> errors = new ArrayList<String>();
		
		List<String>  text = TestUtils.runStory("inkfiles/content/multi-line.ink.json", null, errors);
		
		Assert.assertEquals(0, errors.size());
		Assert.assertEquals("Hello, world!\nHello?\nHello, are you there?\n", TestUtils.joinText(text));
	}

}

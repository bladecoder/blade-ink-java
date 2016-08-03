package com.bladecoder.ink.runtime.test;

import org.junit.Assert;
import org.junit.Test;

public class SingleLineTest {

	private static final String FILENAME = "inkfiles/content/single-line.ink.json";

	@Test
	public void test() throws Exception {
		String text = TestUtils.runStory(FILENAME);
		System.out.println(text);
		Assert.assertEquals("Hello, world!\n", text);
	}

}

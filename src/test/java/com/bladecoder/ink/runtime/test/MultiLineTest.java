package com.bladecoder.ink.runtime.test;

import org.junit.Assert;
import org.junit.Test;

public class MultiLineTest {

	private static final String FILENAME = "inkfiles/content/multi-line.ink.json";

	@Test
	public void test() throws Exception {
		String text = TestUtils.runStory(FILENAME);
		System.out.println(text);
		Assert.assertEquals("Hello, world!\nHello?\nHello, are you there?\n", text);
	}

}

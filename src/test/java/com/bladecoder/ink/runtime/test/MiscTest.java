package com.bladecoder.ink.runtime.test;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class MiscTest {

	/**
	 * Issue: https://github.com/bladecoder/blade-ink/issues/15
	 */
	@Test
	public void issue15() throws Exception {
		String json = TestUtils.getJsonString("inkfiles/misc/issue15.ink.json");
		Story story = new Story(json);

		Assert.assertEquals("This is a test\n", story.Continue());

		while (story.canContinue()) {
			System.out.println(story.buildStringOfHierarchy());
			String line = story.Continue();

			if (line.startsWith("SET_X:")) {
				story.getVariablesState().set("x", 100);
			} else {
				Assert.assertEquals("X is set\n", line);
			}
		}
	}
}

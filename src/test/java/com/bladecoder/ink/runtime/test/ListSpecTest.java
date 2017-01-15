package com.bladecoder.ink.runtime.test;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class ListSpecTest {

	/**
	 * "- testListBasicOperations"
	 */
	@Test
	public void testListBasicOperations() throws Exception {

		String json = TestUtils.getJsonString("inkfiles/lists/list-basic-operations.ink.json");
		Story story = new Story(json);

		Assert.assertEquals("b, d\na, b, c, e\nb, c\n0\n1\n1\n", story.continueMaximally());
	}

	/**
	 * "- TestListMixedItems"
	 */
	@Test
	public void testListMixedItems() throws Exception {

		String json = TestUtils.getJsonString("inkfiles/lists/list-mixed-items.ink.json");
		Story story = new Story(json);

		Assert.assertEquals("b, d\na, b, c, e\nb, c\n0\n1\n1\n", story.continueMaximally());
	}

	/**
	 * "- TestMoreListOperations"
	 */
	@Test
	public void testMoreListOperations() throws Exception {

		String json = TestUtils.getJsonString("inkfiles/lists/more-list-operations.ink.json");
		Story story = new Story(json);

		Assert.assertEquals("b, d\na, b, c, e\nb, c\n0\n1\n1\n", story.continueMaximally());
	}

	/**
	 * "- TestEmptyListOrigin"
	 */
	@Test
	public void testEmptyListOrigin() throws Exception {

		String json = TestUtils.getJsonString("inkfiles/lists/empty-list-origin.ink.json");
		Story story = new Story(json);

		Assert.assertEquals("b, d\na, b, c, e\nb, c\n0\n1\n1\n", story.continueMaximally());
	}

	/**
	 * "- TestListSaveLoad"
	 */
	@Test
	public void testListSaveLoad() throws Exception {

		String json = TestUtils.getJsonString("inkfiles/lists/list-save-load.ink.json");
		Story story = new Story(json);

		Assert.assertEquals("a, x, c\n", story.continueMaximally());

		String savedState = story.getState().toJson();

		// Compile new version of the story
		story = new Story(json);

		// Load saved game
		story.getState().loadJson(savedState);

		story.choosePathString("elsewhere");
		Assert.assertEquals("a, x, c, z\n", story.continueMaximally());
	}

}

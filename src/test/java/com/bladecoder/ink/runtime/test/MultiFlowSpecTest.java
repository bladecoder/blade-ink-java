package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class MultiFlowSpecTest {

	@Test
	public void basics() throws Exception {
		List<String> text = new ArrayList<>();

		String json = TestUtils.getJsonString("inkfiles/runtime/multiflow-basics.ink.json");
		Story story = new Story(json);

		story.switchFlow("First");
		story.choosePathString("knot1");
		Assert.assertEquals("knot 1 line 1\n", story.Continue());

		story.switchFlow("Second");
		story.choosePathString("knot2");
		Assert.assertEquals("knot 2 line 1\n", story.Continue());

		story.switchFlow("First");
		Assert.assertEquals("knot 1 line 2\n", story.Continue());

		story.switchFlow("Second");
		Assert.assertEquals("knot 2 line 2\n", story.Continue());
	}

	@Test
	public void testMultiFlowSaveLoadThreads() throws Exception {
		List<String> text = new ArrayList<>();

		String json = TestUtils.getJsonString("inkfiles/runtime/multiflow-saveloadthreads.ink.json");
		Story story = new Story(json);

		// Default flow
		Assert.assertEquals("Default line 1\n", story.Continue());

		story.switchFlow("Blue Flow");
		story.choosePathString("blue");
		Assert.assertEquals("Hello I'm blue\n", story.Continue());

		story.switchFlow("Red Flow");
		story.choosePathString("red");
		Assert.assertEquals("Hello I'm red\n", story.Continue());

		// Test existing state remains after switch (blue)
		story.switchFlow("Blue Flow");
		Assert.assertEquals("Hello I'm blue\n", story.getCurrentText());
		Assert.assertEquals("Thread 1 blue choice", story.getCurrentChoices().get(0).getText());

		// Test existing state remains after switch (red)
		story.switchFlow("Red Flow");
		Assert.assertEquals("Hello I'm red\n", story.getCurrentText());
		Assert.assertEquals("Thread 1 red choice", story.getCurrentChoices().get(0).getText());

		// Save/load test
		String saved = story.getState().toJson();

		// Test choice before reloading state before resetting
		story.chooseChoiceIndex(0);
		Assert.assertEquals("Thread 1 red choice\nAfter thread 1 choice (red)\n", story.continueMaximally());
		story.resetState();

		// Load to pre-choice: still red, choose second choice
		story.getState().loadJson(saved);

		story.chooseChoiceIndex(1);
		Assert.assertEquals("Thread 2 red choice\nAfter thread 2 choice (red)\n", story.continueMaximally());

		// Load: switch to blue, choose 1
		story.getState().loadJson(saved);
		story.switchFlow("Blue Flow");
		story.chooseChoiceIndex(0);
		Assert.assertEquals("Thread 1 blue choice\nAfter thread 1 choice (blue)\n", story.continueMaximally());

		// Load: switch to blue, choose 2
		story.getState().loadJson(saved);
		story.switchFlow("Blue Flow");
		story.chooseChoiceIndex(1);
		Assert.assertEquals("Thread 2 blue choice\nAfter thread 2 choice (blue)\n", story.continueMaximally());

		// Remove active blue flow, should revert back to global flow
		story.removeFlow("Blue Flow");
		Assert.assertEquals("Default line 2\n", story.Continue());
	}

}

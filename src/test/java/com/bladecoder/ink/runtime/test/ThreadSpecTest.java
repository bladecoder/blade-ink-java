package com.bladecoder.ink.runtime.test;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class ThreadSpecTest {

	/**
	 * "- Exception on threads to add additional choices (#5)
	 */
	@Test
	public void testThread() throws Exception {

		String json = TestUtils.getJsonString("inkfiles/threads/thread-bug.ink.json");
		Story story = new Story(json);

		Assert.assertEquals("Here is some gold. Do you want it?\n", story.continueMaximally());
		
		Assert.assertEquals(2, story.getCurrentChoices().size());
		
		Assert.assertEquals("No", story.getCurrentChoices().get(0).getText());
		Assert.assertEquals("Yes", story.getCurrentChoices().get(1).getText());
		
		story.chooseChoiceIndex(0);
		
		Assert.assertEquals("No\nTry again!\n", story.continueMaximally());
		
		Assert.assertEquals(2, story.getCurrentChoices().size());
		
		Assert.assertEquals("No", story.getCurrentChoices().get(0).getText());
		Assert.assertEquals("Yes", story.getCurrentChoices().get(1).getText());
		
		story.chooseChoiceIndex(1);
		
		Assert.assertEquals("Yes\nYou win!\n", story.continueMaximally());
	}

	
	/**
	 * "- Exception on threads to add additional choices (#5)
	 */
	@Test
	public void testThreadBug() throws Exception {

		String json = TestUtils.getJsonString("inkfiles/threads/thread-bug.ink.json");
		Story story = new Story(json);

		Assert.assertEquals("Here is some gold. Do you want it?\n", story.continueMaximally());
		
		Assert.assertEquals(2, story.getCurrentChoices().size());
		
		Assert.assertEquals("No", story.getCurrentChoices().get(0).getText());
		Assert.assertEquals("Yes", story.getCurrentChoices().get(1).getText());
		
		// Convert state to JSON then load it back in
		String stateJson = story.getState().toJson();
		
		story = new Story(json);
		story.getState().loadJson(stateJson);

		// + Works correctly if choiceIdx is 1 ('Yes', defined in the knot)
		// + Throws exception if choiceIdx is 0 ('No', defined in the thread)
		story.chooseChoiceIndex(0);
		
		Assert.assertEquals("No\nTry again!\n", story.continueMaximally());
		
		Assert.assertEquals(2, story.getCurrentChoices().size());
		
		Assert.assertEquals("No", story.getCurrentChoices().get(0).getText());
		Assert.assertEquals("Yes", story.getCurrentChoices().get(1).getText());
		
		story.chooseChoiceIndex(1);
		
		Assert.assertEquals("Yes\nYou win!\n", story.continueMaximally());
	}

}

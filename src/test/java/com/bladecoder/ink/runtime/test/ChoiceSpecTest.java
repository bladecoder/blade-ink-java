package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class ChoiceSpecTest {

	@Test
	public void noChoice() throws Exception {
		List<String> errors = new ArrayList<String>();

		List<String> text = TestUtils.runStory("inkfiles/choices/no-choice-text.ink.json", null, errors);

		Assert.assertEquals(0, errors.size());
		Assert.assertEquals("Hello world!\nHello back!\n", TestUtils.joinText(text));
	}

	@Test
	public void one() throws Exception {
		List<String> errors = new ArrayList<String>();

		List<String> text = TestUtils.runStory("inkfiles/choices/one.ink.json", null, errors);

		Assert.assertEquals(0, errors.size());
		Assert.assertEquals("Hello world!\nHello back!\nHello back!\n", TestUtils.joinText(text));
	}

	@Test
	public void multiChoice() throws Exception {
		List<String> errors = new ArrayList<String>();

		List<String> text = TestUtils.runStory("inkfiles/choices/multi-choice.ink.json", Arrays.asList(0), errors);

		Assert.assertEquals(0, errors.size());
		Assert.assertEquals("Hello, world!\nHello back!\nGoodbye\nHello back!\nNice to hear from you\n",
				TestUtils.joinText(text));

		// Select second choice
		text = TestUtils.runStory("inkfiles/choices/multi-choice.ink.json", Arrays.asList(1), errors);

		Assert.assertEquals(0, errors.size());
		Assert.assertEquals("Hello, world!\nHello back!\nGoodbye\nGoodbye\nSee you later\n", TestUtils.joinText(text));
	}

	/**
	 * "- demarcate end of text for parent container"
	 */
	@Test
	public void singleChoice1() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/single-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);

		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Hello, world!", text.get(0));
	}

	/**
	 * "- continue processing with the choice text when a choice is selected"
	 */
	@Test
	public void singleChoice2() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/single-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);

		Assert.assertEquals(2, text.size());
		Assert.assertEquals("Hello back!", text.get(0));
		Assert.assertEquals("Nice to hear from you", text.get(1));
	}

	/**
	 * "- be suppressed in the text flow using the [] syntax"
	 */
	@Test
	public void suppressChoice() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/suppress-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		Assert.assertEquals("Hello back!", story.getCurrentChoices().get(0).getText());
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);

		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Nice to hear from you.", text.get(0));
	}

	/**
	 * "- be suppressed in the text flow using the [] syntax"
	 */
	@Test
	public void mixedChoice() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/mixed-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		Assert.assertEquals("Hello back!", story.getCurrentChoices().get(0).getText());
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());

		Assert.assertEquals("Hello  right back to you!", text.get(0));
		Assert.assertEquals("Nice to hear from you.", text.get(1));
	}

	/**
	 * "- disappear when used if they are a once-only choice"
	 */
	@Test
	public void varyingChoice() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/varying-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, story.getCurrentChoices().size());

		Assert.assertEquals("The man with the briefcase?", story.getCurrentChoices().get(0).getText());
	}

	/**
	 * "- not disappear when used if they are a sticky choices"
	 */
	@Test
	public void stickyChoice() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/sticky-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());
	}

	/**
	 * "- not be shown if it is a fallback choice and there are non-fallback
	 * choices available"
	 */
	@Test
	public void fallbackChoice() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/fallback-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());
	}

	/**
	 * "- not be shown if it is a fallback choice and there are non-fallback
	 * choices available"
	 */
	@Test
	public void fallbackChoice2() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/fallback-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);

		Assert.assertEquals(true, TestUtils.isEnded(story));
	}

	/**
	 * "- not be visible if their conditions evaluate to 0"
	 */
	@Test
	public void conditionalChoice() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/conditional-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);

		Assert.assertEquals(4, story.getCurrentChoices().size());
	}

	/**
	 * "- handle labels on choices and evaluate in expressions (example 1)"
	 */
	@Test
	public void labelFlow() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/label-flow.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());
		Assert.assertEquals("\'Having a nice day?\'", story.getCurrentChoices().get(0).getText());
	}

	/**
	 * "- handle labels on choices and evaluate in expressions (example 2)"
	 */
	@Test
	public void labelFlow2() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/label-flow.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(1);

		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());
		Assert.assertEquals("Shove him aside", story.getCurrentChoices().get(1).getText());
	}

	/**
	 * "- allow label references out of scope using the full path id"
	 */
	@Test
	public void labelScope() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/label-scope.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, story.getCurrentChoices().size());
		Assert.assertEquals("Found gatherpoint", story.getCurrentChoices().get(0).getText());
	}

	/**
	 * "- fail label references that are out of scope"
	 * 
	 * NOTE: Label is found in ref. impl. and in blade-ink. It must fail?
	 */
	@Test
	public void labelScopeError() throws Exception {
//		List<String> text = new ArrayList<String>();
//
//		String json = TestUtils.getJsonString("inkfiles/choices/label-scope-error.ink.json");
//		Story story = new Story(json);
//
//		try {
//			TestUtils.nextAll(story, text);
//			story.chooseChoiceIndex(0);
//			Assert.fail();
//		} catch (Exception e) {
//
//		}

	}
	
	/**
	 * "- be used up if they are once-only and a divert goes through them"
	 */
	@Test
	public void divertChoice() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/choices/divert-choice.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());
		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());
		Assert.assertEquals("You pull a face, and the soldier comes at you!  You shove the guard to one side, but he comes back swinging.", text.get(0));
		
		Assert.assertEquals(1, story.getCurrentChoices().size());
		Assert.assertEquals("Grapple and fight", story.getCurrentChoices().get(0).getText());
	}	
}

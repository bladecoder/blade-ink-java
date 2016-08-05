package com.bladecoder.ink.runtime.test;

import static org.junit.Assert.fail;

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
		Assert.assertEquals("Hello, world!\nHello back!\nGoodbye\nHello back!\nNice to hear from you\n", TestUtils.joinText(text));
		
		// Select second choice
		text = TestUtils.runStory("inkfiles/choices/multi-choice.ink.json", Arrays.asList(1), errors);
		
		Assert.assertEquals(0, errors.size());
		Assert.assertEquals("Hello, world!\nHello back!\nGoodbye\nGoodbye\nSee you later\n", TestUtils.joinText(text));
	}
	
	/**
	 *     "- not be visible if their conditions evaluate to 0"
	 */
	@Test
	public void conditionalChoice() throws Exception {
		List<String> errors = new ArrayList<String>();
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/choices/conditional-choice.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		System.out.println(story.buildStringOfHierarchy());
		
		while (story.canContinue()) {
			String line = story.Continue();
			System.out.print(line);
			text.add(line);
		}

		if (story.hasError()) {
			for (String errorMsg : story.getCurrentErrors()) {
				System.out.println(errorMsg);
				errors.add(errorMsg);
			}
			
			fail();
		}
		
		Assert.assertEquals(4, story.getCurrentChoices().size());
	}
}

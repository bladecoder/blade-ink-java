package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class ConditionalSpecTest {
	
	/**
	 *     "- evaluate the statements if the condition evaluates to true"
	 */
	@Test
	public void ifTrue() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/iftrue.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);		
		
		Assert.assertEquals(1, text.size());	
		Assert.assertEquals("The value is 1.", text.get(0));
	}
	
	/**
	 *    "- not evaluate the statement if the condition evaluates to false"
	 */
	@Test
	public void ifFalse() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/iffalse.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);		
		
		Assert.assertEquals(1, text.size());	
		Assert.assertEquals("The value is 3.", text.get(0));
	}
	
	/**
	 *    "- evaluate an else statement if it exists and no other condition evaluates to true"
	 */
	@Test
	public void ifElse() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/ifelse.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);		
		
		Assert.assertEquals(1, text.size());	
		Assert.assertEquals("The value is 1.", text.get(0));
	}
	
	/**
	 *    "- evaluate an extended else statement if it exists and no other condition evaluates to true"
	 */
	@Test
	public void ifElseExt() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/ifelse-ext.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);		
		
		Assert.assertEquals(1, text.size());	
		Assert.assertEquals("The value is -1.", text.get(0));
	}
	
	/**
	 *    "- evaluate an extended else statement with text and divert at the end"
	 */
	@Test
	public void ifElseExtText1() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/ifelse-ext-text1.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		
		Assert.assertEquals(1, text.size());	
		Assert.assertEquals("This is text 1.", text.get(0));
		
		Assert.assertEquals(1, story.getCurrentChoices().size());
		story.chooseChoiceIndex(0);
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());	
		Assert.assertEquals("This is the end.", text.get(1));
	}
	
	/**
	 *      "- evaluate an extended else statement with text and divert at the end"
	 */
	@Test
	public void ifElseExtText2() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/ifelse-ext-text2.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		
		Assert.assertEquals(1, text.size());	
		Assert.assertEquals("This is text 2.", text.get(0));
		
		Assert.assertEquals(1, story.getCurrentChoices().size());
		story.chooseChoiceIndex(0);
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());	
		Assert.assertEquals("This is the end.", text.get(1));
	}
	
	/**
	 *      "- evaluate an extended else statement with text and divert at the end"
	 */
	@Test
	public void ifElseExtText3() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/ifelse-ext-text3.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		
		Assert.assertEquals(1, text.size());	
		Assert.assertEquals("This is text 3.", text.get(0));
		
		Assert.assertEquals(1, story.getCurrentChoices().size());
		story.chooseChoiceIndex(0);
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());	
		Assert.assertEquals("This is the end.", text.get(1));
	}	
}

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
	
	/**
	 *      "- work with conditional content which is not only logic (example 1)"
	 */
	@Test
	public void condText1() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/condtext.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(3, text.size());	
		Assert.assertEquals("I stared at Monsieur Fogg. \"But surely you are not serious?\" I demanded.", text.get(1));
	}
	
	/**
	 *      "- work with conditional content which is not only logic (example 2)"
	 */
	@Test
	public void condText2() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/condtext.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(1);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());	
		Assert.assertEquals("I stared at Monsieur Fogg. \"But there must be a reason for this trip,\" I observed.", text.get(0));
	}
	
	/**
	 *      "- work with options as conditional content (example 1)"
	 */
	@Test
	public void condOpt1() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/condopt.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, story.getCurrentChoices().size());	
	}	
	
	/**
	 *      "- work with options as conditional content (example 2)"
	 */
	@Test
	public void condOpt2() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/condopt.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(1);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());	
	}
	
	/**
	 *      "- go through the alternatives and stick on last when the keyword is stopping"
	 */
	@Test
	public void stopping() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/stopping.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I entered the casino.", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I entered the casino again.", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Once more, I went inside.", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Once more, I went inside.", text.get(0));
		story.chooseChoiceIndex(0);		
	}	
	
	/**
	 *      "- show each in turn and then cycle when the keyword is cycle"
	 */
	@Test
	public void cycle() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/cycle.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I held my breath.", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I waited impatiently.", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I paused.", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I held my breath.", text.get(0));
		story.chooseChoiceIndex(0);		
	}
	
	/**
	 *      "- show each, once, in turn, until all have been shown when the keyword is once"
	 */
	@Test
	public void once() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/once.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Would my luck hold?", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Could I win the hand?", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(0, text.size());
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(0, text.size());
		story.chooseChoiceIndex(0);		
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(0, text.size());
	}
	
	/**
	 *      "- show one at random when the keyword is shuffle"
	 */
	@Test
	public void shuffle() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/shuffle.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());	

		// No check of the result, as that is random
	}
	
	/**
	 *      "- show multiple lines of texts from multiline list blocks"
	 */
	@Test
	public void multiline() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/multiline.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("At the table, I drew a card. Ace of Hearts.", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());
		Assert.assertEquals("I drew a card. 2 of Diamonds.", text.get(0));
		Assert.assertEquals("\"Should I hit you again,\" the croupier asks.", text.get(1));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());
		Assert.assertEquals("I drew a card. King of Spades.", text.get(0));
		Assert.assertEquals("\"You lose,\" he crowed.", text.get(1));
	}
	
	/**
	 *      "- allow for embedded diverts"
	 */
	@Test
	public void multilineDivert() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/multiline-divert.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("At the table, I drew a card. Ace of Hearts.", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());
		Assert.assertEquals("I drew a card. 2 of Diamonds.", text.get(0));
		Assert.assertEquals("\"Should I hit you again,\" the croupier asks.", text.get(1));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, text.size());
		Assert.assertEquals("I drew a card. King of Spades.", text.get(0));
		Assert.assertEquals("\"You lose,\" he crowed.", text.get(1));
	}	
	
	/**
	 *      "- allow for embedded choices"
	 */
	@Test
	public void multilineChoice() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/conditional/multiline-choice.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("At the table, I drew a card. Ace of Hearts.", text.get(0));
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(2, story.getCurrentChoices().size());
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("I left the table.", text.get(0));
	}		
}

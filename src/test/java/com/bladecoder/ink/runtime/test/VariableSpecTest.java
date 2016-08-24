package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class VariableSpecTest {
	
	/**
	 *      "- be declared with a VAR statement and print out a text value when used in content"
	 */
	@Test
	public void variableDeclaration() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/variable/variable-declaration.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("\"My name is Jean Passepartout, but my friend's call me Jackie. I'm 23 years old.\"", text.get(0));
	}
	
	/**
	 *      "- be declared with a VAR statement and print out a text value when used in content"
	 */
	@Test
	public void varCalc() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/variable/varcalc.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("The values are 1 and -1 and -6.", text.get(0));
	}
	
	/**
	 *      "- be declarable as diverts and be usable in text"
	 */
	@Test
	public void varDivert() throws Exception {
		List<String> text = new ArrayList<String>();
		
		String json = TestUtils.getJsonString("inkfiles/variable/var-divert.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(1);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Everybody dies.", text.get(0));
	}
}

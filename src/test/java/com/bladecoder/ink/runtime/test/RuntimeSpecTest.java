package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;
import com.bladecoder.ink.runtime.Story.ExternalFunction;
import com.bladecoder.ink.runtime.Story.VariableObserver;

public class RuntimeSpecTest {

	/**
	 * Test external function call.
	 */
	@Test
	public void externalFunction() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/external-function.ink.json").replace('\uFEFF', ' ');
		final Story story = new Story(json);

		story.bindExternalFunction("externalFunction", new ExternalFunction() {

			@Override
			public Object call(Object[] args) throws Exception {
				int x = story.tryCoerce(args[0], Integer.class);
				int y = story.tryCoerce(args[1], Integer.class);

				return x - y;
			}
		});

		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("The value is -1.", text.get(0));
	}

	/**
	 * Test external function fallback.
	 */
	@Test
	public void externalFunctionFallback() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/external-function.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);

		story.setAllowExternalFunctionFallbacks(true);

		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("The value is 7.", text.get(0));
	}
	
	private static int variableObserversExceptedValue = 5;

	/**
	 * Test variable observers.
	 */
	@Test
	public void variableObservers() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/variable-observers.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);

		story.observeVariable("x", new VariableObserver() {

			@Override
			public void call(String variableName, Object newValue) {
				if (!"x".equals(variableName))
					Assert.fail();
				try {
					if ((int)newValue != variableObserversExceptedValue)
						Assert.fail();
					
					variableObserversExceptedValue = 10;
				} catch (Exception e) {
					Assert.fail();
				}
			}
		});

		TestUtils.nextAll(story, text);
		story.chooseChoiceIndex(0);
		TestUtils.nextAll(story, text);
	}
	
	/**
	 * Test set/get variables from code.
	 */
	@Test
	public void setAndGetVariable() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/set-get-variables.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		
		Assert.assertEquals(10, (int)story.getVariablesState().get("x"));
		
		story.getVariablesState().set("x", 15);
		
		Assert.assertEquals(15, (int)story.getVariablesState().get("x"));
		
		story.chooseChoiceIndex(0);
		
		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals("OK", text.get(0));
	}

	
	/**
	 * Jump to knot from code.
	 */
	@Test
	public void jumpKnot() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/jump-knot.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		story.choosePathString("two");
		TestUtils.nextAll(story, text);		
		Assert.assertEquals("Two", text.get(0));
		
		text.clear();
		story.choosePathString("three");
		TestUtils.nextAll(story, text);		
		Assert.assertEquals("Three", text.get(0));
		
		text.clear();
		story.choosePathString("one");
		TestUtils.nextAll(story, text);		
		Assert.assertEquals("One", text.get(0));
		
		text.clear();
		story.choosePathString("two");
		TestUtils.nextAll(story, text);		
		Assert.assertEquals("Two", text.get(0));
	}
	
	/**
	 * Jump to stitch from code.
	 */
	@Test
	public void jumpStitch() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/jump-stitch.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		story.choosePathString("two.sthree");
		TestUtils.nextAll(story, text);		
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("Two.3", text.get(0));
		
		text.clear();
		story.choosePathString("one.stwo");
		TestUtils.nextAll(story, text);		
		Assert.assertEquals("One.2", text.get(0));
		
		text.clear();
		story.choosePathString("one.sone");
		TestUtils.nextAll(story, text);		
		Assert.assertEquals("One.1", text.get(0));
		
		text.clear();
		story.choosePathString("two.stwo");
		TestUtils.nextAll(story, text);		
		Assert.assertEquals("Two.2", text.get(0));
	}
	
	/**
	 * Read the visit counts from code.
	 */
	@Test
	public void readVisitCounts() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/read-visit-counts.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);
		
		TestUtils.nextAll(story, text);
		Assert.assertEquals(4, story.getState().visitCountAtPathString("two.s2"));
		Assert.assertEquals(5, story.getState().visitCountAtPathString("two"));
	}
}

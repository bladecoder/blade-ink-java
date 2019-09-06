package com.bladecoder.ink.runtime.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Profiler;
import com.bladecoder.ink.runtime.Story;
import com.bladecoder.ink.runtime.Story.ExternalFunction;
import com.bladecoder.ink.runtime.Story.VariableObserver;
import com.bladecoder.ink.runtime.StoryException;

public class RuntimeSpecTest {

	/**
	 * Test external function call.
	 */
	@Test
	public void externalFunction() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/external-function.ink.json");
		final Story story = new Story(json);

		story.bindExternalFunction("externalFunction", new ExternalFunction.TwoArguments<Integer, Integer, Integer>() {

			@Override
			public Integer call(Integer x, Integer y) {
				return x - y;
			}

			@Override
			protected Integer coerceFirstArg(Object arg) throws Exception {
				return story.tryCoerce(arg, Integer.class);
			}

			@Override
			protected Integer coerceSecondArg(Object arg) throws Exception {
				return story.tryCoerce(arg, Integer.class);
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

		String json = TestUtils.getJsonString("inkfiles/runtime/external-function.ink.json");
		Story story = new Story(json);

		story.setAllowExternalFunctionFallbacks(true);

		TestUtils.nextAll(story, text);
		Assert.assertEquals(1, text.size());
		Assert.assertEquals("The value is 7.0.", text.get(0));
	}

	private static int variableObserversExceptedValue = 5;

	/**
	 * Test variable observers.
	 */
	@Test
	public void variableObservers() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/variable-observers.ink.json");
		Story story = new Story(json);

		story.observeVariable("x", new VariableObserver() {

			@Override
			public void call(String variableName, Object newValue) {
				if (!"x".equals(variableName))
					Assert.fail();
				try {
					if ((int) newValue != variableObserversExceptedValue)
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

		String json = TestUtils.getJsonString("inkfiles/runtime/set-get-variables.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);

		Assert.assertEquals(10, (int) story.getVariablesState().get("x"));

		story.getVariablesState().set("x", 15);

		Assert.assertEquals(15, (int) story.getVariablesState().get("x"));

		story.chooseChoiceIndex(0);

		text.clear();
		TestUtils.nextAll(story, text);
		Assert.assertEquals("OK", text.get(0));
	}
	
	/**
	 * Test non existant variable.
	 */
	@Test
	public void testSetNonExistantVariable() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/set-get-variables.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		
		try { 
			story.getVariablesState().set("y", "earth");
			Assert.fail("Setting non existant variable.");
		} catch(StoryException e) {
			
		}

		Assert.assertEquals(10, (int) story.getVariablesState().get("x"));

		story.getVariablesState().set("x", 15);

		Assert.assertEquals(15, (int) story.getVariablesState().get("x"));

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

		String json = TestUtils.getJsonString("inkfiles/runtime/jump-knot.ink.json");
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
	 * Test the Profiler.
	 */
	@Test
	public void profiler() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/jump-knot.ink.json");
		Story story = new Story(json);
		
		Profiler profiler = story.startProfiling();

		story.choosePathString("two");
		TestUtils.nextAll(story, text);

		story.choosePathString("three");
		TestUtils.nextAll(story, text);

		story.choosePathString("one");
		TestUtils.nextAll(story, text);

		story.choosePathString("two");
		TestUtils.nextAll(story, text);
		
		String reportStr = profiler.report();
		
		story.endProfiling();
		
		System.out.println("PROFILER REPORT: " + reportStr);
	}	

	/**
	 * Jump to stitch from code.
	 */
	@Test
	public void jumpStitch() throws Exception {
		List<String> text = new ArrayList<String>();

		String json = TestUtils.getJsonString("inkfiles/runtime/jump-stitch.ink.json");
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

		String json = TestUtils.getJsonString("inkfiles/runtime/read-visit-counts.ink.json");
		Story story = new Story(json);

		TestUtils.nextAll(story, text);
		Assert.assertEquals(4, story.getState().visitCountAtPathString("two.s2"));
		Assert.assertEquals(5, story.getState().visitCountAtPathString("two"));
	}

	@Test
	public void testLoadSave() throws Exception {
		String json = TestUtils.getJsonString("inkfiles/runtime/load-save.ink.json");
		Story story = new Story(json);
				
		List<String> text = new ArrayList<String>();
		
		TestUtils.nextAll(story, text);

		Assert.assertEquals(1, text.size());
		Assert.assertEquals("We arrived into London at 9.45pm exactly.", text.get(0));

//		String choicesText = getChoicesText(story);
//		assertThat(choicesText, is(
//				"0:\"There is not a moment to lose!\"\n1:\"Monsieur, let us savour this moment!\"\n2:We hurried home\n"));

		// save the game state
		String saveString = story.getState().toJson();

		// recreate game and load state
		story = new Story(json);
		story.getState().loadJson(saveString);

		story.chooseChoiceIndex(0);

		TestUtils.nextAll(story, text);
		Assert.assertEquals(
				"\"There is not a moment to lose!\" I declared.", text.get(1));
		Assert.assertEquals("We hurried home to Savile Row as fast as we could.", text.get(2));

		// check that we are at the end
		Assert.assertEquals(false, story.canContinue());
		Assert.assertEquals(0, story.getCurrentChoices().size());
	}

}

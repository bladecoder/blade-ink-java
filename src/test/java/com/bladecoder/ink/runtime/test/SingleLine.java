package com.bladecoder.ink.runtime.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class SingleLine {

	private static final String FILENAME = "inkfiles/content/single-line.ink.json";

	@Test
	public void test() throws Exception {
		// 1) Load story
		String json = getJsonString(FILENAME).replace('\uFEFF', ' ');;
		
		System.out.println("-->" + Character.getNumericValue(json.charAt(0)) + "<--");
		
		System.out.println(json);
		
		Story story = new Story(json);

		// 2) Game content, line by line
		while (story.canContinue())
			System.out.println(story.Continue());

		// 3) Display story.currentChoices list, allow player to choose one
		// System.out.println(story.currentChoices[0].choiceText);
		// story.ChooseChoiceIndex(0);

		// fail("Not yet implemented");
	}

	private String getJsonString(String filename) throws IOException {
	
		InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream(filename);

		BufferedReader br = new BufferedReader(new InputStreamReader(systemResourceAsStream, "UTF-8"));
		
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}

}

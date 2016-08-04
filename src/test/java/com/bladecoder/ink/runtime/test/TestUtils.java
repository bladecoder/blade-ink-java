package com.bladecoder.ink.runtime.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.bladecoder.ink.runtime.Choice;
import com.bladecoder.ink.runtime.Story;

public class TestUtils {

	public static final String getJsonString(String filename) throws IOException {

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

	
	public static final String runStory(String filename, List<String> choiceList, List<String> errors) throws Exception {
		// 1) Load story
		String json = TestUtils.getJsonString(filename).replace('\uFEFF', ' ');

		System.out.println(json);

		Story story = new Story(json);

		StringBuffer text = new StringBuffer();

		System.out.println(story.BuildStringOfHierarchy());

		while (story.canContinue() || story.getCurrentChoices().size() > 0) {
			// 2) Game content, line by line
			while (story.canContinue()) {
				String line = story.Continue();
				System.out.print(line);
				text.append(line);
			}

			if (story.hasError()) {
				for (String errorMsg : story.getCurrentErrors()) {
					System.out.println(errorMsg);
					errors.add(errorMsg);
				}
			}

			// 3) Display story.currentChoices list, allow player to choose one
			if (story.getCurrentChoices().size() > 0) {

				for (Choice c : story.getCurrentChoices()) {
					System.out.println(c.gettext());
					text.append(c.gettext() + "\n");
				}

				if(choiceList == null)
					story.ChooseChoiceIndex((int) (Math.random() * story.getCurrentChoices().size()));
				else
					story.ChooseChoiceIndex((int) (Math.random() * story.getCurrentChoices().size()));
			}
		}

		return text.toString();
	}
}

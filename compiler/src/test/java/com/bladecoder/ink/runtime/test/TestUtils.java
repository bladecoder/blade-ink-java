package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.runtime.Choice;
import com.bladecoder.ink.runtime.Story;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class TestUtils {

    public static String getJsonString(String filename) throws IOException {

        InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream(filename);

        assert systemResourceAsStream != null;
        try (BufferedReader br =
                new BufferedReader(new InputStreamReader(systemResourceAsStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        }
    }

    public static List<String> runStory(String filename, List<Integer> choiceList, List<String> errors)
            throws Exception {
        // 1) Load story
        String json = getJsonString(filename);

        Story story = new Story(json);

        List<String> text = new ArrayList<>();

        //		System.out.println(story.BuildStringOfHierarchy());

        int choiceListIndex = 0;

        while (story.canContinue() || !story.getCurrentChoices().isEmpty()) {
            // System.out.println(story.buildStringOfHierarchy());

            // 2) Game content, line by line
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
            }

            // 3) Display story.currentChoices list, allow player to choose one
            if (!story.getCurrentChoices().isEmpty()) {

                for (Choice c : story.getCurrentChoices()) {
                    System.out.println(c.getText());
                    text.add(c.getText() + "\n");
                }

                if (choiceList == null || choiceListIndex >= choiceList.size())
                    story.chooseChoiceIndex(
                            (int) (Math.random() * story.getCurrentChoices().size()));
                else {
                    story.chooseChoiceIndex(choiceList.get(choiceListIndex));
                    choiceListIndex++;
                }
            }
        }

        return text;
    }

    public static String joinText(List<String> text) {
        StringBuilder sb = new StringBuilder();

        for (String s : text) {
            sb.append(s);
        }

        return sb.toString();
    }

    public static boolean isEnded(Story story) {
        return !story.canContinue() && story.getCurrentChoices().isEmpty();
    }

    public static void nextAll(Story story, List<String> text) throws Exception {
        while (story.canContinue()) {
            String line = story.Continue();
            System.out.print(line);

            if (!line.trim().isEmpty()) text.add(line.trim());
        }

        if (story.hasError()) {
            fail(TestUtils.joinText(story.getCurrentErrors()));
        }
    }
}

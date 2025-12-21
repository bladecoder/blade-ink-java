package com.bladecoder.ink.inklecate;

import com.bladecoder.ink.compiler.CommandLineInput;
import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.compiler.InkParser;
import com.bladecoder.ink.runtime.Choice;
import com.bladecoder.ink.runtime.Error.ErrorType;
import com.bladecoder.ink.runtime.SimpleJson;
import com.bladecoder.ink.runtime.Story;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CommandLinePlayer {
    private final Story story;
    private boolean autoPlay;
    private boolean keepOpenAfterStoryFinish;
    private final Compiler compiler;
    private final boolean jsonOutput;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public CommandLinePlayer(
            Story story, boolean autoPlay, Compiler compiler, boolean keepOpenAfterStoryFinish, boolean jsonOutput) {
        this.story = story;
        this.story.onError = this::onStoryError;
        this.autoPlay = autoPlay;
        this.compiler = compiler;
        this.keepOpenAfterStoryFinish = keepOpenAfterStoryFinish;
        this.jsonOutput = jsonOutput;
    }

    public void begin() throws Exception {
        evaluateStory();

        Random rand = new Random();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (!story.getCurrentChoices().isEmpty() || keepOpenAfterStoryFinish) {
            List<Choice> choices = story.getCurrentChoices();
            int choiceIdx = 0;
            boolean choiceIsValid = false;
            String userDivertedPath = null;

            if (autoPlay) {
                choiceIdx = rand.nextInt(choices.size());
            } else {
                if (!jsonOutput) {
                    System.out.println();
                    int i = 1;
                    for (Choice choice : choices) {
                        System.out.println(i + ": " + choice.getText());
                        i++;
                    }
                } else {
                    SimpleJson.Writer writer = new SimpleJson.Writer();
                    writer.writeObjectStart();
                    writer.writePropertyStart("choices");
                    writer.writeArrayStart();
                    for (Choice choice : choices) {
                        writer.writeObjectStart();
                        writer.writePropertyStart("text");
                        writer.write(choice.getText());
                        writer.writePropertyEnd();
                        if (choice.getTags() != null && !choice.getTags().isEmpty()) {
                            writer.writePropertyStart("tags");
                            writer.writeArrayStart();
                            for (String tag : choice.getTags()) {
                                writer.write(tag);
                            }
                            writer.writeArrayEnd();
                            writer.writePropertyEnd();
                            writer.writePropertyStart("tag_count");
                            writer.write(choice.getTags().size());
                            writer.writePropertyEnd();
                        }
                        writer.writeObjectEnd();
                    }
                    writer.writeArrayEnd();
                    writer.writePropertyEnd();
                    writer.writeObjectEnd();
                    System.out.println(writer.toString());
                }

                do {
                    if (!jsonOutput) {
                        System.out.print("?> ");
                    } else {
                        System.out.print("{\"needInput\": true}");
                    }

                    String userInput = reader.readLine();
                    if (userInput == null) {
                        if (jsonOutput) {
                            System.out.println("{\"close\": true}");
                        } else {
                            System.out.println("<User input stream closed.>");
                        }
                        return;
                    }

                    Compiler.CommandLineInputResult result = readCommandLineInput(userInput);
                    if (result.output != null) {
                        if (jsonOutput) {
                            SimpleJson.Writer writer = new SimpleJson.Writer();
                            writer.writeObjectStart();
                            writer.writeProperty("cmdOutput", result.output);
                            writer.writeObjectEnd();
                            System.out.println(writer.toString());
                        } else {
                            System.out.println(result.output);
                        }
                    }

                    if (result.requestsExit) {
                        return;
                    }

                    if (result.divertedPath != null) {
                        userDivertedPath = result.divertedPath;
                    }

                    if (result.choiceIdx >= 0) {
                        if (result.choiceIdx >= choices.size()) {
                            if (!jsonOutput) {
                                System.out.println("Choice out of range");
                            }
                        } else {
                            choiceIdx = result.choiceIdx;
                            choiceIsValid = true;
                        }
                    }

                } while (!choiceIsValid && userDivertedPath == null);
            }

            if (choiceIsValid) {
                story.chooseChoiceIndex(choiceIdx);
            } else if (userDivertedPath != null) {
                story.choosePathString(userDivertedPath);
            }

            evaluateStory();
        }
    }

    private void evaluateStory() throws Exception {
        int lineCounter = 0;

        while (story.canContinue()) {
            story.Continue();

            if (compiler != null) {
                compiler.retrieveDebugSourceForLatestContent();
            }

            if (jsonOutput) {
                SimpleJson.Writer writer = new SimpleJson.Writer();
                writer.writeObjectStart();
                writer.writeProperty("text", story.getCurrentText());
                writer.writeObjectEnd();
                System.out.println(writer.toString());
            } else {
                System.out.print(story.getCurrentText());
            }

            List<String> tags = story.getCurrentTags();
            if (!tags.isEmpty()) {
                if (jsonOutput) {
                    SimpleJson.Writer writer = new SimpleJson.Writer();
                    writer.writeObjectStart();
                    writer.writePropertyStart("tags");
                    writer.writeArrayStart();
                    for (String tag : tags) {
                        writer.write(tag);
                    }
                    writer.writeArrayEnd();
                    writer.writePropertyEnd();
                    writer.writeObjectEnd();
                    System.out.println(writer.toString());
                } else {
                    System.out.println("# tags: " + String.join(", ", tags));
                }
            }

            if (jsonOutput && (!errors.isEmpty() || !warnings.isEmpty())) {
                SimpleJson.Writer issueWriter = new SimpleJson.Writer();
                issueWriter.writeObjectStart();
                issueWriter.writePropertyStart("issues");
                issueWriter.writeArrayStart();
                for (String errorMsg : errors) {
                    issueWriter.write(errorMsg);
                }
                for (String warningMsg : warnings) {
                    issueWriter.write(warningMsg);
                }
                issueWriter.writeArrayEnd();
                issueWriter.writePropertyEnd();
                issueWriter.writeObjectEnd();
                System.out.println(issueWriter.toString());
            }

            if (!jsonOutput) {
                for (String errorMsg : errors) {
                    System.out.println(errorMsg);
                }
                for (String warningMsg : warnings) {
                    System.out.println(warningMsg);
                }
            }

            errors.clear();
            warnings.clear();

            lineCounter++;
            if (lineCounter > 1000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (story.getCurrentChoices().isEmpty() && keepOpenAfterStoryFinish) {
            if (jsonOutput) {
                System.out.println("{\"end\": true}");
            } else {
                System.out.println("--- End of story ---");
            }
        }
    }

    private void onStoryError(String msg, ErrorType type) {
        if (type == ErrorType.Error) {
            errors.add(msg);
        } else {
            warnings.add(msg);
        }
    }

    private Compiler.CommandLineInputResult readCommandLineInput(String userInput) throws Exception {
        InkParser inputParser = new InkParser(userInput);
        CommandLineInput inputResult = inputParser.CommandLineUserInput();
        Compiler.CommandLineInputResult result = new Compiler.CommandLineInputResult();

        if (inputResult.choiceInput != null) {
            result.choiceIdx = inputResult.choiceInput - 1;
            return result;
        }

        if (inputResult.isHelp) {
            result.output =
                    "Type a choice number, a divert (e.g. '-> myKnot'), an expression, or a variable assignment (e.g. 'x = 5')";
            return result;
        }

        if (inputResult.isExit) {
            result.requestsExit = true;
            return result;
        }

        if (compiler != null) {
            Compiler.CommandLineInputResult compilerResult = compiler.handleInput(inputResult);
            if (compilerResult != null) {
                return compilerResult;
            }
        }

        result.output = "Unexpected input. Type 'help' or a choice number.";
        return result;
    }
}

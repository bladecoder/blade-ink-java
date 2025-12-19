package com.bladecoder.ink.inklecate;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.compiler.IFileHandler;
import com.bladecoder.ink.compiler.Stats;
import com.bladecoder.ink.runtime.Error.ErrorHandler;
import com.bladecoder.ink.runtime.Error.ErrorType;
import com.bladecoder.ink.runtime.SimpleJson;
import com.bladecoder.ink.runtime.Story;
import com.bladecoder.ink.runtime.StoryException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CommandLineTool {
    private static class Options {
        boolean verbose;
        boolean playMode;
        boolean stats;
        boolean jsonOutput;
        String inputFile;
        String outputFile;
        boolean countAllVisits;
        boolean keepOpenAfterStoryFinish;
    }

    public static int ExitCodeError = 1;

    public static void main(String[] args) {
        new CommandLineTool(args);
    }

    private CommandLineTool(String[] args) {
        if (!processArguments(args)) {
            exitWithUsageInstructions();
        }

        if (opts.inputFile == null) {
            exitWithUsageInstructions();
        }

        String inputString;
        Path workingDirectory = Paths.get("").toAbsolutePath();
        Path inputBaseDir = null;
        Path outputBaseDir = workingDirectory;

        if (opts.outputFile == null) {
            opts.outputFile = changeExtension(opts.inputFile, ".ink.json");
        }
        boolean outputFileWasRelative = !Paths.get(opts.outputFile).isAbsolute();

        try {
            Path fullFilename = Paths.get(opts.inputFile);
            if (!fullFilename.isAbsolute()) {
                fullFilename = workingDirectory.resolve(opts.inputFile);
            }

            if (!Files.exists(fullFilename)) {
                fullFilename = workingDirectory.resolve(opts.inputFile);
            }

            Path parent = fullFilename.getParent();
            if (parent != null) {
                inputBaseDir = parent;
                outputBaseDir = parent;
            }

            if (outputFileWasRelative) {
                opts.outputFile = outputBaseDir.resolve(opts.outputFile).toString();
            }

            opts.inputFile = fullFilename.getFileName().toString();
            inputString = readFile(fullFilename.toString());
        } catch (IOException e) {
            System.out.println("Could not open file '" + opts.inputFile + "'");
            System.exit(ExitCodeError);
            return;
        }

        boolean inputIsJson = opts.inputFile.toLowerCase().endsWith(".json");
        if (inputIsJson && opts.stats) {
            System.out.println("Cannot show stats for .json, only for .ink");
            System.exit(ExitCodeError);
            return;
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Story parsedStory = null;
        Story story;
        Compiler compiler = null;

        if (!inputIsJson) {
            compiler = new Compiler(
                    inputString,
                    buildCompilerOptions(opts, opts.inputFile, pluginDirectories, this::onError, inputBaseDir));

            if (opts.stats) {
                parsedStory = compiler.parse();

                printAllMessages();

                Stats stats = Stats.generate(parsedStory);

                if (opts.jsonOutput) {
                    SimpleJson.Writer writer = new SimpleJson.Writer();
                    try {
                        writer.writeObjectStart();
                        writer.writePropertyStart("stats");
                        writer.writeObjectStart();
                        writer.writeProperty("words", stats.words);
                        writer.writeProperty("knots", stats.knots);
                        writer.writeProperty("stitches", stats.stitches);
                        writer.writeProperty("functions", stats.functions);
                        writer.writeProperty("choices", stats.choices);
                        writer.writeProperty("gathers", stats.gathers);
                        writer.writeProperty("diverts", stats.diverts);
                        writer.writeObjectEnd();
                        writer.writePropertyEnd();
                        writer.writeObjectEnd();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println(writer.toString());
                } else {
                    System.out.println("Words: " + stats.words);
                    System.out.println("Knots: " + stats.knots);
                    System.out.println("Stitches: " + stats.stitches);
                    System.out.println("Functions: " + stats.functions);
                    System.out.println("Choices: " + stats.choices);
                    System.out.println("Gathers: " + stats.gathers);
                    System.out.println("Diverts: " + stats.diverts);
                }

                return;
            } else {
                story = compiler.compile();
            }
        } else {
            try {
                story = new Story(inputString);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            opts.playMode = true;
        }

        boolean compileSuccess = story != null && errors.isEmpty();
        if (opts.jsonOutput) {
            if (compileSuccess) {
                System.out.println("{\"compile-success\": true}");
            } else {
                System.out.println("{\"compile-success\": false}");
            }
        }

        printAllMessages();

        if (!compileSuccess) {
            System.exit(ExitCodeError);
            return;
        }

        if (opts.playMode) {
            playing = true;
            story.setAllowExternalFunctionFallbacks(true);

            CommandLinePlayer player =
                    new CommandLinePlayer(story, false, compiler, opts.keepOpenAfterStoryFinish, opts.jsonOutput);

            try {
                player.begin();
            } catch (StoryException e) {
                if (e.getMessage() != null && e.getMessage().contains("Missing function binding")) {
                    onError(e.getMessage(), ErrorType.Error);
                    printAllMessages();
                } else {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                String storyPath = "<END>";
                String path = story.getState().getCurrentPathString();
                if (path != null) {
                    storyPath = path;
                }
                throw new RuntimeException(e.getMessage() + " (Internal story path: " + storyPath + ")", e);
            }
        } else {
            String jsonStr;
            try {
                jsonStr = story.toJson();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                writeFile(opts.outputFile, jsonStr);
                if (opts.jsonOutput) {
                    System.out.println("{\"export-complete\": true}");
                }
            } catch (IOException e) {
                System.out.println("Could not write to output file '" + opts.outputFile + "'");
                System.exit(ExitCodeError);
            }
        }
    }

    private void exitWithUsageInstructions() {
        System.out.println("Usage: inklecate <options> <ink file>\n"
                + "   -o <filename>:   Output file name\n"
                + "   -c:              Count all visits to knots, stitches and weave points, not\n"
                + "                    just those referenced by TURNS_SINCE and read counts.\n"
                + "   -p:              Play mode\n"
                + "   -j:              Output in JSON format (for communication with tools like Inky)\n"
                + "   -s:              Print stats about story including word count in JSON format\n"
                + "   -v:              Verbose mode - print compilation timings\n"
                + "   -k:              Keep inklecate running in play mode even after story is complete\n"
                + "   -x <directory>:              Import plugins for the compiler.");
        System.exit(ExitCodeError);
    }

    private Compiler.Options buildCompilerOptions(
            Options opts, String inputFile, List<String> pluginDirectories, ErrorHandler handler, Path inputBaseDir) {
        Compiler.Options options = new Compiler.Options();
        options.sourceFilename = inputFile;
        options.pluginDirectories = pluginDirectories;
        options.countAllVisits = opts.countAllVisits;
        options.errorHandler = handler;
        if (inputBaseDir != null) {
            options.fileHandler = new InklecateFileHandler(inputBaseDir);
        }
        return options;
    }

    private boolean processArguments(String[] args) {
        if (args.length < 1) {
            opts = null;
            return false;
        }

        opts = new Options();
        pluginDirectories = new ArrayList<>();

        boolean nextArgIsOutputFilename = false;
        boolean nextArgIsPluginDirectory = false;

        int argIdx = 0;
        for (String arg : args) {
            if (nextArgIsOutputFilename) {
                opts.outputFile = arg;
                nextArgIsOutputFilename = false;
            } else if (nextArgIsPluginDirectory) {
                pluginDirectories.add(arg);
                nextArgIsPluginDirectory = false;
            }

            if (arg.startsWith("-") && arg.length() > 1) {
                for (int i = 1; i < arg.length(); ++i) {
                    char argChar = arg.charAt(i);

                    switch (argChar) {
                        case 'p':
                            opts.playMode = true;
                            break;
                        case 'j':
                            opts.jsonOutput = true;
                            break;
                        case 'v':
                            opts.verbose = true;
                            break;
                        case 's':
                            opts.stats = true;
                            break;
                        case 'o':
                            nextArgIsOutputFilename = true;
                            break;
                        case 'c':
                            opts.countAllVisits = true;
                            break;
                        case 'x':
                            nextArgIsPluginDirectory = true;
                            break;
                        case 'k':
                            opts.keepOpenAfterStoryFinish = true;
                            break;
                        default:
                            System.out.println("Unsupported argument type: '" + argChar + "'");
                            break;
                    }
                }
            } else if (argIdx == args.length - 1) {
                opts.inputFile = arg;
            }

            argIdx++;
        }

        return true;
    }

    private void onError(String message, ErrorType errorType) {
        switch (errorType) {
            case Author:
                authorMessages.add(message);
                break;
            case Warning:
                warnings.add(message);
                break;
            case Error:
                errors.add(message);
                break;
            default:
                break;
        }

        if (playing) {
            printAllMessages();
        }
    }

    private void printAllMessages() {
        if (opts != null && opts.jsonOutput) {
            SimpleJson.Writer writer = new SimpleJson.Writer();
            try {
                writer.writeObjectStart();
                writer.writePropertyStart("issues");
                writer.writeArrayStart();
                for (String msg : authorMessages) {
                    writer.write(msg);
                }
                for (String msg : warnings) {
                    writer.write(msg);
                }
                for (String msg : errors) {
                    writer.write(msg);
                }
                writer.writeArrayEnd();
                writer.writePropertyEnd();
                writer.writeObjectEnd();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.print(writer.toString());
        } else {
            printIssues(authorMessages);
            printIssues(warnings);
            printIssues(errors);
        }

        authorMessages.clear();
        warnings.clear();
        errors.clear();
    }

    private void printIssues(List<String> messageList) {
        for (String msg : messageList) {
            System.out.println(msg);
        }
    }

    private static String readFile(String filename) throws IOException {
        Path path = Paths.get(filename);
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeFile(String filename, String contents) throws IOException {
        Path path = Paths.get(filename);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), filename);
        }
        Files.write(path, contents.getBytes(StandardCharsets.UTF_8));
    }

    private static String changeExtension(String filename, String extension) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return filename + extension;
        }
        return filename.substring(0, lastDot) + extension;
    }

    private static class InklecateFileHandler implements IFileHandler {
        private final Path baseDir;

        private InklecateFileHandler(Path baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public String resolveInkFilename(String includeName) {
            return baseDir.resolve(includeName).toString();
        }

        @Override
        public String loadInkFileContents(String fullFilename) throws IOException {
            byte[] bytes = Files.readAllBytes(Paths.get(fullFilename));
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private Options opts;
    private List<String> pluginDirectories;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> authorMessages = new ArrayList<>();
    private boolean playing;
}

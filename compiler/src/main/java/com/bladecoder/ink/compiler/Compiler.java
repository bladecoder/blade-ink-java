package com.bladecoder.ink.compiler;

import com.bladecoder.ink.compiler.ParsedHierarchy.ParsedObject;
import com.bladecoder.ink.compiler.ParsedHierarchy.Story;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.DebugMetadata;
import com.bladecoder.ink.runtime.Error.ErrorHandler;
import com.bladecoder.ink.runtime.Error.ErrorType;
import com.bladecoder.ink.runtime.RTObject;
import java.util.ArrayList;
import java.util.List;

public class Compiler {
    private static final ErrorHandler DEFAULT_ERROR_HANDLER = new ErrorHandler() {
        @Override
        public void error(String message, ErrorType errorType) {
            if (errorType == ErrorType.Error) {
                throw new RuntimeException(message);
            }
            if (message != null && !message.isEmpty()) {
                System.err.println(message);
            }
        }
    };

    public static class Options {
        public String sourceFilename;
        public List<String> pluginDirectories;
        public boolean countAllVisits;
        public ErrorHandler errorHandler;
        public IFileHandler fileHandler;
    }

    public Story getParsedStory() {
        return parsedStory;
    }

    public Compiler(String inkSource, Options options) {
        inputString = inkSource;
        if (options != null) {
            this.options = options;
        } else {
            this.options = new Options();
            this.options.countAllVisits = true;
        }
        if (this.options.errorHandler == null) {
            this.options.errorHandler = DEFAULT_ERROR_HANDLER;
        }
        if (this.options.pluginDirectories != null) {
            pluginManager = new PluginManager(this.options.pluginDirectories);
        }
    }

    public Compiler() {
        this(null, null);
    }

    public Story parse() {
        parser = new InkParser(inputString, options.sourceFilename, this::onParseError, options.fileHandler);
        parsedStory = parser.parse();
        return parsedStory;
    }

    public com.bladecoder.ink.runtime.Story compile() {
        if (pluginManager != null) {
            inputString = pluginManager.preParse(inputString);
        }

        parse();

        if (pluginManager != null) {
            parsedStory = pluginManager.postParse(parsedStory);
        }

        if (parsedStory != null && !hadParseError) {
            parsedStory.countAllVisits = options.countAllVisits;

            runtimeStory = parsedStory.exportRuntime(options.errorHandler);

            if (pluginManager != null) {
                runtimeStory = pluginManager.postExport(parsedStory, runtimeStory);
            }
        } else {
            runtimeStory = null;
        }

        return runtimeStory;
    }

    public String compile(String source) {
        Compiler compiler = new Compiler(source, null);
        com.bladecoder.ink.runtime.Story story = compiler.compile();
        if (story == null) {
            return "{}";
        }
        try {
            return story.toJson();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class CommandLineInputResult {
        public boolean requestsExit;
        public int choiceIdx = -1;
        public String divertedPath;
        public String output;
    }

    public CommandLineInputResult handleInput(CommandLineInput inputResult) {
        CommandLineInputResult result = new CommandLineInputResult();

        if (inputResult.debugSource != null) {
            int offset = inputResult.debugSource;
            DebugMetadata dm = debugMetadataForContentAtOffset(offset);
            if (dm != null) {
                result.output = "DebugSource: " + dm.toString();
            } else {
                result.output = "DebugSource: Unknown source";
            }
        } else if (inputResult.debugPathLookup != null) {
            String pathStr = inputResult.debugPathLookup;
            try {
                com.bladecoder.ink.runtime.SearchResult contentResult =
                        runtimeStory.contentAtPath(new com.bladecoder.ink.runtime.Path(pathStr));
                DebugMetadata dm = contentResult.obj.getDebugMetadata();
                if (dm != null) {
                    result.output = "DebugSource: " + dm.toString();
                } else {
                    result.output = "DebugSource: Unknown source";
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (inputResult.userImmediateModeStatement != null) {
            ParsedObject parsedObj = (ParsedObject) inputResult.userImmediateModeStatement;
            return executeImmediateStatement(parsedObj);
        } else {
            return null;
        }

        return result;
    }

    private CommandLineInputResult executeImmediateStatement(ParsedObject parsedObj) {
        CommandLineInputResult result = new CommandLineInputResult();

        if (parsedObj instanceof com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment) {
            com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment varAssign =
                    (com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment) parsedObj;
            if (varAssign.isNewTemporaryDeclaration) {
                parsedStory.tryAddNewVariableDeclaration(varAssign);
            }
        }

        parsedObj.parent = parsedStory;
        RTObject runtimeObj = parsedObj.getRuntimeObject();

        parsedObj.resolveReferences(parsedStory);

        if (!parsedStory.hadError()) {
            if (parsedObj instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Divert) {
                com.bladecoder.ink.compiler.ParsedHierarchy.Divert parsedDivert =
                        (com.bladecoder.ink.compiler.ParsedHierarchy.Divert) parsedObj;
                try {
                    result.divertedPath =
                            parsedDivert.runtimeDivert.getTargetPath().toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (parsedObj instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Expression
                    || parsedObj instanceof com.bladecoder.ink.compiler.ParsedHierarchy.VariableAssignment) {
                try {
                    RTObject evalResult = runtimeStory.evaluateExpression((Container) runtimeObj);
                    if (evalResult != null) {
                        result.output = evalResult.toString();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            parsedStory.resetError();
        }

        return result;
    }

    public void retrieveDebugSourceForLatestContent() {
        for (RTObject outputObj : runtimeStory.getState().getOutputStream()) {
            com.bladecoder.ink.runtime.StringValue textContent =
                    outputObj instanceof com.bladecoder.ink.runtime.StringValue
                            ? (com.bladecoder.ink.runtime.StringValue) outputObj
                            : null;
            if (textContent != null) {
                DebugSourceRange range = new DebugSourceRange();
                range.length = textContent.getValue().length();
                range.debugMetadata = textContent.getDebugMetadata();
                range.text = textContent.getValue();
                debugSourceRanges.add(range);
            }
        }
    }

    private DebugMetadata debugMetadataForContentAtOffset(int offset) {
        int currOffset = 0;

        DebugMetadata lastValidMetadata = null;
        for (DebugSourceRange range : debugSourceRanges) {
            if (range.debugMetadata != null) {
                lastValidMetadata = range.debugMetadata;
            }

            if (offset >= currOffset && offset < currOffset + range.length) {
                return lastValidMetadata;
            }

            currOffset += range.length;
        }

        return null;
    }

    public static class DebugSourceRange {
        public int length;
        public DebugMetadata debugMetadata;
        public String text;
    }

    private void onParseError(String message, ErrorType errorType) {
        if (errorType == ErrorType.Error) {
            hadParseError = true;
        }

        if (options.errorHandler != null) {
            options.errorHandler.error(message, errorType);
        } else {
            throw new RuntimeException(message);
        }
    }

    private String inputString;
    private Options options;

    private InkParser parser;
    private Story parsedStory;
    private com.bladecoder.ink.runtime.Story runtimeStory;

    private PluginManager pluginManager;

    private boolean hadParseError;

    private final List<DebugSourceRange> debugSourceRanges = new ArrayList<>();
}

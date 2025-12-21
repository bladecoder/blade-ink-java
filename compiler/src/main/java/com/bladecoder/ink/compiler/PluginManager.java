package com.bladecoder.ink.compiler;

import com.bladecoder.ink.compiler.ParsedHierarchy.Story;
import java.util.List;

public class PluginManager {
    public PluginManager(List<String> pluginDirectories) {
        this.pluginDirectories = pluginDirectories;
    }

    public String preParse(String input) {
        return input;
    }

    public Story postParse(Story story) {
        return story;
    }

    public com.bladecoder.ink.runtime.Story postExport(
            Story parsedStory, com.bladecoder.ink.runtime.Story runtimeStory) {
        return runtimeStory;
    }

    private final List<String> pluginDirectories;
}

package com.bladecoder.ink.compiler;

import com.bladecoder.ink.compiler.ParsedHierarchy.Choice;
import com.bladecoder.ink.compiler.ParsedHierarchy.Divert;
import com.bladecoder.ink.compiler.ParsedHierarchy.Gather;
import com.bladecoder.ink.compiler.ParsedHierarchy.Knot;
import com.bladecoder.ink.compiler.ParsedHierarchy.Stitch;
import com.bladecoder.ink.compiler.ParsedHierarchy.Story;
import com.bladecoder.ink.compiler.ParsedHierarchy.Text;
import java.util.List;

public class Stats {
    public int words;
    public int knots;
    public int stitches;
    public int functions;
    public int choices;
    public int gathers;
    public int diverts;

    public static Stats generate(Story story) {
        Stats stats = new Stats();

        List<Text> allText = story.findAll(Text.class);

        stats.words = 0;
        for (Text text : allText) {
            int wordsInThisStr = 0;
            boolean wasWhiteSpace = true;
            for (char c : text.getText().toCharArray()) {
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    wasWhiteSpace = true;
                } else if (wasWhiteSpace) {
                    wordsInThisStr++;
                    wasWhiteSpace = false;
                }
            }

            stats.words += wordsInThisStr;
        }

        List<Knot> knots = story.findAll(Knot.class);
        stats.knots = knots.size();

        stats.functions = 0;
        for (Knot knot : knots) {
            if (knot.isFunction()) {
                stats.functions++;
            }
        }

        List<Stitch> stitches = story.findAll(Stitch.class);
        stats.stitches = stitches.size();

        List<Choice> choices = story.findAll(Choice.class);
        stats.choices = choices.size();

        List<Gather> gathers = story.findAll(Gather.class, gather -> gather.getDebugMetadata() != null);
        stats.gathers = gathers.size();

        List<Divert> diverts = story.findAll(Divert.class);
        stats.diverts = diverts.size() - 1;

        return stats;
    }
}

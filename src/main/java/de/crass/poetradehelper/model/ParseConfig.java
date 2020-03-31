package de.crass.poetradehelper.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ParseConfig {

    private HashMap<String, String> placeholders = new HashMap<>();
    private List<PatternOutput> patternToOutput = new LinkedList<>();

    public ParseConfig() {
    }

    public int parseConfigLength() {
        return patternToOutput.size();
    }

    public HashMap<String, String> getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(HashMap<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    public List<PatternOutput> getPatternToOutput() {
        return patternToOutput;
    }

    public void setPatternToOutput(List<PatternOutput> patternToOutput) {
        this.patternToOutput = patternToOutput;
    }
}

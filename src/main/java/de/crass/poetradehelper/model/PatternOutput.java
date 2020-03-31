package de.crass.poetradehelper.model;

public class PatternOutput {
    private boolean active = true;
    private String pattern;
    private String output;
    private int probability = 100;

    public PatternOutput(){

    }

    public PatternOutput(String pattern, String output) {
        this.pattern = pattern;
        this.output = output;
    }

    public PatternOutput(String pattern, String output, int probability) {
        this.pattern = pattern;
        this.output = output;
        this.probability = probability;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getProbability() {
        return probability;
    }

    public void setProbability(int probability) {
        this.probability = probability;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof PatternOutput){
            PatternOutput other = (PatternOutput) obj;
            return other.pattern.equals(pattern);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }
}

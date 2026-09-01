package net.filemaid.ui.rename;

import net.filemaid.similarity.Match;

public class StringMatch<Value, Candidate>
extends Match<Value, Candidate> {
    private final String string;

    public StringMatch(String string, Value Value2, Candidate Candidate) {
        super(Value2, Candidate);
        this.string = string;
    }

    public String getStringValue() {
        return this.string;
    }

    @Override
    public String toString() {
        return this.string + " " + super.toString();
    }
}


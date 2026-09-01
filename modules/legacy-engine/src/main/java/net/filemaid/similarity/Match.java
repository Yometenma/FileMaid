package net.filemaid.similarity;

import java.util.Objects;

public class Match<Value, Candidate> {
    private final Value value;
    private final Candidate candidate;

    public Match(Value Value2, Candidate Candidate) {
        this.value = Value2;
        this.candidate = Candidate;
    }

    public Value getValue() {
        return this.value;
    }

    public Candidate getCandidate() {
        return this.candidate;
    }

    public boolean equals(Object object) {
        if (object instanceof Match) {
            Match match = (Match)object;
            return this.value == match.value && this.candidate == match.candidate;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.value, this.candidate);
    }

    public String toString() {
        return "[" + this.value + ", " + this.candidate + "]";
    }

    public static <Value, Candidate> Match<Value, Candidate> of(Value Value2, Candidate Candidate) {
        return new Match<Value, Candidate>(Value2, Candidate);
    }
}


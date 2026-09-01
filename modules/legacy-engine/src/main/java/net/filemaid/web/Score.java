package net.filemaid.web;

import java.util.Collections;
import java.util.Comparator;

public class Score<T> {
    public final T value;
    public final int score;

    public Score(T t, int n) {
        this.value = t;
        this.score = n;
    }

    public T getValue() {
        return this.value;
    }

    public int getScore() {
        return this.score;
    }

    public static <T> Score<T> of(T t, int n) {
        return new Score<T>(t, n);
    }

    public static <T> Comparator<Score<T>> descending() {
        return Collections.reverseOrder(Comparator.comparingInt(Score::getScore));
    }
}


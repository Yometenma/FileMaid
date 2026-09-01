package net.filemaid.ui.episodelist;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.SpinnerListModel;

class SeasonSpinnerModel
extends SpinnerListModel {
    public static final int ALL_SEASONS = 0;
    public static final int YEAR_SEASON_MIN_VALUE = 1990;
    public static final int YEAR_SEASON_MAX_VALUE = 2100;
    public static final int SEASON_MIN_VALUE = 1;
    public static final int SEASON_MAX_VALUE = 50;
    private Object valueBeforeLock = null;

    public static List<Integer> getSeasonValues() {
        IntStream intStream = IntStream.of(0);
        intStream = IntStream.concat(intStream, IntStream.range(1, 50));
        intStream = IntStream.concat(intStream, IntStream.range(1990, 2100));
        return intStream.boxed().collect(Collectors.toList());
    }

    public SeasonSpinnerModel() {
        super(SeasonSpinnerModel.getSeasonValues());
    }

    public int getSeason() {
        return (Integer)this.getValue();
    }

    public void spin(int n) {
        for (int i = 0; i < Math.abs(n); ++i) {
            this.setValue(i < 0 ? this.getPreviousValue() : this.getNextValue());
        }
    }

    public void lock(int n) {
        this.valueBeforeLock = this.getValue();
        this.setList(Collections.singletonList(0));
        this.setValue(0);
    }

    public void unlock() {
        this.setList(SeasonSpinnerModel.getSeasonValues());
        if (this.valueBeforeLock != null) {
            this.setValue(this.valueBeforeLock);
        }
        this.valueBeforeLock = null;
    }
}


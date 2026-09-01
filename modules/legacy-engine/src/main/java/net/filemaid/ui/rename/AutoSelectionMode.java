package net.filemaid.ui.rename;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

enum AutoSelectionMode {
    Auto,
    Skip,
    Input,
    Cancel;


    public static Set<AutoSelectionMode> newSet() {
        return Collections.synchronizedSet(EnumSet.noneOf(AutoSelectionMode.class));
    }

    public static Set<AutoSelectionMode> cancel() {
        return EnumSet.of(Skip, Cancel);
    }
}


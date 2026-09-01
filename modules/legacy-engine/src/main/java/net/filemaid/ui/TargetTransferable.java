package net.filemaid.ui;

import java.awt.datatransfer.Transferable;
import net.filemaid.ui.Mode;

public class TargetTransferable {
    private final Mode target;
    private final Transferable transferable;

    public TargetTransferable(Mode mode, Transferable transferable) {
        this.target = mode;
        this.transferable = transferable;
    }

    public Mode getTarget() {
        return this.target;
    }

    public Transferable getTransferable() {
        return this.transferable;
    }
}


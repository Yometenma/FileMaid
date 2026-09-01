package net.filemaid.util.ui;

import javax.swing.Icon;
import net.filemaid.util.ui.LabelProvider;

public class NullLabelProvider<T>
implements LabelProvider<T> {
    @Override
    public Icon getIcon(T t) {
        return null;
    }

    @Override
    public String getText(T t) {
        return t.toString();
    }
}


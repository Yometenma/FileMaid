package net.filemaid.util.ui;

import java.util.function.Function;
import javax.swing.Icon;

public interface LabelProvider<T> {
    public String getText(T var1);

    public Icon getIcon(T var1);

    public static <T> LabelProvider<T> via(final Function<T, String> function, final Function<T, Icon> function2) {
        return new LabelProvider<T>(){

            @Override
            public String getText(T t) {
                return (String)function.apply(t);
            }

            @Override
            public Icon getIcon(T t) {
                return (Icon)function2.apply(t);
            }
        };
    }
}


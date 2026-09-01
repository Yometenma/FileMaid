package net.filemaid.postprocess;

import java.io.File;

public interface Feedback {
    public void record(Type var1, Object var2, Object var3);

    default public void file(Object object, File file) {
        this.record(Type.FILE, object, file);
    }

    default public void info(Object object, Object object2) {
        this.record(Type.INFO, object, object2);
    }

    default public void warning(Object object, Object object2) {
        this.record(Type.WARNING, object, object2);
    }

    default public void trace(Object object, Object object2) {
        this.record(Type.TRACE, object, object2);
    }

    public boolean isCancelled();

    public void progress(Integer var1, Integer var2);

    public static enum Type {
        FILE,
        INFO,
        WARNING,
        TRACE;

    }
}


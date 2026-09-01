package net.filemaid.postprocess;

import java.util.function.Supplier;
import java.util.logging.Level;
import net.filemaid.Logging;
import net.filemaid.postprocess.Feedback;

public class FeedbackLogger
implements Feedback {
    private Object section;
    private Level level;

    public FeedbackLogger(Object object, Level level) {
        this.section = object;
        this.level = level;
    }

    public FeedbackLogger(Object object) {
        this(object, Level.INFO);
    }

    @Override
    public void record(Feedback.Type type, Object object, Object object2) {
        this.log(type, Logging.format("[%s] %s (%s)", this.section, object, object2));
    }

    public void log(Feedback.Type type, Supplier<String> supplier) {
        switch (type) {
            case FILE: 
            case INFO: {
                Logging.log.log(this.level, supplier);
                break;
            }
            case WARNING: {
                Logging.debug.warning(supplier);
                break;
            }
            default: {
                Logging.debug.finest(supplier);
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void progress(Integer n, Integer n2) {
    }
}


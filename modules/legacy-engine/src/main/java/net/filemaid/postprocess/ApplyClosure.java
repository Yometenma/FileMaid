package net.filemaid.postprocess;

import groovy.lang.Closure;
import java.io.File;
import java.util.stream.Stream;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.postprocess.ApplyStep;
import net.filemaid.postprocess.Feedback;

public class ApplyClosure
implements ApplyStep {
    private final Closure<?> closure;

    public ApplyClosure(Closure<?> closure) {
        this.closure = closure;
    }

    @Override
    public boolean accept(File file, Object object) {
        return true;
    }

    @Override
    public void apply(File file, File file2, Object object, Feedback feedback) {
        feedback.trace("Run Closure", file2);
        this.closure.setDelegate((Object)new MediaBindingBean(object, file2).getSelf());
        Object object2 = this.closure.call(Stream.of(file, file2, object).limit(this.closure.getMaximumNumberOfParameters()).toArray());
        feedback.trace(object2, file2);
    }

    public String toString() {
        return "CLOSURE";
    }
}


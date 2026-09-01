package net.filemaid.postprocess;

import groovy.lang.Closure;
import java.io.File;
import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import net.filemaid.GroovyEngine;
import net.filemaid.RenameAction;
import net.filemaid.format.AssociativeScriptObject;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.ApplyClosure;
import net.filemaid.postprocess.Feedback;
import net.filemaid.postprocess.FeedbackWriter;
import net.filemaid.postprocess.ScriptEngine;
import net.filemaid.similarity.Match;
import net.filemaid.util.FunctionList;

public class Script
implements Apply {
    public String id;
    public String name;
    public String code;
    private transient CompiledScript script;

    public Script(String string, String string2) {
        this(string, string, string2);
    }

    public Script(String string, String string2, String string3) {
        this.id = string;
        this.name = string2;
        this.code = string3;
    }

    public String getIdentifier() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }

    public synchronized CompiledScript compile() throws ScriptException {
        if (this.script == null) {
            this.script = ScriptEngine.getScriptEngine().compile(GroovyEngine.resolveScript(this.code));
        }
        return this.script;
    }

    public ScriptContext createScriptContext(Map<File, Match<File, ?>> map, RenameAction renameAction, Feedback feedback) {
        Model model = new Model(map);
        SimpleScriptContext simpleScriptContext = new SimpleScriptContext();
        simpleScriptContext.setAttribute("args", model.getTarget(), 100);
        simpleScriptContext.setAttribute("model", model, 100);
        simpleScriptContext.setAttribute("action", renameAction, 100);
        simpleScriptContext.setAttribute("log", feedback, 100);
        simpleScriptContext.setWriter(FeedbackWriter.newPrintWriter(string -> feedback.info(string, this.name)));
        simpleScriptContext.setErrorWriter(FeedbackWriter.newPrintWriter(string -> feedback.warning(string, this.name)));
        return simpleScriptContext;
    }

    @Override
    public void apply(Map<File, Match<File, ?>> map, RenameAction renameAction, Feedback feedback) {
        feedback.info("Run Script", this.name);
        try {
            ScriptContext scriptContext = this.createScriptContext(map, renameAction, feedback);
            Object object = this.compile().eval(scriptContext);
            if (object instanceof Closure) {
                ApplyClosure applyClosure = new ApplyClosure((Closure)object);
                applyClosure.apply(map, renameAction, feedback);
                return;
            }
            feedback.trace(object, this.name);
        }
        catch (ScriptException scriptException) {
            feedback.warning(GroovyEngine.sanitizeErrorMessage(scriptException), this.name);
        }
    }

    public String toString() {
        return "SCRIPT";
    }

    public static class Model
    extends AbstractList<AssociativeScriptObject> {
        private final Map.Entry<File, Match<File, ?>>[] map;
        private final AssociativeScriptObject[] model;

        public Model(Map<File, Match<File, ?>> map) {
            this.map = map.entrySet().toArray(new Map.Entry[0]);
            this.model = new AssociativeScriptObject[this.map.length];
        }

        public File getTarget(int n) {
            return this.map[n].getKey();
        }

        public File getSource(int n) {
            return this.map[n].getValue().getValue();
        }

        public Object getObject(int n) {
            return this.map[n].getValue().getCandidate();
        }

        @Override
        public AssociativeScriptObject get(int n) {
            if (this.model[n] == null) {
                this.model[n] = new MediaBindingBean(this.getObject(n), this.getTarget(n)).getSelf();
            }
            return this.model[n];
        }

        @Override
        public int size() {
            return this.model.length;
        }

        public List<File> getSource() {
            return new FunctionList<File>(this::getSource, this::size);
        }

        public List<File> getTarget() {
            return new FunctionList<File>(this::getTarget, this::size);
        }

        public List<Object> getObject() {
            return new FunctionList<Object>(this::getObject, this::size);
        }

        public List<Object> each(Closure<?> closure) {
            return IntStream.range(0, this.size()).mapToObj(n -> {
                closure.setDelegate((Object)this.get(n));
                switch (closure.getMaximumNumberOfParameters()) {
                    case 0: {
                        return closure.call();
                    }
                    case 1: {
                        return closure.call((Object)this.get(n));
                    }
                    case 2: {
                        return closure.call(new Object[]{this.getSource(n), this.getTarget(n)});
                    }
                    case 3: {
                        return closure.call(new Object[]{this.getSource(n), this.getTarget(n), this.getObject(n)});
                    }
                }
                throw new IllegalArgumentException("Unexpected number of parameters in closure: " + closure.getMaximumNumberOfParameters());
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
    }
}


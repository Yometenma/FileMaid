package net.filemaid.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.script.Bindings;
import javax.script.ScriptException;
import net.filemaid.Execute;
import net.filemaid.ExecuteException;
import net.filemaid.Logging;
import net.filemaid.cli.ArgumentProcessor;
import net.filemaid.format.ExpressionBindings;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.MediaBindingBean;

public class ExecCommand {
    private List<ExpressionFormat> template;
    private boolean parallel;
    private boolean distinct;
    private File directory;

    public ExecCommand(List<ExpressionFormat> list, boolean bl, boolean bl2, File file) {
        this.template = list;
        this.parallel = bl;
        this.distinct = bl2;
        this.directory = file;
    }

    public IntStream execute(Stream<MediaBindingBean> stream) {
        if (this.parallel) {
            return this.executeParallel(stream.map(ExpressionBindings::new));
        }
        return this.executeSequence(stream.map(ExpressionBindings::new));
    }

    private IntStream executeSequence(Stream<Bindings> stream) {
        Stream<List> stream2 = stream.map(bindings -> this.template.stream().map(expressionFormat -> this.getArgumentValue((ExpressionFormat)expressionFormat, (Bindings)bindings)).filter(Objects::nonNull).collect(Collectors.toList()));
        if (this.distinct) {
            return stream2.distinct().mapToInt(this::execute);
        }
        return stream2.mapToInt(this::execute);
    }

    private IntStream executeParallel(Stream<Bindings> stream) {
        List list = stream.collect(Collectors.toList());
        if (list.isEmpty()) {
            return IntStream.empty();
        }
        List list2 = this.template.stream().flatMap(expressionFormat -> {
            Stream<String> argumentStream = list.stream().map(bindings -> this.getArgumentValue((ExpressionFormat)expressionFormat, (Bindings)bindings)).filter(Objects::nonNull);
            if (expressionFormat.isConstant() && !expressionFormat.isEmpty()) {
                return argumentStream.limit(1L);
            }
            if (this.distinct) {
                return argumentStream.distinct();
            }
            return argumentStream;
        }).collect(Collectors.toList());
        return Stream.of(list2).mapToInt(this::execute);
    }

    private int execute(List<String> list) {
        if (list.isEmpty()) {
            return 0;
        }
        try {
            ArgumentProcessor argumentProcessor = ArgumentProcessor.parseCommand(list);
            if (argumentProcessor != null) {
                Logging.debug.finest(Logging.format("Run %s", list));
                return argumentProcessor.run();
            }
            Execute.system(list.get(0), list.subList(1, list.size()), this.directory, null);
            return 0;
        }
        catch (ExecuteException executeException) {
            Logging.log.warning(executeException::getMessage);
            return executeException.getExitCode();
        }
        catch (Exception exception) {
            Logging.log.warning(exception::getMessage);
            return 1;
        }
    }

    private String getArgumentValue(ExpressionFormat expressionFormat, Bindings bindings) {
        if (expressionFormat.isEmpty() && "{}".equals(expressionFormat.getExpression())) {
            return bindings.get("f").toString();
        }
        try {
            return expressionFormat.format(bindings);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(expressionFormat.getExpression(), exception));
            return null;
        }
    }

    public static ExecCommand parse(List<String> list, File file) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("args is empty");
        }
        Set<Flag> set = Flag.parse(list.get(list.size() - 1));
        if (!set.isEmpty()) {
            list = list.subList(0, list.size() - 1);
        }
        ArrayList<ExpressionFormat> arrayList = new ArrayList<ExpressionFormat>();
        for (String string : list) {
            try {
                arrayList.add(new ExpressionFormat(string));
            }
            catch (ScriptException scriptException) {
                throw new IllegalArgumentException("Invalid expression: " + string + ": " + scriptException.getMessage(), scriptException);
            }
        }
        return new ExecCommand(arrayList, set.contains((Object)Flag.Parallel), !set.contains((Object)Flag.Duplicate), file);
    }

    public static enum Flag {
        Parallel,
        Duplicate;


        public static Set<Flag> parse(String string) {
            switch (string) {
                case "+": {
                    return EnumSet.of(Parallel);
                }
                case "*": {
                    return EnumSet.of(Duplicate);
                }
                case "+*": {
                    return EnumSet.of(Parallel, Duplicate);
                }
            }
            return Collections.emptySet();
        }
    }
}


package net.filemaid.similarity;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.similarity.ICU;

public class DeltaEdit
implements Function<String, String> {
    private final Patch<String> diff;
    private final String prefix;

    private DeltaEdit(String string, String string2, boolean bl) {
        this.diff = DiffUtils.diff(this.tokenize(string), this.tokenize(string2), (boolean)bl);
        Chunk chunk = ((AbstractDelta)this.diff.getDeltas().get(0)).getSource();
        this.prefix = string.substring(0, chunk.getPosition() + ((List<String>)chunk.getLines()).stream().mapToInt(String::length).sum());
    }

    private List<String> tokenize(String string) {
        return ICU.tokenizeGraphemeClusters(string);
    }

    private String join(List<String> list) {
        return list.stream().collect(Collectors.joining());
    }

    public List<AbstractDelta<String>> getDelta() {
        return this.diff.getDeltas();
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String toString() {
        return this.diff.toString();
    }

    @Override
    public String apply(String string) {
        try {
            return this.join(this.diff.applyTo(this.tokenize(string)));
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause(exception));
            return null;
        }
    }

    public static DeltaEdit of(String string, String string2) {
        return string.equals(string2) ? null : new DeltaEdit(string, string2, false);
    }
}


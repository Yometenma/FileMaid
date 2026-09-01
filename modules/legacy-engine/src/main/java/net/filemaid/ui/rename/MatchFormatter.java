package net.filemaid.ui.rename;

import java.util.Map;
import net.filemaid.similarity.Match;

public interface MatchFormatter {
    public boolean canFormat(Match<?, ?> var1);

    public String preview(Match<?, ?> var1);

    public String format(Match<?, ?> var1, boolean var2, Map<?, ?> var3) throws Exception;
}


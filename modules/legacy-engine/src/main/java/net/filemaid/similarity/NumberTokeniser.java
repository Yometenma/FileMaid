package net.filemaid.similarity;

import java.util.List;
import java.util.stream.Collectors;
import net.filemaid.util.StringUtilities;
import org.simmetrics.tokenizers.AbstractTokenizer;

class NumberTokeniser
extends AbstractTokenizer {
    NumberTokeniser() {
    }

    public List<String> tokenizeToList(String string) {
        return StringUtilities.matchIntegers(string).stream().map(String::valueOf).collect(Collectors.toList());
    }
}


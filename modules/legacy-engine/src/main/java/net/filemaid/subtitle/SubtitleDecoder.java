package net.filemaid.subtitle;

import java.util.List;
import net.filemaid.subtitle.SubtitleElement;

public interface SubtitleDecoder {
    public List<SubtitleElement> decode(String var1);
}


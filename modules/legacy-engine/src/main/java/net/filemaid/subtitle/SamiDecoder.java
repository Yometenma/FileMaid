package net.filemaid.subtitle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.similarity.Normalization;
import net.filemaid.subtitle.SubtitleElement;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SamiDecoder {
    private CharSequence content;
    private Matcher matcher;

    public SamiDecoder(CharSequence charSequence) {
        this.content = charSequence;
        this.matcher = Pattern.compile("<SYNC(.*?)>", 2).matcher(charSequence);
    }

    public List<SubtitleElement> decode() {
        Object object;
        ArrayList<SubtitleElement> arrayList = new ArrayList<SubtitleElement>(2048);
        long l = -1L;
        long l2 = -1L;
        int n = -1;
        while (this.matcher.find()) {
            object = Jsoup.parseBodyFragment((String)this.matcher.group()).select("sync").first();
            long l3 = this.getLongAttribute((Element)object, "start");
            long l4 = this.getLongAttribute((Element)object, "end");
            if (n > 0) {
                SubtitleElement subtitleElement;
                if (l2 < 0L) {
                    l2 = l3;
                }
                if ((subtitleElement = this.getSubtitle(l, l2, this.content.subSequence(n, this.matcher.start()))) != null) {
                    arrayList.add(subtitleElement);
                }
            }
            if (l3 < 0L) continue;
            l = l3;
            l2 = l4;
            n = this.matcher.end();
        }
        if (n > 0) {
            if (l2 < 0L) {
                l2 = l + 2000L;
            }
            if ((object = this.getSubtitle(l, l2, this.content.subSequence(n, this.content.length()))) != null) {
                arrayList.add((SubtitleElement)object);
            }
        }
        return arrayList;
    }

    private SubtitleElement getSubtitle(long l, long l2, CharSequence charSequence) {
        Document document;
        String string2;
        if (l >= 0L && l2 >= 0L && (string2 = (document = Jsoup.parseBodyFragment((String)charSequence.toString())).select("p").stream().map(element -> element.text()).map(string -> Normalization.replaceSpace(string, " ")).filter(string -> string.length() > 0).collect(Collectors.joining("\n")).trim()).length() > 0) {
            return new SubtitleElement(l, l2, string2);
        }
        return null;
    }

    private long getLongAttribute(Element element, String string) {
        String string2 = element.attr(string);
        if (string2 != null && !string2.isEmpty()) {
            try {
                return Long.parseLong(string2);
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.message(this.getClass(), string2, exception));
            }
        }
        return -1L;
    }

    public static List<SubtitleElement> decode(String string) {
        return new SamiDecoder(string).decode();
    }
}


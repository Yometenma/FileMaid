package net.filemaid.postprocess;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.filemaid.MediaTypes;
import net.filemaid.MetaAttributeView;
import net.filemaid.Resource;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.postprocess.ApplyMetadata;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.XmlUtilities;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;

public class SetFinderTags
implements ApplyMetadata {
    public static final String XATTR_ITEM_USER_TAGS = "com.apple.metadata:_kMDItemUserTags";
    private final Resource<ExpressionFormat> format = Resource.lazy(() -> new ExpressionFormat(System.getProperty("apply.finder.tags", this.getDefaultFormatExpression())));

    @Override
    public boolean accept(File file, Object object) {
        return MediaTypes.VIDEO_FILES.accept(file);
    }

    @Override
    public void apply(File file, File file2, Movie movie, Feedback feedback) throws Exception {
        List<String> list = this.tags(file2, movie, feedback);
        this.setItemUserTags(file2, list, feedback);
    }

    @Override
    public void apply(File file, File file2, Episode episode, Feedback feedback) throws Exception {
        List<String> list = this.tags(file2, episode, feedback);
        this.setItemUserTags(file2, list, feedback);
    }

    private List<String> tags(File file, Object object, Feedback feedback) {
        try {
            String string2 = this.format.get().format(new MediaBindingBean(object, file));
            return RegularExpressions.NEWLINE.splitAsStream(string2).flatMap(RegularExpressions.PIPE::splitAsStream).map(String::trim).filter(string -> !string.isEmpty()).collect(Collectors.toList());
        }
        catch (Exception exception) {
            feedback.warning(exception, file);
            return Collections.emptyList();
        }
    }

    private void setItemUserTags(File file, List<String> list, Feedback feedback) throws Exception {
        if (list.isEmpty()) {
            return;
        }
        MetaAttributeView metaAttributeView = new MetaAttributeView(file);
        String string = this.plist(list);
        feedback.info(list, file);
        feedback.trace(string, file);
        metaAttributeView.put(XATTR_ITEM_USER_TAGS, string);
    }

    private String plist(List<String> list) throws Exception {
        Element element = XmlUtilities.root("plist");
        XmlUtilities.attr(element, "version", "1.0");
        Element element2 = XmlUtilities.element(element, "array");
        for (String string : list) {
            XmlUtilities.text(element2, "string", string);
        }
        return XmlUtilities.writeDocument(element);
    }

    private String getDefaultFormatExpression() {
        try {
            return IOUtils.toString((URL)SetFinderTags.class.getResource("SetFinderTags.format"), (Charset)StandardCharsets.UTF_8);
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}


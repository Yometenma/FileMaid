package net.filemaid.ui.subtitle;

import java.awt.Component;
import java.awt.LayoutManager;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.filemaid.subtitle.SubtitleFormat;
import net.miginfocom.swing.MigLayout;

public class SubtitleFileChooser
extends JFileChooser {
    protected final JComboBox format = new JComboBox();
    protected final JComboBox encoding = new JComboBox();
    protected final JSpinner offset = new JSpinner(new SpinnerNumberModel(0, -14400000, 14400000, 100));

    public SubtitleFileChooser() {
        this.setAccessory(this.createAcessory());
        this.setDefaultOptions();
    }

    protected void setDefaultOptions() {
        this.setFormatOptions(Collections.singleton(SubtitleFormat.SubRip));
        LinkedHashSet<Charset> linkedHashSet = new LinkedHashSet<Charset>(2);
        linkedHashSet.add(StandardCharsets.UTF_8);
        linkedHashSet.add(Charset.defaultCharset());
        this.setEncodingOptions(linkedHashSet);
    }

    protected JComponent createAcessory() {
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("nogrid"));
        jPanel.add((Component)new JLabel("Encoding:"), "wrap rel");
        jPanel.add((Component)this.encoding, "sg w, wrap para");
        jPanel.add((Component)new JLabel("Format:"), "wrap rel");
        jPanel.add((Component)this.format, "sg w, wrap para");
        jPanel.add((Component)new JLabel("Timing Offset:"), "wrap rel");
        jPanel.add((Component)this.offset, "wmax 50px");
        jPanel.add(new JLabel("ms"));
        return jPanel;
    }

    public void setEncodingOptions(Set<Charset> set) {
        this.encoding.setModel(new DefaultComboBoxModel<Object>(set.toArray()));
    }

    public Charset getSelectedEncoding() {
        return (Charset)this.encoding.getSelectedItem();
    }

    public void setFormatOptions(Set<SubtitleFormat> set) {
        this.format.setModel(new DefaultComboBoxModel<Object>(set.toArray()));
    }

    public SubtitleFormat getSelectedFormat() {
        return (SubtitleFormat)((Object)this.format.getSelectedItem());
    }

    public long getTimingOffset() {
        return ((Integer)this.offset.getValue()).intValue();
    }
}


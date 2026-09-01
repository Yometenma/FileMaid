package net.filemaid.ui.sfv;

import java.awt.Component;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import net.filemaid.ui.sfv.ChecksumRow;

class HighlightPatternCellRenderer
extends DefaultTableCellRenderer {
    private final Pattern pattern;

    public HighlightPatternCellRenderer(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
        super.getTableCellRendererComponent(jTable, object, bl, false, n, n2);
        boolean bl3 = EnumSet.of(ChecksumRow.State.ERROR, ChecksumRow.State.WARNING).contains(jTable.getValueAt(n, 0));
        Matcher matcher = this.pattern.matcher(String.valueOf(object));
        StringBuffer stringBuffer = new StringBuffer("<html><nobr>");
        while (matcher.find()) {
            matcher.appendReplacement(stringBuffer, this.createReplacement(bl ? null : (bl3 ? "red" : "#009900"), "smaller", bl3 ? "bold" : null));
        }
        matcher.appendTail(stringBuffer).append("</nobr></html>");
        this.setText(stringBuffer.toString());
        return this;
    }

    protected String createReplacement(String string, String string2, String string3) {
        StringBuilder stringBuilder = new StringBuilder(60);
        stringBuilder.append("<span style='");
        if (string != null) {
            stringBuilder.append("color:").append(string).append(';');
        }
        if (string2 != null) {
            stringBuilder.append("font-size:").append(string2).append(';');
        }
        if (string3 != null) {
            stringBuilder.append("font-weight:").append(string3).append(';');
        }
        return stringBuilder.append("'>$0</span>").toString();
    }
}


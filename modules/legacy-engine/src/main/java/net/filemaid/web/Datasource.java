package net.filemaid.web;

import javax.swing.Icon;

public interface Datasource {
    public String getIdentifier();

    public Icon getIcon();

    default public String getName() {
        return this.getIdentifier();
    }
}


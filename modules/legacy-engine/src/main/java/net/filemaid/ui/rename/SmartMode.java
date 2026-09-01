package net.filemaid.ui.rename;

import java.util.Locale;
import javax.swing.Icon;
import net.filemaid.ResourceManager;
import net.filemaid.ui.rename.AttributesMatcher;
import net.filemaid.ui.rename.AutoCompleteMatcher;
import net.filemaid.ui.rename.AutoDetectMatcher;
import net.filemaid.web.Datasource;

public enum SmartMode implements Datasource
{
    Automatic,
    Attributes;


    @Override
    public String getIdentifier() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public Icon getIcon() {
        switch (this) {
            case Automatic: {
                return ResourceManager.getIcon("action.auto");
            }
        }
        return ResourceManager.getIcon("action.properties");
    }

    public AutoCompleteMatcher newMatcher() {
        switch (this) {
            case Automatic: {
                return new AutoDetectMatcher();
            }
        }
        return new AttributesMatcher();
    }
}


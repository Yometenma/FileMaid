package net.filemaid.ui;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import net.filemaid.HistorySpooler;
import net.filemaid.Logging;
import net.filemaid.Resource;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.UserData;
import net.filemaid.UserInteraction;
import net.filemaid.util.PreferencesMap;
import net.filemaid.util.StringUtilities;

public enum SupportDialog {
    AppStoreReview{

        @Override
        String getMessage(int n) {
            return String.format("<html><p style='font-size:16pt; font-weight:bold'>Thank you for using FileBot!</p><br><p>It has taken thousands of hours to develop this application. If you enjoy using it,<br>please consider writing a nice review on the %s.<p><p style='font-size:14pt; font-weight:bold'>You've renamed %,d files.</p><br><html>", this.getAppStoreName(), n);
        }

        @Override
        String[] getActions(boolean bl) {
            if (bl) {
                return new String[]{"Write a Review! :)", "Nope! Maybe next time."};
            }
            return new String[]{"Update my Review! :)", "Nope! Not this time."};
        }

        @Override
        Icon getIcon() {
            return ResourceManager.getIcon("window.icon.large");
        }

        @Override
        String getTitle() {
            return "Please write a Review";
        }

        @Override
        public boolean feelingLucky(Resource<Integer> resource, Resource<Integer> resource2, int n, int n2, int n3) throws Exception {
            if (n <= n2) {
                return false;
            }
            if (resource.get() < 50 || resource2.get() < 5000) {
                return false;
            }
            if (Math.random() <= 0.777) {
                return false;
            }
            return super.feelingLucky(resource, resource2, n, n2, n3);
        }

        String getAppStoreName() {
            if (Settings.isMacApp()) {
                return "Mac App Store";
            }
            if (Settings.isWindowsApp()) {
                return "Microsoft Store";
            }
            return null;
        }

        @Override
        String getURI() {
            if (Settings.isMacApp()) {
                return Settings.getApplicationProperty("link.review.mas");
            }
            if (Settings.isWindowsApp()) {
                return Settings.getApplicationProperty("link.review.mws");
            }
            return null;
        }
    };


    public boolean feelingLucky(Resource<Integer> resource, Resource<Integer> resource2, int n, int n2, int n3) throws Exception {
        if ((double)resource.get().intValue() >= 2000.0 * Math.pow(2.0, n3)) {
            return true;
        }
        return (double)resource2.get().intValue() >= 2000.0 * Math.pow(5.0, n3);
    }

    public boolean show(Resource<Integer> resource, boolean bl) throws Exception {
        String string = this.getMessage(resource.get());
        Object[] objectArray = this.getActions(bl);
        JOptionPane jOptionPane = new JOptionPane(string, 1, 0, this.getIcon(), objectArray, objectArray[0]);
        jOptionPane.createDialog(null, this.getTitle()).setVisible(true);
        if (jOptionPane.getValue() == objectArray[0]) {
            UserInteraction.browse(this.getURI());
        }
        return true;
    }

    abstract String getMessage(int var1);

    abstract String[] getActions(boolean var1);

    abstract Icon getIcon();

    abstract String getTitle();

    abstract String getURI();

    public void maybeShow() {
        try {
            PreferencesMap.PreferencesEntry<String> preferencesEntry = UserData.forPackage(SupportDialog.class).entry("support.revision");
            List<Integer> list = StringUtilities.matchIntegers(preferencesEntry.getValue());
            int n = list.stream().max(Integer::compare).orElse(0);
            int n2 = Settings.getApplicationRevisionNumber();
            Resource.Memoized<Integer> memoized = Resource.lazy(HistorySpooler.HISTORY::getSessionCount);
            Resource.Memoized<Integer> memoized2 = Resource.lazy(HistorySpooler.HISTORY::getTotalCount);
            if (this.feelingLucky(memoized, memoized2, n2, n, list.size()) && this.show(memoized2, list.isEmpty())) {
                list = Stream.concat(list.stream(), Stream.of(Integer.valueOf(n2))).sorted().distinct().collect(Collectors.toList());
                preferencesEntry.setValue(list.toString());
            }
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
    }
}


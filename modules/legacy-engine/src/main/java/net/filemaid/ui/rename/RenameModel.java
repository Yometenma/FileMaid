package net.filemaid.ui.rename;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.SwingUtilities;
import net.filemaid.media.MetaAttributes;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.ExpressionFormatter;
import net.filemaid.ui.rename.FormattedFuture;
import net.filemaid.ui.rename.MatchFormatter;
import net.filemaid.ui.rename.MatchFormatterType;
import net.filemaid.ui.rename.MatchModel;
import net.filemaid.ui.rename.StringMatch;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.util.RegularExpressions;
import net.filemaid.vfs.SimpleFileInfo;

public class RenameModel
extends MatchModel<Object, File> {
    private final FormattedFutureEventList names = new FormattedFutureEventList(this.values());
    private final Map<MatchFormatterType, MatchFormatter> formatters = new EnumMap<MatchFormatterType, MatchFormatter>(MatchFormatterType.class);
    private boolean preserveExtension = true;

    public EventList<FormattedFuture> names() {
        return this.names;
    }

    public EventList<File> files() {
        return this.candidates();
    }

    public boolean preserveExtension() {
        return this.preserveExtension;
    }

    public void setPreserveExtension(boolean bl) {
        this.preserveExtension = bl;
        this.names.refresh();
    }

    public List<Match<File, Object>> getMatchModel() {
        ArrayList<Match<File, Object>> arrayList = new ArrayList<Match<File, Object>>(this.names.size());
        Iterator iterator = this.names.iterator();
        while (iterator.hasNext()) {
            FormattedFuture formattedFuture = (FormattedFuture)iterator.next();
            if (!formattedFuture.hasComplement()) continue;
            File file = ReadOnlyFile.asRegularFile(formattedFuture.getMatch().getCandidate());
            Object object = formattedFuture.getMatch().getValue();
            arrayList.add(Match.of(file, object));
        }
        return arrayList;
    }

    public Map<File, File> getRenameMap() {
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>(this.names.size());
        Iterator iterator = this.names.iterator();
        while (iterator.hasNext()) {
            FormattedFuture formattedFuture = (FormattedFuture)iterator.next();
            if (!formattedFuture.hasComplement()) continue;
            try {
                File file = formattedFuture.getMatch().getCandidate();
                File file2 = (File)formattedFuture.get(0L, TimeUnit.MILLISECONDS);
                if (linkedHashMap.put(file = ReadOnlyFile.asRegularFile(file), file2 = ReadOnlyFile.asRegularFile(file2)) == null) continue;
                throw new IllegalStateException("Duplicate source file: " + file.getName());
            }
            catch (ExecutionException executionException) {
                throw new IllegalStateException("\"" + formattedFuture.preview() + "\" could not be formatted: " + executionException.getCause().getMessage());
            }
            catch (TimeoutException timeoutException) {
                throw new IllegalStateException("\"" + formattedFuture.preview() + "\" has not been formatted yet. Please wait a little while longer.");
            }
            catch (InterruptedException interruptedException) {
                throw new CancellationException();
            }
        }
        return linkedHashMap;
    }

    public void useFormatter(MatchFormatterType matchFormatterType, MatchFormatter matchFormatter) {
        MatchFormatter matchFormatter2 = this.formatters.get(matchFormatterType);
        if (matchFormatter2 == matchFormatter || matchFormatter2 != null && matchFormatter2.equals(matchFormatter)) {
            return;
        }
        if (matchFormatter == null) {
            this.formatters.remove(matchFormatterType);
        } else {
            this.formatters.put(matchFormatterType, matchFormatter);
        }
        this.names.refresh();
    }

    public String getFormatExpression(Match<Object, File> match) {
        for (MatchFormatter matchFormatter : this.formatters.values()) {
            if (!(matchFormatter instanceof ExpressionFormatter) || !matchFormatter.canFormat(match)) continue;
            return ((ExpressionFormatter)matchFormatter).getFormatExpression();
        }
        return null;
    }

    private MatchFormatter getFormatter(Match<Object, File> match) {
        if (MatchFormatterType.INPUT.canFormat(match)) {
            return MatchFormatterType.INPUT;
        }
        for (MatchFormatter matchFormatter : this.formatters.values()) {
            if (!matchFormatter.canFormat(match)) continue;
            return matchFormatter;
        }
        return MatchFormatterType.OBJECT;
    }

    public void fill(int n, Object object) {
        Object object2 = null;
        do {
            object2 = this.set(n++, object);
        } while (this.hasComplement(n) && object2.equals(((Match)this.matches().get(n)).getValue()));
    }

    public Object set(int n, Object object) {
        Match match = (Match)this.matches().get(n);
        this.matches().set(n, Match.of(object, (File)match.getCandidate()));
        return match.getValue();
    }

    public Object set(int n, String string, Object object) {
        Match match = (Match)this.matches().get(n);
        this.matches().set(n, new StringMatch<Object, File>(string, object, (File)match.getCandidate()));
        return match.getValue();
    }

    public void fill(int n, Function<String, String> function, Predicate<FormattedFuture> predicate) {
        String string;
        FormattedFuture formattedFuture;
        for (int i = n; i < this.size() && (formattedFuture = (FormattedFuture)this.names().get(i)) != null && formattedFuture.isDone() && predicate.test(formattedFuture) && (string = function.apply(formattedFuture.toString())) != null && !string.isEmpty(); ++i) {
            this.set(i, string, formattedFuture.getMatch().getValue());
        }
    }

    public Map<File, Object> getMatchContext(Match<Object, File> match) {
        if (match.getValue() == null || match.getCandidate() == null) {
            return Collections.emptyMap();
        }
        return new AbstractMap<File, Object>(){

            @Override
            public Set<Map.Entry<File, Object>> entrySet() {
                LinkedHashSet<Map.Entry<File, Object>> linkedHashSet = new LinkedHashSet<Map.Entry<File, Object>>();
                for (Match match : RenameModel.this.matches()) {
                    if (match.getValue() == null || match.getCandidate() == null) continue;
                    linkedHashSet.add(new AbstractMap.SimpleImmutableEntry((File)match.getCandidate(), match.getValue()));
                }
                return linkedHashSet;
            }
        };
    }

    public String exportContent() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Match match : this.matches()) {
            File file = (File)match.getCandidate();
            Object Value2 = match.getValue();
            stringBuilder.append(file == null ? "" : file).append('\t');
            stringBuilder.append(Value2 == null ? "" : MetaAttributes.toJson(Value2 instanceof File ? new SimpleFileInfo((File)Value2) : Value2, false)).append('\n');
        }
        return stringBuilder.toString();
    }

    public void importContent(String string) {
        if (string == null || string.isEmpty()) {
            return;
        }
        this.clear();
        for (String string2 : RegularExpressions.NEWLINE.split(string)) {
            File file;
            String[] stringArray = RegularExpressions.TAB.split(string2);
            if (stringArray.length != 2 || !(file = new File(stringArray[0].trim())).isAbsolute()) continue;
            Object object = MetaAttributes.toObject(stringArray[1].trim());
            this.matches().add(Match.of(object, file));
        }
    }

    private class FormattedFutureEventList
    extends TransformedList<Object, FormattedFuture> {
        private final List<FormattedFuture> futures;

        public FormattedFutureEventList(EventList<Object> eventList) {
            super(eventList);
            this.futures = new ArrayList<FormattedFuture>();
            this.source.addListEventListener((ListEventListener)this);
        }

        public FormattedFuture get(int n) {
            return this.futures.get(n);
        }

        public int size() {
            return this.futures.size();
        }

        protected boolean isWritable() {
            return false;
        }

        public void add(int n, FormattedFuture formattedFuture) {
            this.source.add(n, formattedFuture.getMatch().getValue());
        }

        public FormattedFuture set(int n, FormattedFuture formattedFuture) {
            FormattedFuture formattedFuture2 = this.get(n);
            this.source.set(n, formattedFuture.getMatch().getValue());
            return formattedFuture2;
        }

        public FormattedFuture remove(int n) {
            this.source.remove(n);
            return null;
        }

        public void listChanged(ListEvent<Object> listEvent) {
            this.updates.beginEvent(true);
            while (listEvent.next()) {
                Object object;
                int n = listEvent.getIndex();
                int n2 = listEvent.getType();
                if (n2 == 2 || n2 == 1) {
                    object = RenameModel.this.getMatch(n);
                    FormattedFuture formattedFuture = new FormattedFuture((Match<Object, File>)object, !RenameModel.this.preserveExtension, RenameModel.this.getFormatter((Match<Object, File>)object), RenameModel.this.getMatchContext((Match<Object, File>)object));
                    if (n2 == 2) {
                        this.futures.add(n, formattedFuture);
                        this.updates.elementInserted(n, formattedFuture);
                    } else if (n2 == 1) {
                        FormattedFuture formattedFuture2 = this.futures.set(n, formattedFuture);
                        this.cancel(formattedFuture2);
                    }
                    this.submit(n, formattedFuture);
                    continue;
                }
                if (n2 != 0) continue;
                object = this.futures.remove(n);
                this.cancel((FormattedFuture)object);
                this.updates.elementDeleted(n, (FormattedFuture)object);
            }
            this.updates.commitEvent();
        }

        public void refresh() {
            if (this.isEmpty()) {
                return;
            }
            this.updates.beginEvent(true);
            for (int i = 0; i < this.size(); ++i) {
                FormattedFuture formattedFuture = this.futures.get(i);
                Match<Object, File> match = formattedFuture.getMatch();
                FormattedFuture formattedFuture2 = new FormattedFuture(match, !RenameModel.this.preserveExtension, RenameModel.this.getFormatter(match), RenameModel.this.getMatchContext(match));
                this.futures.set(i, formattedFuture2);
                this.cancel(formattedFuture);
                this.submit(i, formattedFuture2);
                this.updates.elementUpdated(i, formattedFuture, formattedFuture2);
            }
            this.updates.commitEvent();
        }

        private void submit(int n, FormattedFuture formattedFuture) {
            formattedFuture.start().thenRunAsync(() -> this.publish(n, formattedFuture), SwingUtilities::invokeLater);
        }

        private void cancel(FormattedFuture formattedFuture) {
            formattedFuture.cancel(true);
        }

        private void publish(int n, FormattedFuture formattedFuture) {
            if (formattedFuture.isCancelled()) {
                return;
            }
            if (n >= this.futures.size() || formattedFuture != this.futures.get(n)) {
                n = this.futures.indexOf(formattedFuture);
            }
            if (n >= 0 && n < this.size()) {
                this.updates.beginEvent(true);
                this.updates.elementUpdated(n, formattedFuture, formattedFuture);
                this.updates.commitEvent();
            }
        }
    }
}


package net.filemaid.ui.rename;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import net.filemaid.Logging;
import net.filemaid.Parallelism;
import net.filemaid.similarity.EpisodeMetrics;
import net.filemaid.similarity.Match;
import net.filemaid.similarity.Matcher;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.ui.rename.RenameModel;
import net.filemaid.util.ui.GlassProgressMonitor;
import net.filemaid.util.ui.SwingUI;

class MatchAction
extends AbstractAction {
    private final RenameModel model;

    public MatchAction(RenameModel renameModel) {
        this.model = renameModel;
        this.setMatchMode(MatchMode.Opportunistic);
    }

    public void setMatchMode(MatchMode matchMode) {
        this.putValue("Name", "Match");
        this.putValue("SmallIcon", matchMode.getIcon());
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (this.model.names().isEmpty() || this.model.files().isEmpty()) {
            return;
        }
        Window window = SwingUI.getWindow(actionEvent.getSource());
        SwingUI.withWaitCursor((Object)window, () -> {
            MatchWorker matchWorker = new MatchWorker((Collection<Object>)this.model.values(), (Collection<File>)this.model.candidates(), SwingUI.isShiftOrAltDown(actionEvent));
            List list = GlassProgressMonitor.runTask(matchWorker, window);
            if (this.sameMatches(list, (List<Match<Object, File>>)this.model.matches())) {
                Logging.log.info("Files and Names are already optimally aligned.");
                return;
            }
            this.model.clear();
            this.model.addAll(list);
            this.model.addAll(matchWorker.remainingValues(), matchWorker.remainingCandidates());
        });
    }

    private boolean sameMatches(List<Match<Object, File>> list, List<Match<Object, File>> list2) {
        for (int i = 0; i < list.size() && i < list2.size(); ++i) {
            if (list.get(i).equals(list2.get(i))) continue;
            return false;
        }
        return true;
    }

    private static class MatchWorker
    implements GlassProgressMonitor.ProgressWorker<List<Match<Object, File>>> {
        private final Set<Object> values;
        private final Set<File> candidates;
        private final boolean strict;

        public MatchWorker(Collection<Object> collection, Collection<File> collection2, boolean bl) {
            this.values = new LinkedHashSet<Object>(collection);
            this.candidates = new LinkedHashSet<File>(collection2);
            this.strict = bl;
        }

        public Set<Object> remainingValues() {
            return this.values;
        }

        public Set<File> remainingCandidates() {
            return this.candidates;
        }

        @Override
        public String getName() {
            return "Finding optimal alignment...";
        }

        @Override
        public Icon getIcon() {
            return this.strict ? MatchMode.Strict.getIcon() : MatchMode.Opportunistic.getIcon();
        }

        @Override
        public String getDescription() {
            return "Preparing...";
        }

        @Override
        public boolean isIndeterminate() {
            return true;
        }

        @Override
        public List<Match<Object, File>> call(Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
            return this.strict ? this.matchNtoM(consumer2, biConsumer, supplier) : this.match1toN(consumer2, biConsumer, supplier);
        }

        private List<Match<Object, File>> matchNtoM(Consumer<String> consumer, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) {
            int n = this.candidates.size() * this.values.size();
            consumer.accept("Checking " + n + " combinations...");
            Matcher<File, Object> matcher = new Matcher<File, Object>(this.candidates, this.values, false, new EpisodeMetrics().matchFileSequence());
            ArrayList<Match<Object, File>> arrayList = new ArrayList<Match<Object, File>>();
            matcher.match().forEach(match -> {
                arrayList.add(Match.of(match.getCandidate(), (File)match.getValue()));
                this.values.remove(match.getCandidate());
                this.candidates.remove(match.getValue());
            });
            return arrayList;
        }

        private List<Match<Object, File>> match1toN(Consumer<String> consumer, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
            EpisodeMetrics episodeMetrics = new EpisodeMetrics();
            List<Matcher<File, Object>> list = this.candidates.stream().map(file -> new Matcher<File, Object>(Collections.singleton(file), this.values, false, episodeMetrics.matchFileSequence())).collect(Collectors.toList());
            ArrayList<Match<Object, File>> arrayList = new ArrayList<Match<Object, File>>();
            AtomicInteger atomicInteger = new AtomicInteger(0);
            Parallelism.commonPool().map(list, Matcher::match, (matcher, list3) -> {
                if (((Boolean)supplier.get()).booleanValue()) {
                    throw new CancellationException();
                }
                list3.stream().map(Match::getValue).map(File::getName).limit(1L).forEach(consumer);
                biConsumer.accept(atomicInteger.incrementAndGet(), list.size());
                for (Match match : list3) {
                    arrayList.add(Match.of(match.getCandidate(), (File)match.getValue()));
                    this.values.remove(match.getCandidate());
                    this.candidates.remove(match.getValue());
                }
            });
            return arrayList;
        }
    }
}


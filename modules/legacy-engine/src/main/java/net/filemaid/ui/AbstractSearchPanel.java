package net.filemaid.ui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.UserData;
import net.filemaid.ui.FileBotTab;
import net.filemaid.ui.HistoryPanel;
import net.filemaid.ui.SelectButtonTextField;
import net.filemaid.ui.SelectDialog;
import net.filemaid.ui.rename.BlankThumbnail;
import net.filemaid.util.ui.LabelProvider;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Datasource;
import net.filemaid.web.SearchResult;
import net.filemaid.web.ThumbnailProvider;
import net.miginfocom.swing.MigLayout;

public abstract class AbstractSearchPanel<S, E>
extends JComponent {
    protected final JComponent tabbedPaneGroup = SwingUI.newPanel((LayoutManager)new MigLayout("nogrid, fill, insets 0", "align center", "[fill]8px[pref!]4px"));
    protected final JTabbedPane tabbedPane = SwingUI.createTabbedPaneGroup("Search Results");
    protected final HistoryPanel historyPanel = new HistoryPanel();
    protected final SelectButtonTextField<S> searchTextField = new SelectButtonTextField();
    protected final BasicEventList<String> searchIndex = new BasicEventList();
    protected final Action submit = SwingUI.newAction("Find", ResourceManager.getIcon("action.find"), this::find);
    protected final Action close = SwingUI.newAction("Close", actionEvent -> {
        int n = this.tabbedPane.getSelectedIndex();
        if (n > 0) {
            FileBotTab fileBotTab = (FileBotTab)this.tabbedPane.getComponentAt(n);
            fileBotTab.close();
        }
    });

    public AbstractSearchPanel() {
        this.historyPanel.setColumnHeader(2, "Duration");
        JScrollPane jScrollPane = new JScrollPane(this.historyPanel, 20, 31);
        jScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.tabbedPane.addTab("History", ResourceManager.getIcon("action.find"), jScrollPane);
        this.tabbedPaneGroup.add((Component)this.tabbedPane, "grow, wrap");
        this.setLayout((LayoutManager)new MigLayout("insets 10px n n n, nogrid, fill, novisualpadding", "align 45%", "[center, pref:pref:pref]10px[fill]"));
        this.add(this.searchTextField, "hmin 60px, gap indent:push, gap after 25px");
        this.add((Component)SwingUI.newButton(this.submit), "gap 15px, gap after indent:push, h 2+pref!, id search, sgy button");
        this.add((Component)this.tabbedPaneGroup, "newline, grow");
        this.searchTextField.getEditor().setAction(this.submit);
        this.searchTextField.getSelectButton().setModel(Arrays.asList(this.getSearchEngines()));
        this.searchTextField.getSelectButton().setLabelProvider(this.getSearchEngineLabelProvider());
        this.searchTextField.getSelectButton().addPropertyChangeListener("selected value", propertyChangeEvent -> {
            S s = this.searchTextField.getSelectButton().getSelectedValue();
            SwingUI.onSwingWorker(() -> this.getSearchIndex(s).collect(Collectors.toCollection(() -> new TreeSet(String.CASE_INSENSITIVE_ORDER))), treeSet -> {
                if (s == this.searchTextField.getSelectButton().getSelectedValue()) {
                    this.searchIndex.clear();
                    this.searchIndex.addAll((Collection)treeSet);
                }
            });
        });
        this.searchTextField.getSelectButton().setSelectedIndex(this.getUserData().entry("engine.selected", 0).getValue());
        this.searchTextField.getSelectButton().getSelectionModel().addChangeListener(changeEvent -> this.getUserData().put("engine.selected", Integer.toString(this.searchTextField.getSelectButton().getSelectedIndex())));
        AutoCompleteSupport autoCompleteSupport = AutoCompleteSupport.install(this.searchTextField.getEditor(), this.searchIndex);
        autoCompleteSupport.setTextMatchingStrategy(TextMatcherEditor.IDENTICAL_STRATEGY);
        autoCompleteSupport.setFilterMode(0);
        autoCompleteSupport.setCorrectsCase(true);
        autoCompleteSupport.setStrict(false);
        SwingUI.installAction((JComponent)this, 10, this.submit);
        SwingUI.installAction((JComponent)this, 87, 128, this.close);
    }

    protected abstract Stream<String> getSearchIndex(S var1) throws Exception;

    protected abstract S[] getSearchEngines();

    protected abstract LabelProvider<S> getSearchEngineLabelProvider();

    protected abstract UserData getUserData();

    protected abstract RequestProcessor<?, E> createRequestProcessor();

    private void find(ActionEvent actionEvent) {
        if (actionEvent.getActionCommand() == null) {
            return;
        }
        if (this.searchTextField.getText().trim().isEmpty()) {
            Logging.log.info("No search query has been entered. Please enter a search query first.");
            this.searchTextField.getEditor().requestFocusInWindow();
            return;
        }
        RequestProcessor<?, E> requestProcessor = this.createRequestProcessor();
        if (requestProcessor != null) {
            this.search(requestProcessor);
        }
    }

    private void search(RequestProcessor<?, E> requestProcessor) {
        FileBotTab<JComponent> fileBotTab = requestProcessor.tab;
        fileBotTab.setTitle(requestProcessor.getTitle());
        fileBotTab.setLoading(true);
        fileBotTab.setIcon(requestProcessor.getService().getIcon());
        fileBotTab.addTo(this.tabbedPane);
        this.tabbedPane.setSelectedComponent(fileBotTab);
        new SearchTask(requestProcessor).execute();
    }

    protected static abstract class RequestProcessor<R extends Request, E> {
        protected final R request;
        private FileBotTab<JComponent> tab;
        private SearchResult searchResult;
        private long duration = 0L;

        public RequestProcessor(R r, JComponent jComponent) {
            this.request = r;
            this.tab = new FileBotTab<JComponent>(jComponent);
        }

        public abstract Collection<? extends SearchResult> search() throws Exception;

        public abstract Collection<E> fetch() throws Exception;

        public abstract void process(Collection<E> var1);

        public abstract Datasource getService();

        public abstract URI getLink();

        public JComponent getComponent() {
            return this.tab.getComponent();
        }

        public SearchResult getSearchResult() {
            return this.searchResult;
        }

        public void setSearchResult(SearchResult searchResult) {
            this.searchResult = searchResult;
        }

        public String getStatusMessage(Collection<E> collection) {
            return String.format("%,d elements found", collection.size());
        }

        public String getTitle() {
            return Optional.ofNullable(this.searchResult).map(SearchResult::getName).orElseGet(() -> this.request.getSearchText());
        }

        protected SearchResult selectSearchResult(Collection<? extends SearchResult> collection, Component component) {
            SelectDialog<SearchResult> selectDialog = new SelectDialog<SearchResult>(component, collection, this.preview(), this.thumbnail(component), false, false, null);
            this.configureSelectDialog(selectDialog);
            selectDialog.setVisible(true);
            return selectDialog.getSelectedValue();
        }

        protected Function<SearchResult, Icon> preview() {
            return searchResult -> BlankThumbnail.BLANK_POSTER;
        }

        protected Function<SearchResult, CompletableFuture<Icon>> thumbnail(Component component) {
            if (this.getService() instanceof ThumbnailProvider) {
                ThumbnailProvider thumbnailProvider = (ThumbnailProvider)((Object)this.getService());
                ThumbnailProvider.ResolutionVariant resolutionVariant = ThumbnailProvider.ResolutionVariant.fromScaleFactor(component);
                return searchResult -> thumbnailProvider.requestThumbnail(searchResult.getId(), resolutionVariant);
            }
            return null;
        }

        protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
            selectDialog.setTitle(this.getService().getName());
            selectDialog.setLocation(SwingUI.getOffsetLocation(selectDialog));
            selectDialog.setIconImage(SwingUI.getImage(this.getService().getIcon()));
            selectDialog.setMinimumSize(new Dimension(250, 150));
            selectDialog.pack();
        }

        public long getDuration() {
            return this.duration;
        }
    }

    private class SearchTask
    extends SwingWorker<Collection<? extends SearchResult>, Void> {
        private final RequestProcessor<?, E> requestProcessor;

        public SearchTask(RequestProcessor<?, E> requestProcessor) {
            this.requestProcessor = requestProcessor;
        }

        @Override
        protected Collection<? extends SearchResult> doInBackground() throws Exception {
            long l = System.currentTimeMillis();
            try {
                Collection<? extends SearchResult> collection = this.requestProcessor.search();
                return collection;
            }
            finally {
                this.requestProcessor.duration += System.currentTimeMillis() - l;
            }
        }

        @Override
        public void done() {
            FileBotTab<JComponent> fileBotTab = this.requestProcessor.tab;
            if (fileBotTab.isClosed()) {
                return;
            }
            try {
                Collection collection = (Collection)this.get();
                SearchResult searchResult = null;
                switch (collection.size()) {
                    case 0: {
                        Logging.log.warning(Logging.format("'%s' has not been found.", ((Request)this.requestProcessor.request).getSearchText()));
                        break;
                    }
                    case 1: {
                        searchResult = (SearchResult)collection.iterator().next();
                        break;
                    }
                    default: {
                        searchResult = this.requestProcessor.selectSearchResult(collection, SwingUtilities.getWindowAncestor(AbstractSearchPanel.this));
                    }
                }
                if (searchResult == null) {
                    fileBotTab.close();
                    return;
                }
                this.requestProcessor.setSearchResult(searchResult);
                fileBotTab.setTitle(this.requestProcessor.getTitle());
                new FetchTask(this.requestProcessor).execute();
            }
            catch (Exception exception) {
                fileBotTab.close();
                Logging.log.warning(Logging.cause(exception));
            }
        }
    }

    protected static class Request {
        private final String searchText;

        public Request(String string) {
            this.searchText = string;
        }

        public String getSearchText() {
            return this.searchText;
        }
    }

    private class FetchTask
    extends SwingWorker<Collection<E>, Void> {
        private final RequestProcessor<?, E> requestProcessor;

        public FetchTask(RequestProcessor<?, E> requestProcessor) {
            this.requestProcessor = requestProcessor;
        }

        @Override
        protected final Collection<E> doInBackground() throws Exception {
            long l = System.currentTimeMillis();
            try {
                Collection collection = this.requestProcessor.fetch();
                return collection;
            }
            finally {
                this.requestProcessor.duration += System.currentTimeMillis() - l;
            }
        }

        @Override
        public void done() {
            FileBotTab<JComponent> fileBotTab = this.requestProcessor.tab;
            if (fileBotTab.isClosed()) {
                return;
            }
            try {
                Collection collection = (Collection)this.get();
                this.requestProcessor.process(collection);
                String string = this.requestProcessor.getTitle();
                Icon icon = this.requestProcessor.getService().getIcon();
                String string2 = this.requestProcessor.getStatusMessage(collection);
                AbstractSearchPanel.this.historyPanel.add(string, this.requestProcessor.getLink(), icon, string2, String.format("%,d ms", this.requestProcessor.getDuration()));
                if (((Collection)this.get()).size() <= 0) {
                    Logging.log.info(string2);
                    fileBotTab.close();
                }
            }
            catch (Exception exception) {
                fileBotTab.close();
                Logging.log.warning(Logging.cause(exception));
            }
            finally {
                fileBotTab.setLoading(false);
            }
        }
    }
}


package net.filemaid.ui.episodelist;

import ca.odell.glazedlists.EventList;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.UserData;
import net.filemaid.WebServices;
import net.filemaid.similarity.Normalization;
import net.filemaid.ui.AbstractSearchPanel;
import net.filemaid.ui.FileBotList;
import net.filemaid.ui.FileBotTab;
import net.filemaid.ui.LanguageComboBox;
import net.filemaid.ui.Mode;
import net.filemaid.ui.SelectDialog;
import net.filemaid.ui.TargetTransferable;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.episodelist.EpisodeListExportHandler;
import net.filemaid.ui.episodelist.SeasonSpinnerEditor;
import net.filemaid.ui.episodelist.SeasonSpinnerModel;
import net.filemaid.ui.transfer.SaveAction;
import net.filemaid.util.ui.ActionPopup;
import net.filemaid.util.ui.LabelProvider;
import net.filemaid.util.ui.PrototypeCellSize;
import net.filemaid.util.ui.SwingEventBus;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.MultiEpisode;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;
import net.miginfocom.swing.MigLayout;

public class EpisodeListPanel
extends AbstractSearchPanel<EpisodeListProvider, Episode> {
    private SeasonSpinnerModel seasonSpinnerModel = new SeasonSpinnerModel();
    private LanguageComboBox languageComboBox = new LanguageComboBox(Language.defaultLanguage(), this.getUserData());
    private JComboBox<SortOrder> sortOrderComboBox = new JComboBox<SortOrder>(SortOrder.values());
    private final PropertyChangeListener selectButtonListener = new PropertyChangeListener(){

        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            EpisodeListProvider episodeListProvider = (EpisodeListProvider)EpisodeListPanel.this.searchTextField.getSelectButton().getSelectedValue();
            if (!episodeListProvider.hasSeasonSupport()) {
                EpisodeListPanel.this.seasonSpinnerModel.lock(0);
            } else {
                EpisodeListPanel.this.seasonSpinnerModel.unlock();
            }
        }
    };

    public EpisodeListPanel() {
        this.historyPanel.setColumnHeader(0, "TV Series");
        this.historyPanel.setColumnHeader(1, "Number of Episodes");
        JSpinner jSpinner = new JSpinner(this.seasonSpinnerModel);
        jSpinner.setEditor(new SeasonSpinnerEditor(jSpinner));
        Dimension dimension = jSpinner.getPreferredSize();
        dimension.width += 12;
        jSpinner.setMinimumSize(dimension);
        this.add(jSpinner, "sgy button", 1);
        this.add(this.sortOrderComboBox, "sgy button, gap rel", 2);
        this.add(this.languageComboBox, "sgy button, gap rel", 3);
        this.searchTextField.getSelectButton().addPropertyChangeListener("selected value", this.selectButtonListener);
    }

    @Override
    protected Stream<String> getSearchIndex(EpisodeListProvider episodeListProvider) throws Exception {
        return episodeListProvider.getIndex().stream().flatMap(searchResult -> searchResult.getEffectiveNames().stream().map(Normalization::removeTrailingBrackets));
    }

    protected EpisodeListProvider[] getSearchEngines() {
        return WebServices.getEpisodeListProviders();
    }

    @Override
    protected LabelProvider<EpisodeListProvider> getSearchEngineLabelProvider() {
        return LabelProvider.via(Datasource::getName, Datasource::getIcon);
    }

    @Override
    protected UserData getUserData() {
        return UserData.forPackage(EpisodeListPanel.class);
    }

    protected EpisodeListRequestProcessor createRequestProcessor() {
        EpisodeListProvider episodeListProvider = (EpisodeListProvider)this.searchTextField.getSelectButton().getSelectedValue();
        String string = this.searchTextField.getText().trim();
        int n = this.seasonSpinnerModel.getSeason();
        SortOrder sortOrder = (SortOrder)((Object)this.sortOrderComboBox.getSelectedItem());
        Locale locale = this.languageComboBox.getModel().getSelectedItem().getLocale();
        return new EpisodeListRequestProcessor(new EpisodeListRequest(episodeListProvider, string, n, sortOrder, locale));
    }

    protected static class EpisodeListRequestProcessor
    extends AbstractSearchPanel.RequestProcessor<EpisodeListRequest, Episode> {
        public EpisodeListRequestProcessor(EpisodeListRequest episodeListRequest) {
            super(episodeListRequest, new EpisodeListTab());
        }

        @Override
        public Collection<SearchResult> search() throws Exception {
            return ((EpisodeListRequest)this.request).provider.lookup(((EpisodeListRequest)this.request).getSearchText(), ((EpisodeListRequest)this.request).language);
        }

        @Override
        public Collection<Episode> fetch() throws Exception {
            List<Episode> list;
            List<Episode> list2 = ((EpisodeListRequest)this.request).provider.getEpisodeList(this.getSearchResult(), ((EpisodeListRequest)this.request).order, ((EpisodeListRequest)this.request).language);
            if (((EpisodeListRequest)this.request).season != 0 && !(list = EpisodeUtilities.filterBySeason(list2, ((EpisodeListRequest)this.request).season)).isEmpty()) {
                return list;
            }
            return list2;
        }

        @Override
        public URI getLink() {
            return ((EpisodeListRequest)this.request).provider.getEpisodeListLink(this.getSearchResult());
        }

        @Override
        public void process(Collection<Episode> collection) {
            this.getComponent().setTitle(this.getTitle());
            this.getComponent().getModel().addAll(collection);
        }

        @Override
        public String getStatusMessage(Collection<Episode> collection) {
            return collection.isEmpty() ? "No episodes found" : collection.size() + " episodes";
        }

        @Override
        public EpisodeListTab getComponent() {
            return (EpisodeListTab)super.getComponent();
        }

        @Override
        public Datasource getService() {
            return ((EpisodeListRequest)this.request).provider;
        }

        @Override
        protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
            super.configureSelectDialog(selectDialog);
            selectDialog.getMessageLabel().setText("Select a TV Series:");
        }
    }

    protected static class EpisodeListRequest
    extends AbstractSearchPanel.Request {
        public final EpisodeListProvider provider;
        public final int season;
        public final SortOrder order;
        public final Locale language;

        public EpisodeListRequest(EpisodeListProvider episodeListProvider, String string, int n, SortOrder sortOrder, Locale locale) {
            super(string);
            this.provider = episodeListProvider;
            this.season = n;
            this.order = sortOrder;
            this.language = locale;
        }
    }

    protected static class EpisodeListTab
    extends FileBotList<Episode> {
        private final Action groupMultiEpisode = SwingUI.newAction("Multi Episode", actionEvent -> this.groupRun(0));
        private final Action groupDoubleEpisodes = SwingUI.newAction("Double Episodes", actionEvent -> this.groupRun(2));
        private final Action groupTripleEpisodes = SwingUI.newAction("Triple Episodes", actionEvent -> this.groupRun(3));
        private final Action groupQuadrupleEpisodes = SwingUI.newAction("Quadruple Episodes", actionEvent -> this.groupRun(4));
        private final Action groupByDate = SwingUI.newAction("By Date", actionEvent -> this.groupByDate());
        private final Action ungroupEpisodes = SwingUI.newAction("Ungroup", actionEvent -> this.groupRun(1));

        public EpisodeListTab() {
            PrototypeCellSize.fixedCellSize(this.list);
            EpisodeListExportHandler episodeListExportHandler = new EpisodeListExportHandler(this);
            this.setExportHandler(episodeListExportHandler);
            this.getTransferHandler().setClipboardHandler(episodeListExportHandler);
            this.getRemoveAction().setEnabled(true);
            this.listScrollPane.setBorder(ThemeSupport.getHorizontalRule());
            this.setBorder(null);
            JComponent jComponent = SwingUI.newPanel((LayoutManager)new MigLayout("insets rel, nogrid, novisualpadding, fill", "align center"));
            jComponent.add(SwingUI.newButton(SwingUI.newAction("Copy", ResourceManager.getIcon("rename.action.copy"), actionEvent2 -> {
                ActionPopup actionPopup = new ActionPopup("Copy", ResourceManager.getIcon("rename.action.copy"));
                for (Mode mode : Mode.episodeHandlerSequence()) {
                    actionPopup.add(SwingUI.newAction("Send to " + mode, ResourceManager.getIcon("rename.action.keeplink"), actionEvent -> this.sendTo(mode)));
                }
                actionPopup.addSeparator();
                actionPopup.add(SwingUI.newAction("Copy to Clipboard", ResourceManager.getIcon("action.paste"), this::copyToClipboard));
                SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent2);
            })));
            jComponent.add(SwingUI.newButton(SwingUI.newAction("Group", ResourceManager.getIcon("action.merge"), actionEvent -> {
                ActionPopup actionPopup = new ActionPopup("Group", ResourceManager.getIcon("action.merge"));
                actionPopup.add(this.groupMultiEpisode);
                actionPopup.addSeparator();
                actionPopup.add(this.groupDoubleEpisodes);
                actionPopup.add(this.groupTripleEpisodes);
                actionPopup.add(this.groupQuadrupleEpisodes);
                actionPopup.addSeparator();
                actionPopup.add(this.groupByDate);
                actionPopup.addSeparator();
                actionPopup.add(this.ungroupEpisodes);
                SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent);
            })));
            jComponent.add(SwingUI.newButton(new SaveAction(this.getExportHandler())));
            this.add((Component)jComponent, "South");
            JPopupMenu jPopupMenu = SwingUI.newPopupMenu("Episodes");
            JMenu jMenu = new JMenu("Send to");
            for (Mode mode : Mode.episodeHandlerSequence()) {
                jMenu.add(SwingUI.newAction(mode.toString(), mode.getIcon(), actionEvent -> this.sendTo(mode)));
            }
            jPopupMenu.add(jMenu);
            jPopupMenu.addSeparator();
            JMenu jMenu2 = new JMenu("Group");
            jMenu2.add(this.groupMultiEpisode);
            jMenu2.addSeparator();
            jMenu2.add(this.groupDoubleEpisodes);
            jMenu2.add(this.groupTripleEpisodes);
            jMenu2.add(this.groupQuadrupleEpisodes);
            jMenu2.addSeparator();
            jMenu2.add(this.groupByDate);
            jMenu2.addSeparator();
            jMenu2.add(this.ungroupEpisodes);
            jPopupMenu.add(jMenu2);
            jPopupMenu.addSeparator();
            jPopupMenu.add(SwingUI.newAction("Duplicate", this::duplicate));
            jPopupMenu.add(SwingUI.newAction("Delete", this::delete));
            jPopupMenu.addSeparator();
            jPopupMenu.add(SwingUI.newAction("Copy", ResourceManager.getIcon("rename.action.copy"), this::copyToClipboard));
            jPopupMenu.add(new SaveAction(this.getExportHandler()));
            jPopupMenu.addSeparator();
            jPopupMenu.add(SwingUI.newAction("Select All", this::selectAll));
            this.list.setComponentPopupMenu(jPopupMenu);
            SwingUI.installAction((JComponent)this.list, 71, 128, this.groupMultiEpisode);
            SwingUI.installAction((JComponent)this.list, 85, 128, this.ungroupEpisodes);
        }

        @Override
        public EpisodeListExportHandler getExportHandler() {
            return (EpisodeListExportHandler)super.getExportHandler();
        }

        public void sendTo(Mode mode) {
            SwingEventBus.getInstance().post(new TargetTransferable(mode, this.getExportHandler().exportEpisodeSelection()));
        }

        public void copyToClipboard(ActionEvent actionEvent) {
            this.getTransferHandler().getClipboardHandler().exportToClipboard(this, Toolkit.getDefaultToolkit().getSystemClipboard(), 1);
        }

        public void duplicate(ActionEvent actionEvent) {
            EventList<Episode> eventList = this.getModel();
            ListSelectionModel listSelectionModel = this.list.getSelectionModel();
            if (listSelectionModel.isSelectionEmpty()) {
                Logging.log.info("Please select 1 or more episodes.");
                return;
            }
            for (int i = listSelectionModel.getMaxSelectionIndex(); i >= listSelectionModel.getMinSelectionIndex(); --i) {
                if (!listSelectionModel.isSelectedIndex(i)) continue;
                eventList.add(i + 1, (Episode)eventList.get(i));
                listSelectionModel.addSelectionInterval(i + 1, i + 1);
            }
        }

        public void delete(ActionEvent actionEvent) {
            EventList eventList = this.getModel();
            ListSelectionModel listSelectionModel = this.list.getSelectionModel();
            if (listSelectionModel.isSelectionEmpty()) {
                Logging.log.info("Please select 1 or more episodes.");
                return;
            }
            for (int i = listSelectionModel.getMaxSelectionIndex(); i >= listSelectionModel.getMinSelectionIndex(); --i) {
                if (!listSelectionModel.isSelectedIndex(i)) continue;
                eventList.remove(i);
            }
            if (eventList.size() == 0) {
                this.close();
            }
        }

        public void selectAll(ActionEvent actionEvent) {
            int n = this.getModel().size();
            ListSelectionModel listSelectionModel = this.list.getSelectionModel();
            if (n > 0) {
                listSelectionModel.setValueIsAdjusting(true);
                listSelectionModel.setSelectionInterval(0, n - 1);
                listSelectionModel.setValueIsAdjusting(false);
            }
        }

        private void groupRun(int n) {
            boolean bl = n > 0 && this.list.getSelectedIndex() < 0;
            ArrayList<Object> arrayList = new ArrayList<Object>();
            ArrayDeque<Group> arrayDeque = new ArrayDeque<Group>();
            for (int i = 0; i < this.getModel().size(); ++i) {
                Episode episode = (Episode)this.getModel().get(i);
                if (bl || this.list.isSelectedIndex(i)) {
                    for (Episode episode2 : (Episode[])EpisodeUtilities.streamMultiEpisode(episode).toArray(Episode[]::new)) {
                        if (arrayDeque.isEmpty() || ((Group)arrayDeque.peekLast()).size() == n) {
                            arrayDeque.add(new Group(arrayList.size()));
                            arrayList.add(arrayDeque.getLast());
                        }
                        ((Group)arrayDeque.getLast()).add(episode2);
                    }
                    continue;
                }
                arrayList.add(episode);
            }
            if (arrayDeque.isEmpty() || ((Group)arrayDeque.getFirst()).size() < n) {
                Logging.log.info(Logging.format("Please select at least %s episodes.", n > 1 ? n : 2));
                return;
            }
            this.commit(arrayList);
        }

        private void groupByDate() {
            boolean bl = this.list.getSelectedIndex() < 0;
            Map<SimpleDate, List<Episode>> map = (bl ? this.getModel() : this.list.getSelectedValuesList()).stream().flatMap(episode -> EpisodeUtilities.streamMultiEpisode(episode)).filter(episode -> episode.getAirdate() != null).collect(Collectors.groupingBy(Episode::getAirdate));
            HashSet<SimpleDate> hashSet = new HashSet<SimpleDate>(map.keySet());
            if (map.isEmpty() || map.values().stream().noneMatch(list -> list.size() > 1)) {
                Logging.log.info("Please select at least 2 episodes that aired on the same day.");
                return;
            }
            ArrayList<Object> arrayList = new ArrayList<Object>();
            for (int i = 0; i < this.getModel().size(); ++i) {
                Episode episode2 = (Episode)this.getModel().get(i);
                if (bl || this.list.isSelectedIndex(i)) {
                    for (Episode episode3 : (Episode[])EpisodeUtilities.streamMultiEpisode(episode2).toArray(Episode[]::new)) {
                        SimpleDate simpleDate = episode3.getAirdate();
                        if (simpleDate != null) {
                            if (!hashSet.remove(simpleDate)) continue;
                            List<Episode> list2 = map.get(simpleDate);
                            Group group = new Group(arrayList.size());
                            list2.forEach(group::add);
                            arrayList.add(group);
                            continue;
                        }
                        arrayList.add(episode3);
                    }
                    continue;
                }
                arrayList.add(episode2);
            }
            this.commit(arrayList);
        }

        private void commit(List<Object> list) {
            this.getModel().clear();
            this.getModel().addAll((Collection)list.stream().map(object -> {
                if (object instanceof Group) {
                    return ((Group)object).toEpisode();
                }
                return (Episode)object;
            }).collect(Collectors.toList()));
            this.list.setSelectedIndices(list.stream().map(object -> {
                if (object instanceof Group) {
                    return (Group)object;
                }
                return null;
            }).filter(Objects::nonNull).mapToInt(Group::getIndex).toArray());
        }

        private void close() {
            FileBotTab fileBotTab = (FileBotTab)SwingUtilities.getAncestorOfClass(FileBotTab.class, this);
            if (fileBotTab != null) {
                fileBotTab.close();
            }
        }

        private static class Group {
            private final int index;
            private final List<Episode> episodes = new ArrayList<Episode>();

            public Group(int n) {
                this.index = n;
            }

            public int getIndex() {
                return this.index;
            }

            public void add(Episode episode) {
                this.episodes.add(episode);
            }

            public int size() {
                return this.episodes.size();
            }

            public Episode toEpisode() {
                return this.episodes.size() == 1 ? this.episodes.get(0) : new MultiEpisode((Episode[])this.episodes.stream().sorted(EpisodeUtilities.EPISODE_NUMBERS_COMPARATOR).toArray(Episode[]::new));
            }
        }
    }
}


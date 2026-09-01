package net.filemaid.ui.subtitle;

import com.google.common.eventbus.Subscribe;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.LayoutManager;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.UserData;
import net.filemaid.UserInteraction;
import net.filemaid.WebServices;
import net.filemaid.ui.AbstractSearchPanel;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.LanguageComboBox;
import net.filemaid.ui.LanguageComboBoxModel;
import net.filemaid.ui.SelectDialog;
import net.filemaid.ui.subtitle.SubtitleDownloadComponent;
import net.filemaid.ui.subtitle.SubtitleDropTarget;
import net.filemaid.ui.subtitle.SubtitlePackage;
import net.filemaid.ui.subtitle.SubtitlePackageFeatureLink;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.util.JsonUtilities;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.LabelProvider;
import net.filemaid.util.ui.LinkButton;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Datasource;
import net.filemaid.web.HttpClientError;
import net.filemaid.web.OpenSubtitlesRestClient;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;
import net.filemaid.web.SubtitleSearchResult;
import net.miginfocom.swing.MigLayout;

public class SubtitlePanel
extends AbstractSearchPanel<SubtitleProvider, SubtitlePackage> {
    private LanguageComboBox languageComboBox = new LanguageComboBox(LanguageComboBoxModel.ALL_LANGUAGES, this.getUserData());
    private WebServices.OpenSubtitlesClient database = WebServices.OpenSubtitles;
    private final SubtitleDropTarget uploadDropTarget = new SubtitleDropTarget.Upload(){

        @Override
        public WebServices.OpenSubtitlesClient getSubtitleService() {
            return SubtitlePanel.this.database;
        }

        @Override
        protected void handleDrop(List<File> list) {
            if (SubtitlePanel.this.database instanceof OpenSubtitlesRestClient) {
                UserInteraction.browse("https://www.opensubtitles.com/en/upload");
                return;
            }
            if (SubtitlePanel.this.login(this.getSubtitleService())) {
                super.handleDrop(list);
            }
        }
    };
    private final SubtitleDropTarget downloadDropTarget = new SubtitleDropTarget.Download(){

        @Override
        public Locale getLocale() {
            return SubtitlePanel.this.languageComboBox.getModel().getSelectedItem() == LanguageComboBoxModel.ALL_LANGUAGES ? Locale.ROOT : SubtitlePanel.this.languageComboBox.getModel().getSelectedItem().getLocale();
        }

        @Override
        public SubtitleLookupService[] getVideoHashSubtitleServices() {
            return WebServices.getSubtitleLookupServices(this.getLocale());
        }

        @Override
        public SubtitleProvider[] getSubtitleProviders() {
            return WebServices.getSubtitleProviders(this.getLocale());
        }

        @Override
        public WebServices.OpenSubtitlesClient getSubtitleService() {
            return SubtitlePanel.this.database;
        }

        @Override
        public Locale getQueryLanguage() {
            return SubtitlePanel.this.languageComboBox.getModel().getSelectedItem() == LanguageComboBoxModel.ALL_LANGUAGES ? null : SubtitlePanel.this.languageComboBox.getModel().getSelectedItem().getLocale();
        }

        @Override
        protected void handleDrop(List<File> list) {
            if (SubtitlePanel.this.login(this.getSubtitleService())) {
                super.handleDrop(list);
            }
        }
    };
    private final QueryFilter<Integer> seasonFilter = new QueryFilter<Integer>("season", string -> string == null ? -1 : Integer.parseInt(string));
    private final QueryFilter<Integer> episodeFilter = new QueryFilter<Integer>("episode", string -> string == null ? -1 : Integer.parseInt(string));

    public SubtitlePanel() {
        this.historyPanel.setColumnHeader(0, "Search");
        this.historyPanel.setColumnHeader(1, "Number of Subtitles");
        this.add(this.languageComboBox, "sgy button", 1);
        this.add(SwingUI.createImageButton(SwingUI.newAction("Login", ResourceManager.getIcon("action.user"), actionEvent -> this.promptLogin())), "gap rel, sgy button", 2);
        this.add(this.uploadDropTarget, "width 54px!, height 54px!, gap before unrel", 4);
        this.add(this.downloadDropTarget, "width 54px!, height 54px!, gap before 10px, gap after 10px", 5);
    }

    private void promptLogin() {
        BaseDialog baseDialog = new BaseDialog(SwingUI.getWindow(this), "Login", Dialog.ModalityType.DOCUMENT_MODAL);
        baseDialog.setLocation(SwingUI.getOffsetLocation(baseDialog));
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("fill, insets panel"));
        jPanel.setBorder(new TitledBorder("OpenSubtitles"));
        jPanel.add((Component)new JLabel("Username:"), "gap rel");
        JTextField jTextField = new JTextField(12);
        jPanel.add((Component)jTextField, "growx, wrap rel");
        jPanel.add((Component)new JLabel("Password:"), "gap rel");
        JPasswordField jPasswordField = new JPasswordField(12);
        jPanel.add((Component)jPasswordField, "growx, wrap unrel");
        String[] stringArray = WebServices.getLogin(this.database);
        if (stringArray != null && stringArray.length == 2) {
            jTextField.setText(stringArray[0]);
            jPasswordField.setText(stringArray[1]);
        }
        URI uRI = this.database.getAccountLink(jTextField.getText());
        if (jTextField.getText().isEmpty()) {
            jPanel.add((Component)new LinkButton("Register Account", "Register to increase your download quota", this.database.getIcon(), uRI), "spanx 2, tag left");
        } else {
            jPanel.add((Component)new LinkButton("Upgrade Account", "Upgrade to increase your download quota", this.database.getIcon(), uRI), "spanx 2, tag left");
        }
        JRootPane jRootPane = baseDialog.getRootPane();
        jRootPane.setLayout((LayoutManager)new MigLayout("fill, insets dialog"));
        jRootPane.removeAll();
        jRootPane.add((Component)jPanel, "growx, wrap, top, push");
        Action action = SwingUI.newAction("OK", ResourceManager.getIcon("dialog.continue"), actionEvent -> {
            String string = jTextField.getText();
            String string2 = new String(jPasswordField.getPassword());
            if (string.isEmpty() && string2.isEmpty()) {
                WebServices.setLogin(this.database, null, null);
                baseDialog.setVisible(false);
                return;
            }
            if (UserInteraction.isEmailAddress(string)) {
                Logging.log.warning(Logging.message(this.database.getName(), "Please enter your username and not your email address."));
                return;
            }
            baseDialog.setCursor(Cursor.getPredefinedCursor(3));
            SwingUI.onSwingWorker(() -> {
                WebServices.OpenSubtitlesClient openSubtitlesClient = this.database.newInstance();
                openSubtitlesClient.login(string, string2);
                Map<?, ?> map = openSubtitlesClient.getServerInfo();
                openSubtitlesClient.logout();
                return map;
            }, map -> {
                Map map2 = (Map)map.get("download_limits");
                if (map2 != null) {
                    Logging.log.info(Logging.format("Your daily quota is at %s of %s.", map2.get("client_24h_download_count"), map2.get("client_24h_download_limit")));
                } else {
                    Logging.log.info(Logging.format("Your daily quota is at %s of %s.", map.get("downloads_count"), map.get("allowed_downloads")));
                }
                WebServices.setLogin(this.database, string, string2);
                baseDialog.setVisible(false);
            }, exception -> {
                HttpClientError httpClientError;
                if (uRI.getHost().equals("www.opensubtitles.com") && (httpClientError = Logging.findCause(exception, HttpClientError.class)) != null && httpClientError.isErrorResponse()) {
                    try {
                        Object object = JsonUtilities.readJson(httpClientError.getResponseContent());
                        String message = JsonUtilities.getString(object, "message");
                        if (message != null && !message.isEmpty()) {
                            if (message.contains("invalid username")) {
                                SwingUtilities.invokeLater(() -> GlassOptionPane.showSuggestionDialog(message, "<html><p>Your login details are incorrect.</p><p>Please go to www.opensubtitles.com to reset your password.</p></html>", this.database.getName(), this.database.getIcon(), this, () -> UserInteraction.browse(uRI.toString())));
                                baseDialog.setVisible(false);
                            } else {
                                Logging.log.warning(Logging.message(this.database.getName(), message));
                            }
                            return;
                        }
                    }
                    catch (Exception exception2) {
                        // empty catch block
                    }
                }
                Logging.log.warning(Logging.cause(this.database.getName(), exception));
            }, () -> baseDialog.setCursor(Cursor.getPredefinedCursor(0)));
        });
        Action action2 = SwingUI.newAction("Cancel", ResourceManager.getIcon("dialog.cancel"), actionEvent -> baseDialog.setVisible(false));
        jRootPane.add((Component)SwingUI.newButton(action2), "tag cancel, split 2");
        jRootPane.add((Component)SwingUI.newButton(action), "tag ok");
        baseDialog.pack();
        baseDialog.setVisible(true);
    }

    private boolean login(SubtitleProvider subtitleProvider) {
        if (subtitleProvider.requireLogin()) {
            Logging.log.info(Logging.format("%s account required. Please enter your login details.", subtitleProvider.getName()));
            SwingUI.invokeLater(50, this::promptLogin);
            return false;
        }
        return true;
    }

    @Subscribe
    public void handle(Transferable transferable) throws Exception {
        List<File> list = FileTransferable.getFilesFromTransferable(transferable);
        if (list != null && !list.isEmpty() && this.downloadDropTarget.getDropAction(list) != SubtitleDropTarget.DropAction.Cancel) {
            this.downloadDropTarget.handleDrop(list);
        }
    }

    @Override
    protected Stream<String> getSearchIndex(SubtitleProvider subtitleProvider) throws Exception {
        return subtitleProvider.getIndex().stream().map(SubtitleSearchResult::toString);
    }

    protected SubtitleProvider[] getSearchEngines() {
        return WebServices.getSubtitleProviders(this.getLocale());
    }

    @Override
    protected LabelProvider<SubtitleProvider> getSearchEngineLabelProvider() {
        return LabelProvider.via(Datasource::getName, Datasource::getIcon);
    }

    @Override
    protected UserData getUserData() {
        return UserData.forPackage(SubtitlePanel.class);
    }

    protected SubtitleRequestProcessor createRequestProcessor() {
        SubtitleProvider subtitleProvider = (SubtitleProvider)this.searchTextField.getSelectButton().getSelectedValue();
        if (!this.login(subtitleProvider)) {
            return null;
        }
        String string = this.searchTextField.getText();
        int n = this.seasonFilter.match(string);
        string = this.seasonFilter.remove(string).trim();
        int n2 = this.episodeFilter.match(string);
        string = this.episodeFilter.remove(string).trim();
        Language language = this.languageComboBox.getModel().getSelectedItem();
        return new SubtitleRequestProcessor(new SubtitleRequest(subtitleProvider, string, n, n2, language));
    }

    protected static class QueryFilter<T> {
        private final Pattern pattern;
        private final Function<String, T> parser;

        public QueryFilter(String string, Function<String, T> function) {
            this.pattern = Pattern.compile("(?:" + string + "):(\\w+)", 258);
            this.parser = function;
        }

        public T match(String string) {
            Matcher matcher = this.pattern.matcher(string);
            if (matcher.find()) {
                return this.parser.apply(matcher.group(matcher.groupCount()));
            }
            return this.parser.apply(null);
        }

        public String remove(String string) {
            return this.pattern.matcher(string).replaceAll("");
        }
    }

    protected static class SubtitleRequestProcessor
    extends AbstractSearchPanel.RequestProcessor<SubtitleRequest, SubtitlePackage> {
        public SubtitleRequestProcessor(SubtitleRequest subtitleRequest) {
            super(subtitleRequest, new SubtitleDownloadComponent());
        }

        @Override
        public Collection<SubtitleSearchResult> search() throws Exception {
            return ((SubtitleRequest)this.request).getProvider().search(((SubtitleRequest)this.request).getSearchText());
        }

        @Override
        public SubtitleSearchResult getSearchResult() {
            return (SubtitleSearchResult)super.getSearchResult();
        }

        @Override
        public Collection<SubtitlePackage> fetch() throws Exception {
            ArrayList<SubtitlePackage> arrayList = new ArrayList<SubtitlePackage>();
            for (SubtitleDescriptor subtitleDescriptor : ((SubtitleRequest)this.request).getProvider().getSubtitleList(this.getSearchResult(), ((SubtitleRequest)this.request).getEpisodeFilter(), ((SubtitleRequest)this.request).getLanguage())) {
                arrayList.add(new SubtitlePackage(((SubtitleRequest)this.request).getProvider(), subtitleDescriptor));
            }
            return arrayList;
        }

        @Override
        public URI getLink() {
            return ((SubtitleRequest)this.request).getProvider().getSubtitleListLink(this.getSearchResult(), ((SubtitleRequest)this.request).getLanguage());
        }

        @Override
        public void process(Collection<SubtitlePackage> collection) {
            this.getComponent().setLanguageVisible(((SubtitleRequest)this.request).getLanguage() == null);
            this.getComponent().getPackageModel().add(SubtitlePackageFeatureLink.EXACT_SEARCH);
            this.getComponent().getPackageModel().addAll(collection);
        }

        @Override
        public SubtitleDownloadComponent getComponent() {
            return (SubtitleDownloadComponent)super.getComponent();
        }

        @Override
        public String getStatusMessage(Collection<SubtitlePackage> collection) {
            return collection.isEmpty() ? "No subtitles found" : String.format("%,d subtitles", collection.size());
        }

        @Override
        public Datasource getService() {
            return ((SubtitleRequest)this.request).provider;
        }

        @Override
        protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
            super.configureSelectDialog(selectDialog);
            selectDialog.getMessageLabel().setText("Select a Movie or TV Series:");
        }
    }

    protected static class SubtitleRequest
    extends AbstractSearchPanel.Request {
        private final SubtitleProvider provider;
        private final Language language;
        private final int season;
        private final int episode;

        public SubtitleRequest(SubtitleProvider subtitleProvider, String string, int n, int n2, Language language) {
            super(string);
            this.season = n;
            this.episode = n2;
            this.provider = subtitleProvider;
            this.language = language;
        }

        public SubtitleProvider getProvider() {
            return this.provider;
        }

        public Locale getLanguage() {
            return this.language == LanguageComboBoxModel.ALL_LANGUAGES ? null : this.language.getLocale();
        }

        public int[][] getEpisodeFilter() {
            int[][] nArrayArray;
            if (this.season >= 0 && this.episode >= 0) {
                int[][] nArrayArray2 = new int[1][];
                nArrayArray = nArrayArray2;
                nArrayArray2[0] = new int[]{this.season, this.episode};
            } else if (this.season >= 0) {
                int[][] nArrayArray3 = new int[1][];
                nArrayArray = nArrayArray3;
                nArrayArray3[0] = new int[]{this.season, -1};
            } else {
                nArrayArray = null;
            }
            return nArrayArray;
        }
    }
}


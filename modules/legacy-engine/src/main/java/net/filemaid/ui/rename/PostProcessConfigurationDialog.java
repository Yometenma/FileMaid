package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.TitledBorder;
import net.filemaid.ResourceManager;
import net.filemaid.StandardPostProcessAction;
import net.filemaid.UserData;
import net.filemaid.postprocess.Script;
import net.filemaid.ui.rename.PostProcessScriptEditor;
import net.filemaid.util.PreferencesMap;
import net.filemaid.util.ui.ActionPopup;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class PostProcessConfigurationDialog
extends JComponent {
    private final Set<StandardPostProcessAction> actions;
    private final Map<String, Script> scripts;
    private JComponent selectionPanel;
    private JComponent scriptPanel;
    private static final PreferencesMap<Script> persistentScripts = UserData.forPackage(PostProcessConfigurationDialog.class).node("scripts").asMap(Script.class);
    public static final Script BLANK_SCRIPT = new Script(null, "", "{ source, target, metadata ->\n\t\n}\n");

    public PostProcessConfigurationDialog(Set<StandardPostProcessAction> set, List<Script> list) {
        this.actions = EnumSet.copyOf(set);
        this.scripts = this.collect(list);
        this.selectionPanel = this.createSelectionPanel();
        this.scriptPanel = this.createScriptPanel();
        this.updateScriptPanel();
        this.setLayout((LayoutManager)new MigLayout("flowy, fill, insets 0, hidemode 3"));
        this.add((Component)this.selectionPanel, "grow");
        this.add((Component)this.scriptPanel, "grow");
    }

    private JComponent createSelectionPanel() {
        Set<StandardPostProcessAction> set = StandardPostProcessAction.getMetadataActions();
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("gapx 25px, flowy, wrap " + (set.size() + 1) / 2));
        jPanel.setBorder(new TitledBorder(""));
        for (StandardPostProcessAction standardPostProcessAction : set) {
            JCheckBox jCheckBox = new JCheckBox(standardPostProcessAction.getLabel(), this.actions.contains(standardPostProcessAction));
            jCheckBox.setToolTipText(standardPostProcessAction.getDescription());
            jCheckBox.addItemListener(itemEvent -> {
                if (jCheckBox.isSelected()) {
                    this.actions.add(standardPostProcessAction);
                } else {
                    this.actions.remove(standardPostProcessAction);
                }
            });
            jCheckBox.setOpaque(false);
            jPanel.add(jCheckBox);
        }
        return jPanel;
    }

    private JComponent createScriptPanel() {
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("gapx 25px, fill, wrap 2"));
        jPanel.setBorder(new TitledBorder(""));
        jPanel.setVisible(false);
        return jPanel;
    }

    private void updateScriptPanel() {
        this.scriptPanel.removeAll();
        this.scripts.forEach((string, script) -> {
            JCheckBox jCheckBox = new JCheckBox(script.getName(), true);
            jCheckBox.addItemListener(itemEvent -> {
                if (!jCheckBox.isSelected()) {
                    this.scripts.remove(string);
                    this.updateScriptPanel();
                }
            });
            jCheckBox.setOpaque(false);
            this.scriptPanel.add(jCheckBox);
        });
        this.scriptPanel.setVisible(this.scriptPanel.getComponentCount() > 0);
        this.revalidate();
        this.repaint();
    }

    public Set<StandardPostProcessAction> getSelectedActions() {
        return this.actions;
    }

    public List<Script> getSelectedScripts() {
        return this.scripts.values().stream().collect(Collectors.toList());
    }

    private Map<String, Script> collect(Collection<Script> collection) {
        return collection.stream().filter(Objects::nonNull).collect(Collectors.toMap(script -> script.getIdentifier(), script -> script, (script, script2) -> script, LinkedHashMap::new));
    }

    private void showScriptsPopup(ActionEvent actionEvent2) {
        ActionPopup actionPopup = new ActionPopup("User Scripts", ResourceManager.getIcon("action.script"));
        Map<String, Script> map = this.collect(persistentScripts.values());
        actionPopup.addGroup((Action[])map.values().stream().map(script -> SwingUI.newAction(script.getName(), ResourceManager.getIcon("script.edit"), actionEvent -> this.showScriptEditor((Script)script, SwingUI.getWindow(this)))).toArray(Action[]::new));
        actionPopup.addGroup((Action[])PostProcessConfigurationDialog.getExampleScripts().stream().filter(script -> !map.containsKey(script.getIdentifier())).map(script -> SwingUI.newAction(script.getName(), ResourceManager.getIcon("script.add"), actionEvent -> this.showScriptEditor((Script)script, SwingUI.getWindow(this)))).toArray(Action[]::new));
        actionPopup.addGroup(SwingUI.newAction("New Script", ResourceManager.getIcon("script.add"), actionEvent -> this.showScriptEditor(BLANK_SCRIPT, SwingUI.getWindow(this))));
        SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent2);
    }

    private void showScriptEditor(Script script2, Window window) {
        PostProcessScriptEditor postProcessScriptEditor = new PostProcessScriptEditor(window);
        postProcessScriptEditor.setScript(script2);
        postProcessScriptEditor.setLocation(SwingUI.getOffsetLocation(postProcessScriptEditor));
        postProcessScriptEditor.setVisible(true);
        switch (postProcessScriptEditor.getResult()) {
            case SET: {
                postProcessScriptEditor.getScript().ifPresent(script -> {
                    this.scripts.put(script.getIdentifier(), (Script)script);
                    this.updateScriptPanel();
                    persistentScripts.put(script.getIdentifier(), (Script)script);
                });
                break;
            }
            case DELETE: {
                if (script2.getIdentifier() == null) break;
                this.scripts.remove(script2.getIdentifier());
                this.updateScriptPanel();
                persistentScripts.remove(script2.getIdentifier());
                break;
            }
        }
    }

    private static List<Script> getExampleScripts() {
        ArrayList<Script> arrayList = new ArrayList<Script>();
        try (Scanner scanner = new Scanner(PostProcessConfigurationDialog.class.getResourceAsStream("PostProcessConfigurationDialog.Examples.groovy"), "UTF-8").useDelimiter("/[*]|[*]/");){
            while (scanner.hasNext()) {
                arrayList.add(new Script(scanner.next().trim(), scanner.next().trim() + "\n"));
            }
        }
        return arrayList;
    }

    public static List<Script> getUserScripts() {
        return persistentScripts.values();
    }

    public static void showUserScriptEditor(Script script2, Window window) {
        PostProcessScriptEditor postProcessScriptEditor = new PostProcessScriptEditor(window);
        postProcessScriptEditor.setScript(script2);
        postProcessScriptEditor.setLocation(SwingUI.getOffsetLocation(postProcessScriptEditor));
        postProcessScriptEditor.setVisible(true);
        switch (postProcessScriptEditor.getResult()) {
            case SET: {
                postProcessScriptEditor.getScript().ifPresent(script -> persistentScripts.put(script.getIdentifier(), (Script)script));
                break;
            }
            case DELETE: {
                if (script2.getIdentifier() == null) break;
                persistentScripts.remove(script2.getIdentifier());
                break;
            }
        }
    }

    public static void showConfigurationDialog(Set<StandardPostProcessAction> set, List<Script> list, Component component, Consumer<PostProcessConfigurationDialog> consumer) {
        PostProcessConfigurationDialog postProcessConfigurationDialog = new PostProcessConfigurationDialog(set, list);
        JButton jButton = SwingUI.createImageButton(SwingUI.newAction("User Scripts", ResourceManager.getIcon("script.add"), postProcessConfigurationDialog::showScriptsPopup));
        GlassOptionPane.showConfigurationDialog(postProcessConfigurationDialog, jButton, "Post Processing Options", ResourceManager.getIcon("script.palette"), component, consumer);
    }
}


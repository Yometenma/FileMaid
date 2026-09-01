package net.filemaid.ui.filter;

import java.awt.Component;
import java.awt.LayoutManager;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import net.filemaid.HistorySpooler;
import net.filemaid.MediaTypes;
import net.filemaid.Parallelism;
import net.filemaid.media.ImageMetadata;
import net.filemaid.media.LocalDatasource;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.ui.filter.FileTree;
import net.filemaid.ui.filter.FileTreeExportHandler;
import net.filemaid.ui.filter.Tool;
import net.filemaid.ui.transfer.DefaultTransferHandler;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.miginfocom.swing.MigLayout;

class TypeTool
extends Tool<TreeModel> {
    private FileTree tree = new FileTree();

    public TypeTool() {
        super("Types");
        this.setLayout((LayoutManager)new MigLayout("insets 0, fill"));
        JScrollPane jScrollPane = new JScrollPane(this.tree);
        jScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.add((Component)new LoadingOverlayPane((Component)jScrollPane, this), "grow");
        this.tree.setTransferHandler(new DefaultTransferHandler(null, new FileTreeExportHandler()));
        this.tree.setDragEnabled(true);
    }

    @Override
    protected TreeModel createModelInBackground(List<File> list) throws Exception {
        if (list.isEmpty()) {
            return new DefaultTreeModel(new FileTree.FolderNode("Types", Collections.emptyList()));
        }
        List<File> list3 = FileUtilities.listFiles(list, FileUtilities.NOT_HIDDEN, FileUtilities.HUMAN_NAME_ORDER);
        List<File> list4 = FileUtilities.filter(list3, FileUtilities.FILES);
        Map<MetaType, Set<File>> map = this.getFilterGroups(list3);
        Map<MetaType, List<File>> map2 = this.getInverseFilterGroups(list4, map);
        Map<String, List<File>> map3 = this.getExtensionGroups(list4);
        ArrayList<TreeNode> arrayList = new ArrayList<TreeNode>();
        map.forEach((metaType, set) -> arrayList.add(this.createStatisticsNode(metaType.getName(), (Collection<File>)set)));
        map2.forEach((metaType, list2) -> arrayList.add(this.createStatisticsNode("No " + metaType.getName(), (Collection<File>)list2)));
        map3.forEach((string, list2) -> arrayList.add(this.createStatisticsNode("*." + string, (Collection<File>)list2)));
        return new DefaultTreeModel(new FileTree.FolderNode("Types", arrayList));
    }

    private Map<String, List<File>> getExtensionGroups(List<File> list2) {
        TreeMap<String, List<File>> treeMap = new TreeMap<String, List<File>>();
        FileUtilities.mapByExtension(list2).forEach((string, list) -> {
            if (string != null) {
                treeMap.put((String)string, (List<File>)list);
            }
        });
        return treeMap;
    }

    private Map<MetaType, List<File>> getInverseFilterGroups(List<File> list, Map<MetaType, Set<File>> map) {
        EnumMap<MetaType, List<File>> enumMap = new EnumMap<MetaType, List<File>>(MetaType.class);
        map.forEach((metaType, set) -> {
            List list2;
            if (metaType.inverse() && (list2 = list.stream().filter(file -> metaType.acceptFileType((File)file) && !set.contains(file)).collect(Collectors.toList())).size() > 0) {
                enumMap.put((MetaType)metaType, list2);
            }
        });
        return enumMap;
    }

    private Map<MetaType, Set<File>> getFilterGroups(List<File> list) throws Exception {
        EnumMap<MetaType, Set<File>> enumMap = new EnumMap<MetaType, Set<File>>(MetaType.class);
        Parallelism.commonPool().map(list, MetaType::classify, (file, set) -> set.forEach(metaType2 -> enumMap.computeIfAbsent((MetaType)metaType2, metaType -> new LinkedHashSet()).add(file)));
        return enumMap;
    }

    @Override
    protected void setModel(TreeModel treeModel) {
        this.tree.setModel(treeModel);
    }

    public static enum MetaType implements FileFilter
    {
        EPISODE("Episode", MediaTypes.VIDEO_FILES, file -> MediaDetection.isEpisode(file, true)),
        MOVIE("Movie", MediaTypes.VIDEO_FILES, file -> MediaDetection.isMovie(file)),
        MOVIE_FOLDER("Movie Folder", FileUtilities.FOLDERS, file -> MediaDetection.isMovie(file)),
        DISK_FOLDER("Disk Folder", FileUtilities.FOLDERS, MediaFileUtilities.DISK_FOLDERS),
        VIDEO("Video", FileUtilities.FILES, MediaTypes.VIDEO_FILES),
        SUBTITLE("Subtitle", FileUtilities.FILES, MediaTypes.SUBTITLE_FILES),
        AUDIO("Audio", FileUtilities.FILES, MediaTypes.AUDIO_FILES),
        ARCHIVE("Archive", FileUtilities.FILES, MediaTypes.ARCHIVE_FILES),
        VERIFICATION("Verification", FileUtilities.FILES, MediaTypes.VERIFICATION_FILES),
        EXTRAS("Extras", FileUtilities.FILES, MediaFileUtilities.EXTRA_FILES),
        CLUTTER("Clutter", FileUtilities.FILES, MediaFileUtilities.CLUTTER_TYPES),
        XATTR("Attributes", FileUtilities.FILES, file -> LocalDatasource.XATTR.match(file) != null),
        EXIF("EXIF", ImageMetadata.SUPPORTED_FILE_TYPES, file -> LocalDatasource.EXIF.match(file) != null),
        HISTORY("History", FileUtilities.FILES, file -> HistorySpooler.HISTORY.getOriginalPath(file) != null);

        private final String name;
        private final FileFilter kind;
        private final FileFilter filter;

        private MetaType(String string2, FileFilter fileFilter, FileFilter fileFilter2) {
            this.name = string2;
            this.kind = fileFilter;
            this.filter = fileFilter2;
        }

        public String getName() {
            return this.name;
        }

        public boolean inverse() {
            switch (this) {
                case XATTR: 
                case EXIF: 
                case HISTORY: {
                    return true;
                }
            }
            return false;
        }

        public boolean acceptFileType(File file) {
            return this.kind.accept(file);
        }

        @Override
        public boolean accept(File file) {
            return this.kind.accept(file) && this.filter.accept(file);
        }

        public String toString() {
            return this.name;
        }

        public static Set<MetaType> classify(File file) {
            EnumSet<MetaType> enumSet = EnumSet.noneOf(MetaType.class);
            for (MetaType metaType : MetaType.values()) {
                if (!metaType.accept(file)) continue;
                enumSet.add(metaType);
            }
            return enumSet;
        }
    }
}


package net.filemaid.postprocess;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;
import net.filemaid.RenameAction;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Feedback;
import net.filemaid.similarity.Match;

public class SetPosixPermissions
implements Apply {
    public static final String DEFAULT_PERMISSIONS = System.getProperty("apply.chmod.permissions", "rw-r--r--");
    private final String permissions;

    public SetPosixPermissions(String string) {
        this.permissions = string;
    }

    @Override
    public void apply(Map<File, Match<File, ?>> map, RenameAction renameAction, Feedback feedback) throws Exception {
        String string = this.getFilePermissions();
        String string2 = this.getDirectoryPermissions();
        MediaFileUtilities.walkStructureTree(map.keySet()).forEach(file -> {
            if (file.isDirectory()) {
                this.chmod((File)file, string2, feedback);
            } else {
                this.chmod((File)file, string, feedback);
            }
        });
    }

    private void chmod(File file, String string, Feedback feedback) {
        feedback.info(string, file);
        try {
            Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString(string));
        }
        catch (Exception exception) {
            feedback.warning(exception, file);
        }
    }

    public String getFilePermissions() {
        return this.permissions;
    }

    public String getDirectoryPermissions() {
        Set<PosixFilePermission> set = PosixFilePermissions.fromString(this.permissions);
        if (set.contains((Object)PosixFilePermission.OWNER_READ)) {
            set.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if (set.contains((Object)PosixFilePermission.GROUP_READ)) {
            set.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if (set.contains((Object)PosixFilePermission.OTHERS_READ)) {
            set.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        return PosixFilePermissions.toString(set);
    }
}


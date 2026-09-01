package net.filemaid;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Deployment {
    MSI,
    ZIP,
    PKG,
    APP,
    DEB,
    RPM,
    AUR,
    TAR,
    SPK,
    QPKG,
    APPX,
    MAS,
    SNAP,
    FLATPAK,
    UNRAID,
    DOCKER,
    JAR;


    public boolean is(Deployment ... deploymentArray) {
        for (Deployment deployment : deploymentArray) {
            if (this != deployment) continue;
            return true;
        }
        return false;
    }

    public static List<String> keys() {
        return Arrays.stream(Deployment.values()).map(Enum::name).collect(Collectors.toList());
    }

    public static Deployment get(String string) {
        for (Deployment deployment : Deployment.values()) {
            if (!deployment.name().equalsIgnoreCase(string)) continue;
            return deployment;
        }
        throw new IllegalArgumentException(string + " not in " + Deployment.keys());
    }
}


package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import net.filemaid.core.model.StorageRoot;
import org.junit.jupiter.api.Test;

class StoragePathPolicyTest {
    private final StoragePathPolicy policy = new StoragePathPolicy();
    private final StorageRoot root = new StorageRoot("media", Path.of("build/test-media"), false);

    @Test
    void resolvesRelativePathInsideRoot() {
        assertEquals(root.path().resolve("shows/Example"), policy.resolve(root, "shows/Example"));
    }

    @Test
    void rejectsParentTraversal() {
        assertThrows(IllegalArgumentException.class, () -> policy.resolve(root, "../secrets"));
    }

    @Test
    void rejectsAbsolutePath() {
        assertThrows(IllegalArgumentException.class, () -> policy.resolve(root, root.path().toString()));
    }
}

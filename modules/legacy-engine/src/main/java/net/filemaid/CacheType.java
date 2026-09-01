package net.filemaid;

import java.time.Duration;
import net.sf.ehcache.config.CacheConfiguration;

public enum CacheType {
    Persistent(Duration.ofDays(360L), 50),
    Monthly(Duration.ofDays(90L), 100),
    Daily(Duration.ofHours(18L), 200);

    private final long timeToLiveSeconds;
    private final int maxEntriesLocalHeap;

    private CacheType(Duration duration, int n2) {
        this.timeToLiveSeconds = duration.getSeconds();
        this.maxEntriesLocalHeap = n2;
    }

    public CacheConfiguration getConfiguration(String string) {
        return new CacheConfiguration().name(string).maxEntriesLocalHeap(this.maxEntriesLocalHeap).maxEntriesLocalDisk(0).eternal(false).timeToLiveSeconds(this.timeToLiveSeconds).timeToIdleSeconds(this.timeToLiveSeconds).overflowToDisk(true).diskPersistent(true);
    }
}


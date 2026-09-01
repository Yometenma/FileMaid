package net.filemaid.postprocess;

import java.io.File;
import java.net.URL;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.postprocess.ApplyStep;
import net.filemaid.postprocess.Feedback;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;

public interface ApplyMetadata
extends ApplyStep {
    @Override
    default public boolean accept(File file, Object object) {
        return object instanceof Movie || object instanceof Episode;
    }

    @Override
    default public void apply(File file, File file2, Object object, Feedback feedback) throws Exception {
        if (object instanceof Movie) {
            this.apply(file, file2, (Movie)object, feedback);
            return;
        }
        if (object instanceof Episode) {
            this.apply(file, file2, (Episode)object, feedback);
            return;
        }
    }

    public void apply(File var1, File var2, Movie var3, Feedback var4) throws Exception;

    public void apply(File var1, File var2, Episode var3, Feedback var4) throws Exception;

    default public byte[] cache(URL uRL) throws Exception {
        return Cache.getConcurrentCache("url", CacheType.Monthly).url(uRL).get();
    }
}


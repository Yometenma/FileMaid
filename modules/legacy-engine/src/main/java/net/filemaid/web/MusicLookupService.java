package net.filemaid.web;

import java.io.File;
import java.util.List;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.Datasource;

public interface MusicLookupService
extends Datasource {
    public List<AudioTrack> lookup(File var1) throws Exception;
}


package net.filemaid.web;

import java.nio.ByteBuffer;
import net.filemaid.vfs.FileInfo;

public interface SubtitleDescriptor
extends FileInfo {
    public String getLanguageName();

    public boolean isForced();

    public boolean isHI();

    public ByteBuffer fetch() throws Exception;
}


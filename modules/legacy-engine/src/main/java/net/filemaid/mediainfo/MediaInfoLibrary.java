package net.filemaid.mediainfo;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import java.lang.reflect.Method;
import java.util.Collections;

public interface MediaInfoLibrary
extends Library {
    public static final Library LIB_ZEN = Platform.isLinux() ? (Library)Native.loadLibrary((String)"zen", Library.class) : null;
    public static final MediaInfoLibrary INSTANCE = (MediaInfoLibrary)Native.loadLibrary((String)"mediainfo", MediaInfoLibrary.class, Collections.singletonMap("function-mapper", new FunctionMapper(){

        public String getFunctionName(NativeLibrary nativeLibrary, Method method) {
            return "MediaInfo_" + method.getName();
        }
    }));

    public Pointer New();

    public int Open(Pointer var1, WString var2);

    public WString Option(Pointer var1, WString var2, WString var3);

    public WString Inform(Pointer var1);

    public WString Get(Pointer var1, int var2, int var3, WString var4, int var5, int var6);

    public WString GetI(Pointer var1, int var2, int var3, int var4, int var5);

    public int Count_Get(Pointer var1, int var2, int var3);

    public void Close(Pointer var1);

    public void Delete(Pointer var1);

    public int Open_Buffer_Init(Pointer var1, long var2, long var4);

    public int Open_Buffer_Continue(Pointer var1, Pointer var2, int var3);

    public long Open_Buffer_Continue_GoTo_Get(Pointer var1);

    public int Open_Buffer_Finalize(Pointer var1);
}


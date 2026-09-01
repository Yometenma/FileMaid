package net.filemaid.platform.bsd;

import com.sun.jna.platform.unix.LibCAPI;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.filemaid.platform.bsd.ExtAttr;

public class ExtAttrUtil {
    public static List<String> list(String string) throws IOException {
        long l = ExtAttr.INSTANCE.extattr_list_file(string, 1, null, new LibCAPI.size_t(0L)).longValue();
        if (l <= 0L) {
            return Collections.emptyList();
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)l);
        long l2 = ExtAttr.INSTANCE.extattr_list_file(string, 1, byteBuffer, new LibCAPI.size_t(l)).longValue();
        if (l2 <= 0L) {
            return Collections.emptyList();
        }
        return ExtAttrUtil.decodeStringList(byteBuffer);
    }

    public static ByteBuffer get(String string, String string2) throws IOException {
        long l = ExtAttr.INSTANCE.extattr_get_file(string, 1, string2, null, new LibCAPI.size_t(0L)).longValue();
        if (l <= 0L) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)l);
        long l2 = ExtAttr.INSTANCE.extattr_get_file(string, 1, string2, byteBuffer, new LibCAPI.size_t(l)).longValue();
        if (l2 <= 0L) {
            return null;
        }
        return byteBuffer;
    }

    public static long set(String string, String string2, ByteBuffer byteBuffer) throws IOException {
        return ExtAttr.INSTANCE.extattr_set_file(string, 1, string2, byteBuffer, new LibCAPI.size_t((long)byteBuffer.remaining())).longValue();
    }

    public static int delete(String string, String string2) throws IOException {
        return ExtAttr.INSTANCE.extattr_delete_file(string, 1, string2);
    }

    private static List<String> decodeStringList(ByteBuffer byteBuffer) {
        ArrayList<String> arrayList = new ArrayList<String>();
        while (byteBuffer.hasRemaining()) {
            int n = byteBuffer.get() & 0xFF;
            byte[] byArray = new byte[n];
            byteBuffer.get(byArray);
            arrayList.add(new String(byArray, StandardCharsets.UTF_8));
        }
        return arrayList;
    }
}


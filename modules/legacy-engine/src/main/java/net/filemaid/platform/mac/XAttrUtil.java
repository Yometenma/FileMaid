package net.filemaid.platform.mac;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.filemaid.platform.mac.XAttr;

public class XAttrUtil {
    public static List<String> list(String string, int n) {
        long l = XAttr.INSTANCE.listxattr(string, null, 0L, n);
        if (l <= 0L) {
            return Collections.emptyList();
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)l);
        long l2 = XAttr.INSTANCE.listxattr(string, byteBuffer, l, n);
        if (l2 <= 0L) {
            return Collections.emptyList();
        }
        return XAttrUtil.decodeStringSequence(byteBuffer);
    }

    public static ByteBuffer get(String string, String string2, int n) {
        long l = XAttr.INSTANCE.getxattr(string, string2, null, 0L, 0, n);
        if (l <= 0L) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)l);
        long l2 = XAttr.INSTANCE.getxattr(string, string2, byteBuffer, l, 0, n);
        if (l2 <= 0L) {
            return null;
        }
        return byteBuffer;
    }

    public static int set(String string, String string2, ByteBuffer byteBuffer, int n) {
        return XAttr.INSTANCE.setxattr(string, string2, byteBuffer, byteBuffer.remaining(), 0, n);
    }

    public static int remove(String string, String string2, int n) {
        return XAttr.INSTANCE.removexattr(string, string2, n);
    }

    private static List<String> decodeStringSequence(ByteBuffer byteBuffer) {
        ArrayList<String> arrayList = new ArrayList<String>();
        byteBuffer.mark();
        while (byteBuffer.hasRemaining()) {
            if (byteBuffer.get() != 0) continue;
            ByteBuffer byteBuffer2 = byteBuffer.duplicate().reset().limit(byteBuffer.position() - 1);
            if (byteBuffer2.hasRemaining()) {
                arrayList.add(StandardCharsets.UTF_8.decode(byteBuffer2).toString());
            }
            byteBuffer.mark();
        }
        return arrayList;
    }
}


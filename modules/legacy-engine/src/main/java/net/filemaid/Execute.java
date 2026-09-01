package net.filemaid;

import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.CancellationException;
import net.filemaid.ExecuteException;
import net.filemaid.Logging;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.FileUtilities;

public class Execute {
    public static CharSequence execute(String string, String ... stringArray) throws IOException {
        return Execute.execute(string, Arrays.asList(stringArray), null, null, false);
    }

    public static CharSequence execute(String string, List<String> list, File file, Map<String, String> map, boolean bl) throws IOException {
        CharBuffer charBuffer;
        ProcessBuilder processBuilder = Execute.prepareProcessBuilder(string, list, file, map);
        if (bl) {
            processBuilder.redirectErrorStream(true);
        } else {
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        }
        Logging.debug.finest(Logging.format("Execute %s", processBuilder.command()));
        Process process = processBuilder.start();
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        try {
            byteBufferOutputStream.transferFully(process.getInputStream());
            int n = process.waitFor();
            CharBuffer charBuffer2 = StandardCharsets.UTF_8.decode(byteBufferOutputStream.getByteBuffer());
            Logging.debug.finest(charBuffer2::toString);
            if (n != 0) {
                throw new ExecuteException(processBuilder.command(), n);
            }
            charBuffer = charBuffer2;
        }
        catch (Throwable throwable) {
            try {
                try {
                    byteBufferOutputStream.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (InterruptedException interruptedException) {
                if (process.isAlive()) {
                    process.destroy();
                }
                throw new CancellationException(processBuilder.command() + " timed out");
            }
        }
        byteBufferOutputStream.close();
        return charBuffer;
    }

    public static void system(String string, String ... stringArray) throws IOException {
        Execute.system(string, Arrays.asList(stringArray), null, null);
    }

    public static void system(String string, List<String> list, File file, Map<String, String> map) throws IOException {
        ProcessBuilder processBuilder = Execute.prepareProcessBuilder(string, list, file, map);
        processBuilder.inheritIO();
        Logging.debug.finest(Logging.format("Execute %s", processBuilder.command()));
        Process process = processBuilder.start();
        try {
            int n = process.waitFor();
            if (n != 0) {
                throw new ExecuteException(processBuilder.command(), n);
            }
        }
        catch (InterruptedException interruptedException) {
            if (process.isAlive()) {
                process.destroy();
            }
            throw new CancellationException(processBuilder.command() + " timed out");
        }
    }

    private static ProcessBuilder prepareProcessBuilder(String string, List<String> list, File file, Map<String, String> map) {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.addAll(Execute.resolveCommand(string));
        arrayList.addAll(list);
        ProcessBuilder processBuilder = new ProcessBuilder(arrayList);
        if (file != null) {
            processBuilder.directory(file);
        }
        if (map != null) {
            processBuilder.environment().putAll(map);
        }
        return processBuilder;
    }

    private static List<String> resolveCommand(String string) {
        if (FileUtilities.UNIX) {
            return Arrays.asList(string);
        }
        File file = new File(string);
        if (file.isAbsolute()) {
            return Execute.command(file.getAbsolutePath());
        }
        StringTokenizer stringTokenizer = new StringTokenizer(System.getenv("PATH"), File.pathSeparator);
        while (stringTokenizer.hasMoreTokens()) {
            File file2 = new File(stringTokenizer.nextToken());
            if (!file2.isDirectory()) continue;
            for (String string2 : file2.list()) {
                if (!string2.equalsIgnoreCase(string) && (!FileUtilities.hasExtension(string2, "ps1", "cmd", "bat", "exe") || !FileUtilities.getNameWithoutExtension(string2).equalsIgnoreCase(string))) continue;
                return Execute.command(new File(file2, string2).getAbsolutePath());
            }
        }
        return Execute.command(string);
    }

    private static List<String> command(String string) {
        if (FileUtilities.hasExtension(string, "ps1")) {
            return Arrays.asList("powershell", "-NonInteractive", "-NoProfile", "-NoLogo", "-ExecutionPolicy", "Bypass", "-File", string);
        }
        return Arrays.asList(string);
    }

    public static CharSequence powershell(String string) throws IOException {
        return Execute.execute("powershell", "-NonInteractive", "-NoProfile", "-NoLogo", "-ExecutionPolicy", "Bypass", "-Command", string);
    }
}


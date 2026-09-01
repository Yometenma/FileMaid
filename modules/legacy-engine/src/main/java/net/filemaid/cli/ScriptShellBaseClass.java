package net.filemaid.cli;

import com.sun.jna.Platform;
import groovy.lang.Closure;
import groovy.lang.Script;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import net.filemaid.Cache;
import net.filemaid.HistorySpooler;
import net.filemaid.Logging;
import net.filemaid.Parallelism;
import net.filemaid.RenameAction;
import net.filemaid.StandardPostProcessAction;
import net.filemaid.StandardRenameAction;
import net.filemaid.WebServices;
import net.filemaid.cli.ArgumentBean;
import net.filemaid.cli.CmdlineException;
import net.filemaid.cli.CmdlineInterface;
import net.filemaid.cli.CmdlineOperationsTextUI;
import net.filemaid.cli.ExecCommand;
import net.filemaid.cli.ExecutableRenameAction;
import net.filemaid.cli.GroovyAction;
import net.filemaid.cli.PseudoConsole;
import net.filemaid.cli.ScriptDeath;
import net.filemaid.cli.ScriptShell;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.ExpressionFormatFunctions;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.format.SuppressedThrowables;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.ApplyClosure;
import net.filemaid.similarity.SeasonEpisodeMatcher;
import net.filemaid.util.Builder;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.TemporaryFolder;
import net.filemaid.web.Movie;
import net.filemaid.web.Series;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

public abstract class ScriptShellBaseClass
extends Script {
    private ArgumentBean getArgumentBean() {
        return (ArgumentBean)this.getBinding().getVariable("__args");
    }

    private Map getDefines() {
        return (Map)this.getBinding().getVariable("__defs");
    }

    private ScriptShell getShell() {
        return (ScriptShell)this.getBinding().getVariable("__shell");
    }

    private CmdlineInterface getCLI() {
        return (CmdlineInterface)this.getBinding().getVariable("__cli");
    }

    public void runScript(String string, String ... stringArray) throws Throwable {
        try {
            ArgumentBean argumentBean = stringArray == null || stringArray.length == 0 ? this.getArgumentBean() : new ArgumentBean(stringArray, false);
            this.executeScript(string, argumentBean, argumentBean.defines, argumentBean.getFileArguments());
        }
        catch (Exception exception) {
            this.handleException(exception);
        }
    }

    public Object include(String string) throws Throwable {
        try {
            return this.executeScript(string, null, null, null);
        }
        catch (Exception exception) {
            this.handleException(exception);
            return null;
        }
    }

    public Object executeScript(String string, List list) throws Throwable {
        return this.executeScript(string, this.getArgumentBean(), this.getDefines(), FileUtilities.asFileList(list));
    }

    public Object executeScript(String string, Map map, List list) throws Throwable {
        return this.executeScript(string, this.getArgumentBean(), map, FileUtilities.asFileList(list));
    }

    private Object executeScript(String string, ArgumentBean argumentBean, Map map, List<File> list) throws Throwable {
        SimpleBindings simpleBindings = new SimpleBindings();
        if (map != null) {
            simpleBindings.putAll(map);
        }
        simpleBindings.put("__args", (Object)(argumentBean != null ? argumentBean : new ArgumentBean()));
        simpleBindings.put("__defs", (Object)(map != null ? map : Collections.emptyMap()));
        simpleBindings.put("args", (Object)(list != null ? list : Collections.emptyList()));
        return this.getShell().runScript(string, simpleBindings);
    }

    public List<?> parallel(Closure<?> ... closureArray) throws Exception {
        return Parallelism.commonPool().map(Arrays.asList(closureArray), Closure::call);
    }

    public List<?> parallel(Collection<Closure<?>> collection) throws Exception {
        return Parallelism.commonPool().map(collection, Closure::call);
    }

    public List<?> parallel(Collection<?> collection, Closure<?> closure) throws Exception {
        return Parallelism.commonPool().map(collection, arg_0 -> closure.call(arg_0));
    }

    public Object tryQuietly(Closure<?> closure) {
        try {
            return closure.call();
        }
        catch (Exception exception) {
            return null;
        }
    }

    public Object tryLogCatch(Closure<?> closure) {
        try {
            return closure.call();
        }
        catch (Exception exception) {
            this.handleException(exception);
            return null;
        }
    }

    private void handleException(Throwable throwable) {
        ScriptException scriptException = Logging.findCause(throwable, ScriptException.class);
        if (scriptException != null) {
            MultipleCompilationErrorsException multipleCompilationErrorsException = Logging.findCause(scriptException, MultipleCompilationErrorsException.class);
            throw new CmdlineException("Script Error", multipleCompilationErrorsException != null ? multipleCompilationErrorsException.getMessage() : scriptException.getMessage(), scriptException);
        }
        Logging.log.warning(Logging.cause(throwable));
        Logging.debug.log(Level.ALL, throwable, Logging.cause(throwable));
    }

    public void die(Object object) throws Throwable {
        this.die(object, 4);
    }

    public void die(Object object, int n) throws Throwable {
        throw new ScriptDeath(n, String.valueOf(object));
    }

    public ArgumentBean get_args() {
        return this.getArgumentBean();
    }

    public Map get_def() {
        return Collections.unmodifiableMap(this.getDefines());
    }

    public Map get_system() {
        return Collections.unmodifiableMap(System.getProperties());
    }

    public Map get_environment() {
        return Collections.unmodifiableMap(System.getenv());
    }

    public Number get_build() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = ScriptShellBaseClass.class.getProtectionDomain().getCodeSource().getLocation().openStream();){
                byte[] byArray = new byte[65536];
                int n = 0;
                while ((n = inputStream.read(byArray)) >= 0) {
                    messageDigest.update(byArray, 0, n);
                }
            }
            return new BigInteger(1, messageDigest.digest());
        }
        catch (Exception exception) {
            return null;
        }
    }

    public Map<File, File> getRenameLog() throws IOException {
        return HistorySpooler.HISTORY.getSessionHistory().getRenameMap();
    }

    public Map<File, File> getPersistentRenameLog() throws IOException {
        return HistorySpooler.HISTORY.getCompleteHistory().getRenameMap();
    }

    public void commit() {
        HistorySpooler.HISTORY.commit();
        Cache.DISK_STORE.flush();
        Logging.flushLog();
    }

    public Logger getLog() {
        return Logging.log;
    }

    public Object getConsole() {
        return System.console() != null ? System.console() : PseudoConsole.getSystemConsole();
    }

    public Date getNow() {
        return new Date();
    }

    public TemporaryFolder getTemporaryFolder(String string) {
        return TemporaryFolder.getFolder(string);
    }

    public void help(Object object) {
        Logging.log.log(Level.ALL, object::toString);
    }

    public Object run() {
        return null;
    }

    public String getMediaInfo(File file, String string) throws Exception {
        ExpressionFormat expressionFormat = new ExpressionFormat(string);
        try {
            return expressionFormat.format(new MediaBindingBean(XattrMetaInfo.xattr.getMetaInfo(file), file));
        }
        catch (SuppressedThrowables suppressedThrowables) {
            Logging.debug.finest(Logging.cause(file, string, suppressedThrowables));
            return null;
        }
    }

    public Series detectSeries(Object object) throws Exception {
        return this.detectSeries(object, false);
    }

    public Series detectAnime(Object object) throws Exception {
        return this.detectSeries(object, true);
    }

    public Series detectSeries(Object object, boolean bl) throws Exception {
        List<File> list = FileUtilities.asFileList(object);
        if (list.isEmpty()) {
            return null;
        }
        return MediaDetection.detectSeries(list, bl, Locale.US).stream().findFirst().orElse(null);
    }

    public SeasonEpisodeMatcher.SxE parseEpisodeNumber(Object object) {
        List<SeasonEpisodeMatcher.SxE> list = MediaDetection.parseEpisodeNumber(object.toString(), true);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public Movie detectMovie(File file, boolean bl) {
        List<Movie> list;
        Object object = XattrMetaInfo.xattr.getMetaInfo(file);
        if (object instanceof Movie) {
            return (Movie)object;
        }
        try {
            Movie movie = MediaDetection.matchMovie(file, 4);
            if (movie != null) {
                return movie;
            }
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        try {
            list = MediaDetection.detectMovieWithYear(file, WebServices.TheMovieDB, Locale.US, bl);
            if (bl) {
                list = MediaDetection.matchMovieByFileFolderName(file, (Collection<Movie>)list);
            }
            if (list != null && list.size() > 0) {
                return list.get(0);
            }
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        return null;
    }

    public Movie matchMovie(String string) {
        List<Movie> list = MediaDetection.matchMovieName(Collections.singleton(string), true, 0);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public int execute(Object ... objectArray) throws Exception {
        Stream<String> stream = Arrays.stream(objectArray).filter(Objects::nonNull).map(Objects::toString);
        if (Platform.isWindows()) {
            stream = objectArray.length == 1 ? Stream.concat(Stream.of("powershell", "-NonInteractive", "-NoProfile", "-NoLogo", "-ExecutionPolicy", "Bypass", "-Command"), stream) : Stream.concat(Stream.of("powershell", "-NonInteractive", "-NoProfile", "-NoLogo", "-ExecutionPolicy", "Bypass", "-Command", "&"), stream.map(string -> ExpressionFormatFunctions.quotePowerShell(this, string, new Object[0])));
        } else if (objectArray.length == 1) {
            stream = Stream.concat(Stream.of("sh", "-c"), stream);
        }
        ProcessBuilder processBuilder = new ProcessBuilder(stream.collect(Collectors.toList())).inheritIO();
        Logging.debug.finest(Logging.format("Execute %s", processBuilder.command()));
        return processBuilder.start().waitFor();
    }

    public File XML(File file, Closure closure) throws Exception {
        return FileUtilities.writeFile(this.XML(closure).getBytes(StandardCharsets.UTF_8), file);
    }

    public String XML(Closure<?> closure) {
        return Builder.XML.toString(closure);
    }

    public void telnet(String string, int n, Closure<?> closure) throws IOException {
        try (Socket socket = new Socket(string, n);){
            closure.call(new Object[]{new PrintStream(socket.getOutputStream(), true, "UTF-8"), new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))});
        }
    }

    public void wol(String string2) throws IOException {
        int n;
        int[] nArray = Pattern.compile("[:-]").splitAsStream(string2).mapToInt(string -> Integer.parseInt(string, 16)).toArray();
        byte[] byArray = new byte[102];
        for (n = 0; n < 6; ++n) {
            byArray[n] = -1;
        }
        for (n = 0; n < 16; ++n) {
            for (int i = 0; i < nArray.length; ++i) {
                byArray[6 + n * nArray.length + i] = (byte)nArray[i];
            }
        }
        try (DatagramSocket datagramSocket = new DatagramSocket();){
            datagramSocket.send(new DatagramPacket(byArray, byArray.length, InetAddress.getByName("255.255.255.255"), 9));
        }
    }

    public Object retry(int n, int n2, Closure<?> closure) throws InterruptedException {
        for (int i = 0; n < 0 || i <= n; ++i) {
            try {
                return closure.call();
            }
            catch (Exception exception) {
                if (i >= 0 && i >= n) {
                    throw exception;
                }
                Thread.sleep(n2);
                continue;
            }
        }
        return null;
    }

    public List<File> rename(Map<String, ?> map) throws Exception {
        List<File> list = this.getInputFileList(map);
        List<File> list2 = Collections.emptyList();
        Map<File, File> map2 = Collections.emptyMap();
        if (list.isEmpty() && (map2 = this.getInputFileMap(map)).isEmpty()) {
            list2 = this.consumeInputFileList(map, "list").findFirst().orElse(Collections.emptyList());
        }
        RenameAction renameAction = this.getRenameAction(map);
        Apply[] applyArray = this.getApplyActions(map);
        ArgumentBean argumentBean = this.getArgumentBean(map);
        try {
            if (list.size() > 0) {
                return this.getCLI().rename(list, argumentBean.getDatasource(), argumentBean.getQueryExpression(), argumentBean.getSortOrder(), argumentBean.getLanguage().getLocale(), argumentBean.getExpressionFilter(), argumentBean.getExpressionMapper(), argumentBean.isStrict(), argumentBean.getExpressionFileFormat(), argumentBean.getAbsoluteOutputFolder(), renameAction, argumentBean.getConflictAction(), applyArray, argumentBean.getExecCommand());
            }
            if (map2.size() > 0) {
                return this.getCLI().rename(map2, renameAction, argumentBean.getConflictAction());
            }
            if (list2.size() > 0) {
                return this.getCLI().renameLinear(list2, argumentBean.getDatasource(), argumentBean.getQueryExpression(), argumentBean.getSortOrder(), argumentBean.getLanguage().getLocale(), argumentBean.getExpressionFilter(), argumentBean.getExpressionMapper(), argumentBean.getExpressionFileFormat(), argumentBean.getAbsoluteOutputFolder(), renameAction, argumentBean.getConflictAction(), applyArray, argumentBean.getExecCommand());
            }
        }
        catch (Exception exception) {
            this.handleException(exception);
        }
        return null;
    }

    public List<File> getSubtitles(Map<String, ?> map) throws Exception {
        List<File> list = this.getInputFileList(map);
        ArgumentBean argumentBean = this.getArgumentBean(map);
        try {
            return this.getCLI().getSubtitles(list, argumentBean.getQueryExpression(), argumentBean.getLanguage(), argumentBean.getSubtitleOutputFormat(), argumentBean.getEncoding(), argumentBean.getSubtitleNamingFormat(), argumentBean.isStrict());
        }
        catch (Exception exception) {
            this.handleException(exception);
            return null;
        }
    }

    public List<File> getMissingSubtitles(Map<String, ?> map) throws Exception {
        List<File> list = this.getInputFileList(map);
        ArgumentBean argumentBean = this.getArgumentBean(map);
        try {
            return this.getCLI().getMissingSubtitles(list, argumentBean.getQueryExpression(), argumentBean.getLanguage(), argumentBean.getSubtitleOutputFormat(), argumentBean.getEncoding(), argumentBean.getSubtitleNamingFormat(), argumentBean.isStrict());
        }
        catch (Exception exception) {
            this.handleException(exception);
            return null;
        }
    }

    public boolean check(Map<String, ?> map) throws Exception {
        List<File> list = this.getInputFileList(map);
        try {
            this.getCLI().check(list);
            return true;
        }
        catch (Exception exception) {
            this.handleException(exception);
            return false;
        }
    }

    public File compute(Map<String, ?> map) throws Exception {
        List<File> list = this.getInputFileList(map);
        ArgumentBean argumentBean = this.getArgumentBean(map);
        try {
            return this.getCLI().compute(list, argumentBean.getOutputHashType(), argumentBean.getOutputPath(), argumentBean.getEncoding());
        }
        catch (Exception exception) {
            this.handleException(exception);
            return null;
        }
    }

    public List<File> extract(Map<String, ?> map) throws Exception {
        List<File> list = this.getInputFileList(map);
        FileFilter fileFilter = this.getFileFilter(map);
        ArgumentBean argumentBean = this.getArgumentBean(map);
        try {
            return this.getCLI().extract(list, argumentBean.getOutputPath(), fileFilter, argumentBean.isStrict());
        }
        catch (Exception exception) {
            this.handleException(exception);
            return null;
        }
    }

    public File zip(Map<String, ?> map) throws Exception {
        List<File> list = this.getInputFileList(map);
        ArgumentBean argumentBean = this.getArgumentBean(map);
        try {
            return this.getCLI().zip(list, argumentBean.getOutputPath(), argumentBean.getExpressionFileFilter());
        }
        catch (Exception exception) {
            this.handleException(exception);
            return null;
        }
    }

    public List<String> list(Map<String, ?> map) throws Exception {
        ArgumentBean argumentBean = this.getArgumentBean(map);
        try {
            return this.getCLI().list(argumentBean.getDatasource(), argumentBean.getQueryExpression(), argumentBean.getSortOrder(), argumentBean.getLanguage().getLocale(), argumentBean.getExpressionFilter(), argumentBean.getExpressionMapper(), argumentBean.getExpressionFormat(), argumentBean.isStrict()).collect(Collectors.toList());
        }
        catch (Exception exception) {
            this.handleException(exception);
            return null;
        }
    }

    public Object getMediaInfo(Map<String, ?> map) throws Exception {
        List<File> list = this.getInputFileList(map);
        ArgumentBean argumentBean = this.getArgumentBean(map);
        Apply[] applyArray = this.getApplyActions(map);
        ExecCommand execCommand = argumentBean.getExecCommand();
        try {
            if (applyArray != null || execCommand != null) {
                return this.getCLI().execute(list, argumentBean.getExpressionFileFilter(), argumentBean.getExpressionFormat(), applyArray, execCommand).allMatch(n -> n == 0) ? 0 : 1;
            }
            return this.getCLI().getMediaInfo(list, argumentBean.getExpressionFileFilter(), argumentBean.getExpressionFormat()).peek(Logging.stdout).collect(Collectors.toList());
        }
        catch (Exception exception) {
            this.handleException(exception);
            return null;
        }
    }

    private ArgumentBean getArgumentBean(Map<String, ?> map) throws Exception {
        ArgumentBean argumentBean = new ArgumentBean(this.getArgumentBean().getArgumentArray(), false);
        Stream.of("forceExtractAll", "strict").map(map::remove).filter(Objects::nonNull).forEach(object -> {
            argumentBean.nonStrict = !DefaultTypeTransformation.castToBoolean((Object)object);
        });
        map.forEach((string, object) -> {
            try {
                Field field = argumentBean.getClass().getField((String)string);
                Object object2 = DefaultTypeTransformation.castToType((Object)object, field.getType());
                field.set(argumentBean, object2);
            }
            catch (Exception exception) {
                throw new IllegalArgumentException("Illegal parameter: " + string, exception);
            }
        });
        return argumentBean;
    }

    private List<File> getInputFileList(Map<String, ?> map) {
        return this.consumeInputFileList(map, "file").findFirst().orElseGet(() -> this.consumeInputFileList(map, "folder").flatMap(list -> FileUtilities.listFiles(list, FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER, File::isDirectory, 0).stream()).collect(Collectors.toList()));
    }

    private Map<File, File> getInputFileMap(Map<String, ?> map) {
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>();
        this.consumeParameter(map, "map").map(Map.class::cast).forEach(map2 -> map2.forEach((object, object2) -> {
            File file = new File(object.toString());
            File file2 = new File(object2.toString());
            linkedHashMap.put(file, file2);
        }));
        return linkedHashMap;
    }

    private RenameAction getRenameAction(Map<String, ?> map) throws Exception {
        return this.consumeParameter(map, "action").map(object -> this.getRenameAction(object)).findFirst().orElse(this.getArgumentBean().getRenameAction());
    }

    private FileFilter getFileFilter(Map<String, ?> map) {
        return this.consumeParameter(map, "filter").map(object -> (FileFilter)DefaultTypeTransformation.castToType((Object)object, FileFilter.class)).findFirst().orElse(null);
    }

    private Apply[] getApplyActions(Map<String, ?> map) throws Exception {
        return this.consumeParameter(map, "apply").map(object -> {
            if (object instanceof Collection) {
                return (Apply[])((Collection)object).stream().map(this::getApplyAction).toArray(Apply[]::new);
            }
            return new Apply[]{this.getApplyAction(object)};
        }).findFirst().orElse(this.getArgumentBean().getPostProcessActions());
    }

    private Stream<List<File>> consumeInputFileList(Map<String, ?> map, String ... stringArray) {
        return this.consumeParameter(map, stringArray).map(object -> FileUtilities.asFileList(object));
    }

    private Stream<?> consumeParameter(Map<String, ?> map, String ... stringArray) {
        return Stream.of(stringArray).map(map::remove).filter(Objects::nonNull);
    }

    public RenameAction getRenameAction(Object object) {
        if (object instanceof RenameAction) {
            return (RenameAction)object;
        }
        if (object instanceof CharSequence) {
            return StandardRenameAction.forName(object.toString());
        }
        if (object instanceof File) {
            return ExecutableRenameAction.executable((File)object, this.getArgumentBean().getOutputPath());
        }
        if (object instanceof Closure) {
            return GroovyAction.wrap((Closure)object);
        }
        return (RenameAction)DefaultTypeTransformation.castToType((Object)object, RenameAction.class);
    }

    public Apply getApplyAction(Object object) {
        if (object instanceof Apply) {
            return (Apply)object;
        }
        if (object instanceof CharSequence) {
            return StandardPostProcessAction.forName(object.toString());
        }
        if (object instanceof Closure) {
            return new ApplyClosure((Closure)object);
        }
        return (Apply)DefaultTypeTransformation.castToType((Object)object, Apply.class);
    }

    public <T> T showInputDialog(Collection<T> collection, String string, String string2) throws Exception {
        if (collection.isEmpty()) {
            return null;
        }
        if (this.getCLI() instanceof CmdlineOperationsTextUI) {
            CmdlineOperationsTextUI cmdlineOperationsTextUI = (CmdlineOperationsTextUI)this.getCLI();
            return cmdlineOperationsTextUI.showInputDialog(collection, string, string2);
        }
        return collection.iterator().next();
    }
}


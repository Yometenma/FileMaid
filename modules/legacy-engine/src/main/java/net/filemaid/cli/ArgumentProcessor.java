package net.filemaid.cli;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import net.filemaid.ExitCode;
import net.filemaid.HistorySpooler;
import net.filemaid.LicenseError;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.Parallelism;
import net.filemaid.Settings;
import net.filemaid.UserData;
import net.filemaid.cli.ArgumentBean;
import net.filemaid.cli.CmdlineException;
import net.filemaid.cli.CmdlineInterface;
import net.filemaid.cli.CmdlineOperations;
import net.filemaid.cli.CmdlineOperationsTextUI;
import net.filemaid.cli.ConfigurationException;
import net.filemaid.cli.ExecCommand;
import net.filemaid.cli.ScriptDeath;
import net.filemaid.cli.ScriptShell;
import net.filemaid.cli.ScriptSource;
import net.filemaid.format.ExpressionException;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Delete;
import net.filemaid.util.FileUtilities;
import org.apache.commons.io.IOUtils;

public class ArgumentProcessor {
    private final ArgumentBean args;

    public ArgumentProcessor(ArgumentBean argumentBean) {
        this.args = argumentBean;
    }

    public int run() {
        try {
            this.checkEnvironment();
            this.checkArgumentBean();
            if (this.args.script == null) {
                return this.runCommand();
            }
            this.runScript();
            return this.finish(0);
        }
        catch (Throwable throwable) {
            return this.finish(this.getExitCode(throwable));
        }
    }

    private int finish(int n) {
        Logging.log.finest(Logging.format("%s %s", ExitCode.getErrorMessage(n), ExitCode.getErrorKaomoji(n)));
        return n;
    }

    private int getExitCode(Throwable throwable) {
        if (Logging.findCause(throwable, ScriptException.class) != null) {
            Logging.help(ArgumentProcessor.formatArgumentHelp(this.args.getArgumentArray(), false));
        }
        if (Logging.findCause(throwable, LicenseError.class) != null) {
            LicenseError licenseError = Logging.findCause(throwable, LicenseError.class);
            Logging.log.severe(Logging.message("License Error", licenseError.getMessage()));
            if (Settings.LICENSE.isFile() && licenseError.getCause() == null) {
                ArgumentProcessor.printPurchaseHelp();
            }
            return 2;
        }
        if (Logging.findCause(throwable, ConfigurationException.class) != null) {
            ConfigurationException configurationException = Logging.findCause(throwable, ConfigurationException.class);
            Logging.log.warning(configurationException::getMessage);
            ArgumentProcessor.printConfigureHelp();
            return 3;
        }
        if (Logging.findCause(throwable, CmdlineException.class) != null) {
            CmdlineException cmdlineException = Logging.findCause(throwable, CmdlineException.class);
            Logging.log.warning(cmdlineException::getMessage);
            return 3;
        }
        if (Logging.findCause(throwable, ScriptDeath.class) != null) {
            ScriptDeath scriptDeath = Logging.findCause(throwable, ScriptDeath.class);
            Logging.log.warning(scriptDeath::getMessage);
            return scriptDeath.getExitCode();
        }
        if (Logging.findCause(throwable, ExpressionException.class) != null) {
            ExpressionException expressionException = Logging.findCause(throwable, ExpressionException.class);
            Logging.log.severe(expressionException::getMessage);
            if (expressionException.getExpression() != null) {
                Logging.help(ArgumentProcessor.formatExpressionHelp(expressionException.getExpression()));
            }
            return 1;
        }
        Logging.trace(throwable);
        return 1;
    }

    public int runCommand() throws Exception {
        List<File> list;
        CmdlineInterface cmdlineInterface = this.getCLI();
        if (this.args.revert) {
            return cmdlineInterface.revert(this.getInputArgumentList(), this.args.getExpressionFileFilter(), this.args.getExpressionFileFormat(), this.args.getAbsoluteOutputFolder(), this.args.getRenameAction()).size() > 0 ? 0 : 1;
        }
        if (this.args.list && !this.args.rename) {
            return this.print(cmdlineInterface.list(this.args.getDatasource(), this.args.getQueryExpression(), this.args.getSortOrder(), this.args.getLanguage().getLocale(), this.args.getExpressionFilter(), this.args.getExpressionMapper(), this.args.getExpressionFormat(), this.args.isStrict()));
        }
        List<File> list2 = this.getInputFileSet();
        if (this.args.find || this.args.mediaInfo) {
            ExpressionFormat expressionFormat = this.args.find ? new ExpressionFormat("{f}") : this.args.getExpressionFormat();
            Apply[] applyArray = this.args.getPostProcessActions();
            ExecCommand execCommand = this.args.getExecCommand();
            if (applyArray != null || execCommand != null) {
                if (this.args.isStrict()) {
                    return cmdlineInterface.execute(list2, this.args.getExpressionFileFilter(), expressionFormat, applyArray, execCommand).allMatch(n -> n == 0) ? 0 : 1;
                }
                return cmdlineInterface.execute(list2, this.args.getExpressionFileFilter(), expressionFormat, applyArray, execCommand).filter(n -> n == 0).count() > 0L ? 0 : 1;
            }
            return this.print(cmdlineInterface.getMediaInfo(list2, this.args.getExpressionFileFilter(), expressionFormat));
        }
        if (this.args.list && this.args.rename) {
            return cmdlineInterface.renameLinear(list2, this.args.getDatasource(), this.args.getQueryExpression(), this.args.getSortOrder(), this.args.getLanguage().getLocale(), this.args.getExpressionFilter(), this.args.getExpressionMapper(), this.args.getExpressionFileFormat(), this.args.getAbsoluteOutputFolder(), this.args.getRenameAction(), this.args.getConflictAction(), this.args.getPostProcessActions(), this.args.getExecCommand()).size() > 0 ? 0 : 1;
        }
        if (this.args.extract) {
            list2.addAll(cmdlineInterface.extract(list2, this.args.getOutputPath(), this.args.getExpressionFileFilter(), this.args.isStrict()));
        }
        if (this.args.check && !FileUtilities.containsOnly(list2, FileUtilities.not(MediaTypes.VERIFICATION_FILES))) {
            cmdlineInterface.check(list2);
        }
        if (this.args.getSubtitles) {
            list = cmdlineInterface.getMissingSubtitles(list2, this.args.getQueryExpression(), this.args.getLanguage(), this.args.getSubtitleOutputFormat(), this.args.getEncoding(), this.args.getSubtitleNamingFormat(), this.args.isStrict());
            if (list != null && !list.isEmpty()) {
                list2 = Stream.concat(list2.stream(), list.stream()).collect(Collectors.toList());
            }
            if (list != null && list.isEmpty() && this.args.isStrict()) {
                Logging.help("* Consider using -non-strict to enable fuzzy search");
            }
        }
        if (this.args.rename) {
            list2 = cmdlineInterface.rename(list2, this.args.getDatasource(), this.args.getQueryExpression(), this.args.getSortOrder(), this.args.getLanguage().getLocale(), this.args.getExpressionFilter(), this.args.getExpressionMapper(), this.args.isStrict(), this.args.getExpressionFileFormat(), this.args.getAbsoluteOutputFolder(), this.args.getRenameAction(), this.args.getConflictAction(), this.args.getPostProcessActions(), this.args.getExecCommand());
        }
        if (this.args.check && !list2.isEmpty() && !FileUtilities.containsOnly(list2, (FileFilter)MediaTypes.VERIFICATION_FILES)) {
            cmdlineInterface.compute(list2, this.args.getOutputHashType(), this.args.getOutputPath(), this.args.getEncoding());
        }
        if (Logging.help() && this.args.rename && this.args.apply.isEmpty()) {
            Map<File, File> renameMap = HistorySpooler.HISTORY.getSessionHistory().getRenameMap();
            File[] fileArray = renameMap.keySet().toArray(new File[0]);
            File[] fileArray2 = renameMap.values().toArray(new File[0]);
            List<File> list3 = FileUtilities.filter(MediaFileUtilities.getRemainingEmptyFolders(fileArray, fileArray2, Delete.EMPTY_FOLDERS::isHidden), File::isDirectory);
            if (list3.size() > 0) {
                Logging.help("* Consider using --apply prune to delete left-behind empty folders:");
                list3.forEach(file -> Logging.help(Logging.format("* %s", file)));
            }
        }
        return list2.size() > 0 ? 0 : 1;
    }

    private int print(Stream<?> stream) {
        return stream.peek(Logging.stdout).mapToInt(object -> 1).sum() > 0 ? 0 : 1;
    }

    public void runScript() throws Throwable {
        ScriptSource scriptSource = ScriptSource.findScriptProvider(this.args.script);
        ScriptShell scriptShell = new ScriptShell(scriptSource.getResolver(this.args.script), this.getCLI(), this.args.defines);
        Logging.debug.info(scriptSource::getManifest);
        SimpleBindings simpleBindings = new SimpleBindings();
        simpleBindings.put("__args", (Object)this.args);
        simpleBindings.put("__defs", (Object)this.args.defines);
        simpleBindings.put("args", (Object)this.getInputArgumentList());
        scriptShell.evaluate(scriptSource.getScript(this.args.script), simpleBindings);
    }

    private CmdlineInterface getCLI() throws Exception {
        return this.args.isInteractive() ? new CmdlineOperationsTextUI() : new CmdlineOperations();
    }

    private List<File> getInputFileSet() throws Exception {
        List<File> list = this.args.getFiles();
        this.checkInputArguments(list);
        if (!(!this.args.check || this.args.rename || list.isEmpty() || FileUtilities.containsOnly(list, (FileFilter)MediaTypes.VERIFICATION_FILES) || FileUtilities.containsOnly(list, FileUtilities.not(MediaTypes.VERIFICATION_FILES)))) {
            Logging.help("* Consider using --file-filter \"f.video\" to compute checksums for video files only.");
            Logging.help("* Consider using --file-filter \"f.verification\" to check verification files only.");
        }
        return list.stream().filter(file -> {
            if (!file.exists()) {
                Logging.help(Logging.message("File does not exist", file));
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private List<File> getInputArgumentList() throws Exception {
        List<File> list = this.args.getFileArguments();
        if (list.isEmpty() && !this.args.arguments.isEmpty()) {
            this.checkInputArguments(list);
        }
        return list;
    }

    private void checkInputArguments(List<File> list) {
        if (list.isEmpty()) {
            if (this.args.arguments.isEmpty()) {
                Logging.help("No input arguments");
                return;
            }
            Logging.help(Logging.message("No input files", this.args.arguments));
            if (!this.args.recursive) {
                Logging.help("* Consider using -r to select files recursively");
            }
            if (this.args.inputFileFilter != null) {
                Logging.help("* Consider using --file-filter \"true\" to select all files");
            }
            if (Settings.isLinuxContainer()) {
                Logging.help("* This process is running within a confined sandbox. Access to the host file system may be restricted.");
            }
        }
    }

    private void checkEnvironment() {
        int n = Parallelism.THREAD_POOL_PRIORITY.max();
        if (n != 5) {
            Thread.currentThread().setPriority(n);
        }
        if (Settings.isLinuxApp()) {
            long l;
            String string = System.getProperty("sun.jnu.encoding");
            if (!"UTF-8".equals(string)) {
                Logging.log.warning(Logging.message("Bad system encoding", string));
                Logging.help("* Please configure your locale with LANG=C.UTF-8 or LANG=en_US.UTF-8");
            }
            if ((l = Runtime.getRuntime().maxMemory()) < 384000000L && !this.args.disableMediaIndex) {
                Logging.log.warning(Logging.message("Low Memory Limit", FileUtilities.formatSize(l)));
                Logging.help("* Consider using JAVA_OPTS=\"-Xmx384m\" or higher");
                Logging.help("* Consider using -no-index on low-memory devices");
            } else if (l > 64000000000L) {
                Logging.log.warning(Logging.message("Excessive Memory Limit", FileUtilities.formatSize(l)));
                Logging.help("* Consider using JAVA_OPTS=\"-Xmx768m -XX:ActiveProcessorCount=1\"");
            }
        }
    }

    private void checkArgumentBean() {
        if (this.args.isInteractive() && System.console() == null) {
            throw new CmdlineException("--mode interactive requires an interactive terminal");
        }
        if (this.args.arguments.size() > 0) {
            this.args.arguments.stream().map(File::new).filter(FileUtilities.getFileSystemRoots()::contains).forEach(file -> Logging.help(Logging.format("* Passing the file system root %s as input argument is extremely dangerous. I hope you know what you're doing.", file)));
        }
        if (this.args.script == null && this.args.defines.size() > 0) {
            this.args.defines.forEach((string, string2) -> Logging.help(Logging.format("* This is not a -script call. The script parameter --def %s has no effect.", string)));
        }
        if (this.args.revert && this.args.arguments.isEmpty() && System.console() != null && !this.args.isInteractive()) {
            Logging.help("* Consider using --mode interactive to enable interactive mode");
        }
        if (this.args.rename && "test".equalsIgnoreCase(this.args.action) && System.console() != null && !this.args.isInteractive()) {
            Logging.help("* Consider using --mode interactive to enable interactive mode");
        }
        if (this.args.rename && this.args.apply.isEmpty() && Settings.isNAS()) {
            Logging.help("* Consider using --apply refresh to refresh file services and media library");
        }
    }

    private static void printPurchaseHelp() {
        Logging.log.info(ArgumentProcessor.formatStegosaurus("Please purchase a FileBot License:", Settings.getApplicationProperty("link.app.purchase") + "#" + Settings.getApplicationDeployment()));
        Logging.log.severe(Logging.format("FileBot requires a valid license. Please run `filebot --license *.psm` to install your FileBot license.", new Object[0]));
        ArgumentProcessor.printUserHelp("filebot --license *.psm", UserData.getLicenseFile());
    }

    private static void printConfigureHelp() {
        Logging.log.info(Logging.format("Please enter your login details by running `filebot -script fn:configure`", new Object[0]));
        ArgumentProcessor.printUserHelp("filebot -script fn:configure", UserData.root().getBackingStore());
    }

    private static void printUserHelp(String string, Object object) {
        Logging.help(Logging.format("* FileBot is running as [%s] using [%s]", UserData.getUID(), object));
        if (Settings.isLinuxApp() && !Settings.isLinuxContainer()) {
            Logging.help(Logging.format("* Use `sudo -H -u %s %s` to run as a different user", UserData.getUID(), string));
        }
        if (Settings.isDockerContainer()) {
            Logging.help(Logging.format("* Use `-e PUID=0` and `-e PGID=0` to run `%s` as a different user", string));
        }
    }

    private static Logging.Message formatMessage(String string, Object ... objectArray) {
        return Logging.Message.lazy(() -> {
            try {
                return String.format(IOUtils.toString((URL)ArgumentProcessor.class.getResource(string), (Charset)StandardCharsets.UTF_8), objectArray);
            }
            catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    public static Logging.Message formatStegosaurus(String string, String string2) {
        return ArgumentProcessor.formatMessage("Stegosaurus.format", string, string2);
    }

    public static Logging.Message formatArgumentHelp(String[] stringArray, boolean bl) {
        if (bl) {
            try {
                stringArray = ArgumentBean.expandAtFiles(stringArray);
            }
            catch (Exception exception) {
                Logging.debug.finest(exception::toString);
            }
        }
        return ArgumentProcessor.formatMessage("Argument.help", ArgumentBean.toString(stringArray));
    }

    public static Logging.Message formatExpressionHelp(String string) {
        return ArgumentProcessor.formatMessage("Expression.help", string);
    }

    public static ArgumentProcessor parseArgumentArray(List<String> list) throws Exception {
        return new ArgumentProcessor(new ArgumentBean(list.toArray(new String[0]), true));
    }

    public static ArgumentProcessor parseCommand(List<String> list) throws Exception {
        if (list.size() > 0 && list.get(0).equals("filebot")) {
            return ArgumentProcessor.parseArgumentArray(list.subList(1, list.size()));
        }
        return null;
    }
}


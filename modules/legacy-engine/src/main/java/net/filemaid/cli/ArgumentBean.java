package net.filemaid.cli;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileFilter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.filemaid.ApplicationFolder;
import net.filemaid.GroovyEngine;
import net.filemaid.Language;
import net.filemaid.RenameAction;
import net.filemaid.StandardPostProcessAction;
import net.filemaid.StandardRenameAction;
import net.filemaid.WebServices;
import net.filemaid.cli.ApplyOptionsHandler;
import net.filemaid.cli.BindingsHandler;
import net.filemaid.cli.CmdlineException;
import net.filemaid.cli.ConflictAction;
import net.filemaid.cli.ExecCommand;
import net.filemaid.cli.ExecOptionsHandler;
import net.filemaid.cli.ExecutableRenameAction;
import net.filemaid.cli.GroovyAction;
import net.filemaid.cli.GroovyExpressionHandler;
import net.filemaid.cli.LicenseOptionHandler;
import net.filemaid.cli.StandardConflictAction;
import net.filemaid.format.ExpressionFileComparator;
import net.filemaid.format.ExpressionFileFilter;
import net.filemaid.format.ExpressionFileFormat;
import net.filemaid.format.ExpressionFilter;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.ExpressionMapper;
import net.filemaid.format.QueryExpression;
import net.filemaid.hash.HashType;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Script;
import net.filemaid.subtitle.SubtitleFormat;
import net.filemaid.subtitle.SubtitleNaming;
import net.filemaid.subtitle.SubtitleUtilities;
import net.filemaid.ui.Mode;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.Datasource;
import net.filemaid.web.SortOrder;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.StopOptionHandler;

public class ArgumentBean {
    @Option(name="-rename", usage="Rename media files")
    public boolean rename = false;
    @Option(name="--db", usage="Database", metaVar="[TheTVDB, AniDB, TheMovieDB::TV] or [TheMovieDB] or [AcoustID, ID3] or [xattr, exif, file]")
    public String db;
    @Option(name="--order", usage="Episode order", metaVar="[Airdate, DVD, Absolute, Digital, Production, Date]")
    public String order = "Airdate";
    @Option(name="--format", usage="Format expression", handler=GroovyExpressionHandler.class)
    public String format;
    @Option(name="--action", usage="Rename action", metaVar="[move, copy, keeplink, symlink, hardlink, clone, test]")
    public String action = "move";
    @Option(name="--conflict", usage="Conflict resolution", metaVar="[skip, replace, auto, index, fail]")
    public String conflict = "skip";
    @Option(name="--filter", usage="Filter expression", handler=GroovyExpressionHandler.class)
    public String filter = null;
    @Option(name="--mapper", usage="Mapper expression", handler=GroovyExpressionHandler.class)
    public String mapper = null;
    @Option(name="--q", usage="Query expression", metaVar="[name] or [id] or {expression}", handler=GroovyExpressionHandler.class)
    public String query;
    @Option(name="--lang", usage="Language", metaVar="[English, German, ...]")
    public String lang = "en";
    @Option(name="-non-strict", usage="Enable advanced matching and more aggressive guess work")
    public boolean nonStrict = false;
    @Option(name="-r", usage="Select files from folders recursively")
    public boolean recursive = false;
    @Option(name="-d", usage="Select folders")
    public boolean directory = false;
    @Option(name="--file-filter", usage="Input file filter expression", handler=GroovyExpressionHandler.class)
    public String inputFileFilter = null;
    @Option(name="--file-order", usage="Input file order expression", handler=GroovyExpressionHandler.class)
    public String inputFileOrder = null;
    @Option(name="--output", usage="Output directory", metaVar="/path/to/folder")
    public String output;
    @Option(name="--apply", usage="Apply post-processing actions", metaVar="[artwork, cover, nfo, metadata, import, srt, date, tags, chmod, touch, prune, clean]", handler=ApplyOptionsHandler.class)
    public List<String> apply = new ArrayList<String>();
    @Option(name="-exec", usage="Execute command", metaVar="echo {f} [+*]", handler=ExecOptionsHandler.class)
    public List<String> exec = new ArrayList<String>();
    @Option(name="-extract", usage="Extract archives")
    public boolean extract = false;
    @Option(name="-check", usage="Create / Check verification files")
    public boolean check;
    @Option(name="-get-subtitles", usage="Fetch subtitles")
    public boolean getSubtitles;
    @Option(name="--encoding", usage="Output character encoding", metaVar="[UTF-8, Windows-1252]")
    public String encoding;
    @Option(name="-list", usage="Print episode list")
    public boolean list = false;
    @Option(name="-find", usage="Print file paths")
    public boolean find = false;
    @Option(name="-mediainfo", usage="Print media info")
    public boolean mediaInfo = false;
    @Option(name="-script", usage="Run Groovy script", metaVar="[fn:name] or [script.groovy]")
    public String script = null;
    @Option(name="--def", usage="Define script variables", handler=BindingsHandler.class)
    public Map<String, String> defines = new LinkedHashMap<String, String>();
    @Option(name="-revert", usage="Revert files")
    public boolean revert = false;
    @Option(name="--mode", usage="Enable CLI interactive mode", metaVar="[interactive]")
    public String mode = null;
    @Option(name="--log", usage="Log level", metaVar="[all, fine, info, warning, off]")
    public String log;
    @Option(name="--log-file", usage="Log file", metaVar="*.txt")
    public String logFile = null;
    @Option(name="-clear-cache", usage="Clear cached and temporary data")
    public boolean clearCache = false;
    @Option(name="-clear-prefs", usage="Clear application settings")
    public boolean clearPrefs = false;
    @Option(name="-clear-history", usage="Clear rename history")
    public boolean clearHistory = false;
    @Option(name="-unixfs", usage="Allow special characters in file paths")
    public boolean unixfs = false;
    @Option(name="-no-xattr", usage="Disable extended attributes")
    public boolean disableExtendedAttributes = false;
    @Option(name="-no-probe", usage="Disable media parser")
    public boolean disableMediaParser = false;
    @Option(name="-no-history", usage="Disable history")
    public boolean disableHistory = false;
    @Option(name="-no-index", usage="Disable media index")
    public boolean disableMediaIndex = false;
    @Option(name="-version", usage="Print version identifier")
    public boolean version = false;
    @Option(name="-help", usage="Print this help message")
    public boolean help = false;
    @Option(name="--license", usage="Import license file", handler=LicenseOptionHandler.class)
    public String license = null;
    @Argument
    @Option(name="--", handler=StopOptionHandler.class)
    public List<String> arguments = new ArrayList<String>();
    private final String[] args;

    public boolean runCLI() {
        return this.rename || this.getSubtitles || this.check || this.list || this.find || this.mediaInfo || this.revert || this.extract || this.script != null || this.license != null && (System.console() != null || GraphicsEnvironment.isHeadless());
    }

    public boolean isInteractive() {
        return "interactive".equalsIgnoreCase(this.mode);
    }

    public boolean printVersion() {
        return this.version;
    }

    public boolean printHelp() {
        return this.help;
    }

    public boolean clearCache() {
        return this.clearCache;
    }

    public boolean clearUserData() {
        return this.clearPrefs;
    }

    public boolean clearHistory() {
        return this.clearHistory;
    }

    public List<File> getFileArguments() throws Exception {
        if (this.arguments.isEmpty()) {
            return Collections.emptyList();
        }
        if (this.recursive || this.inputFileFilter != null || this.inputFileOrder != null) {
            return this.getFiles();
        }
        return this.getInputArguments();
    }

    public List<File> getFiles() throws Exception {
        List<File> list = this.getInputArguments();
        if (list.isEmpty() && this.arguments.isEmpty() && this.find) {
            File file = new File(".").getCanonicalFile();
            list = Arrays.asList(file);
        }
        List<File> list2 = new ArrayList<>();
        for (File file2 : list) {
            if (file2.isDirectory()) {
                if (this.directory) {
                    if (this.find || this.recursive) {
                        list2.addAll(FileUtilities.listFiles(file2, FileUtilities.FOLDERS, FileUtilities.HUMAN_NAME_ORDER));
                        continue;
                    }
                    list2.add(file2);
                    continue;
                }
                if (this.find || this.recursive) {
                    list2.addAll(FileUtilities.listFiles(file2, FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER));
                    continue;
                }
                list2.addAll(FileUtilities.getChildren(file2, file -> file.isFile() && !file.isHidden(), FileUtilities.HUMAN_NAME_ORDER));
                continue;
            }
            list2.add(file2);
        }
        if (this.inputFileFilter != null && !list2.isEmpty()) {
            list2 = FileUtilities.filter(list2, new ExpressionFileFilter(this.inputFileFilter));
        }
        if (this.inputFileOrder != null && !list2.isEmpty()) {
            list2.sort(new ExpressionFileComparator(this.inputFileOrder));
        }
        return list2;
    }

    public List<File> getInputArguments() {
        return this.arguments.stream().filter(string -> !string.isEmpty()).map(File::new).map(file -> FileUtilities.getRealPath(file)).collect(Collectors.toList());
    }

    public RenameAction getRenameAction() {
        if (this.isExecutable(this.action)) {
            return ExecutableRenameAction.executable(new File(this.action), this.getOutputPath());
        }
        if (ArgumentBean.isGroovyScript(this.action)) {
            return ArgumentBean.resolveGroovyScript(this.action, GroovyAction::new, "Invalid --action script");
        }
        return ArgumentBean.optional(this.action, StandardRenameAction::forName, "Invalid --action value").orElse(StandardRenameAction.MOVE);
    }

    public ConflictAction getConflictAction() {
        if (ArgumentBean.isGroovyScript(this.conflict)) {
            return ArgumentBean.resolveGroovyScript(this.conflict, GroovyAction::new, "Invalid --conflict script");
        }
        return ArgumentBean.optional(this.conflict, StandardConflictAction::forName, "Invalid --conflict value").orElse(StandardConflictAction.SKIP);
    }

    public SortOrder getSortOrder() {
        return ArgumentBean.optional(this.order, SortOrder::forName, "Invalid --order value").orElse(SortOrder.Airdate);
    }

    public ExpressionFormat getExpressionFormat() throws Exception {
        return this.format == null ? null : new ExpressionFormat(this.format);
    }

    public ExpressionFileFormat getExpressionFileFormat() throws Exception {
        return this.format == null ? null : new ExpressionFileFormat(this.format);
    }

    public ExpressionFilter getExpressionFilter() throws Exception {
        return this.filter == null ? null : new ExpressionFilter(this.filter);
    }

    public FileFilter getExpressionFileFilter() throws Exception {
        return this.filter == null ? null : new ExpressionFileFilter(this.filter, XattrMetaInfo.xattr::getMetaInfo);
    }

    public ExpressionMapper getExpressionMapper() throws Exception {
        return this.mapper == null ? null : new ExpressionMapper(this.mapper);
    }

    public Datasource getDatasource() {
        return ArgumentBean.optional(this.db, WebServices::getService, "Invalid --db value").orElse(null);
    }

    public QueryExpression getQueryExpression() throws Exception {
        return this.query == null ? null : new QueryExpression(this.query);
    }

    public File getOutputPath() {
        return this.output == null ? null : new File(this.output);
    }

    public File getAbsoluteOutputFolder() {
        return ArgumentBean.optional(this.output, string -> this.prepareOutputPath((String)string, null), "Invalid --output folder path").orElse(null);
    }

    public SubtitleFormat getSubtitleOutputFormat() {
        return this.output == null ? null : SubtitleUtilities.getSubtitleFormatByName(this.output);
    }

    public SubtitleNaming getSubtitleNamingFormat() {
        return ArgumentBean.optional(this.format, SubtitleNaming::forName, "Invalid subtitle naming --format value").orElse(SubtitleNaming.MATCH_VIDEO_ADD_LANGUAGE_TAG);
    }

    public HashType getOutputHashType() {
        return ArgumentBean.optional(this.format, string -> VerificationUtilities.getHashType(string), "Invalid checksum --format value").orElseGet(() -> ArgumentBean.optional(this.output, string -> VerificationUtilities.getHashType(new File((String)string)), "Invalid checksum --output path").orElse(HashType.SFV));
    }

    public Charset getEncoding() {
        return ArgumentBean.optional(this.encoding, Charset::forName, "Invalid --encoding value").orElse(null);
    }

    public Language getLanguage() {
        return ArgumentBean.optional(this.lang, Language::forName, "Invalid --lang value").orElseGet(Language::defaultLanguage);
    }

    public File getLogFile() {
        return ArgumentBean.optional(this.logFile, string -> this.prepareOutputPath((String)string, ApplicationFolder.Logs.getDirectory()), "Invalid --log-file path").orElse(null);
    }

    public boolean isStrict() {
        return !this.nonStrict;
    }

    public Level getLogLevel() {
        return ArgumentBean.optional(this.log, string -> Level.parse(string.toUpperCase()), "Invalid --log level").orElse(Level.ALL);
    }

    public ExecCommand getExecCommand() {
        return this.exec.isEmpty() ? null : (ExecCommand)ArgumentBean.optional(this.exec, list -> ExecCommand.parse(list, this.getOutputPath()), "Invalid --exec expression").orElse(null);
    }

    public Apply[] getPostProcessActions() {
        return this.apply.isEmpty() ? null : (Apply[])ArgumentBean.optional(this.apply, list -> (Apply[])list.stream().map(string -> {
            if (ArgumentBean.isGroovyScript(string)) {
                return ArgumentBean.resolveGroovyScript(string, Script::new, "Invalid --apply post-processing script");
            }
            return StandardPostProcessAction.forName(string);
        }).toArray(Apply[]::new), "Invalid --apply post-processing action").orElse(null);
    }

    public Mode[] getPanelMode() {
        return ArgumentBean.optional(this.mode, string -> new Mode[]{Mode.forName(this.mode)}, "Invalid --mode value").orElseGet(Mode::modes);
    }

    public String getLicenseKey() {
        return this.license;
    }

    public ArgumentBean() {
        this.args = new String[0];
    }

    public ArgumentBean(String[] stringArray, boolean bl) throws CmdLineException {
        CmdLineParser cmdLineParser = new CmdLineParser(this, ParserProperties.defaults().withAtSyntax(false).withOptionValueDelimiter("="));
        try {
            this.args = bl ? ArgumentBean.expandAtFiles(stringArray) : (String[])stringArray.clone();
        }
        catch (Exception exception) {
            throw new CmdLineException(cmdLineParser, exception.getMessage(), exception);
        }
        cmdLineParser.parseArgument((String[])this.args.clone());
    }

    public String[] getArgumentArray() {
        return (String[])this.args.clone();
    }

    public String usage() {
        StringWriter stringWriter = new StringWriter(4096);
        CmdLineParser cmdLineParser = new CmdLineParser(this, ParserProperties.defaults().withShowDefaults(false).withOptionSorter(null));
        cmdLineParser.printUsage(stringWriter, null);
        return stringWriter.toString();
    }

    public String toString() {
        return ArgumentBean.toString(this.args);
    }

    private File prepareOutputPath(String string, File file) {
        try {
            File file2 = new File(string);
            if (file != null && !file2.isAbsolute()) {
                file2 = new File(file, string);
            }
            return file2.getCanonicalFile();
        }
        catch (Exception exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }
    }

    private boolean isExecutable(String string) {
        if (FileUtilities.UNIX) {
            return string.startsWith("/") || string.endsWith(".sh");
        }
        return string.endsWith(".ps1") || string.endsWith(".cmd") || string.endsWith(".bat") || string.endsWith(".exe");
    }

    private static boolean isGroovyScript(String string) {
        return string.startsWith("{") || string.endsWith("}") || string.endsWith(".groovy");
    }

    private static <T> T resolveGroovyScript(String string, BiFunction<String, String, T> biFunction, String string2) {
        try {
            if (GroovyEngine.isGroovyFile(string)) {
                File file = new File(string).getAbsoluteFile();
                return biFunction.apply(file.getName(), GroovyEngine.resolveExternalScript(file));
            }
            return biFunction.apply("GROOVY", GroovyEngine.resolveScript(string));
        }
        catch (Exception exception) {
            throw new CmdlineException(string2 + ": " + ArgumentBean.quote(string) + ": " + exception.getMessage());
        }
    }

    private static <S, T> Optional<T> optional(S s, Function<S, T> function, String string) {
        try {
            return Optional.ofNullable(s).map(function);
        }
        catch (CmdlineException cmdlineException) {
            throw cmdlineException;
        }
        catch (Exception exception) {
            throw new CmdlineException(string + ": " + ArgumentBean.quote(s) + " " + exception.getMessage());
        }
    }

    private static String quote(Object object) {
        return object instanceof List ? object.toString() : "'" + object + "'";
    }

    public static String toString(String[] stringArray) {
        return IntStream.range(0, stringArray.length).mapToObj(n -> String.format("args[%s] = %s", n + 1, stringArray[n])).collect(Collectors.joining(System.lineSeparator()));
    }

    public static String[] expandAtFiles(String[] stringArray) {
        return (String[])Arrays.stream(stringArray).flatMap(string2 -> {
            File file;
            if (string2.startsWith("@") && !string2.endsWith(".groovy") && (file = new File(string2.substring(1)).getAbsoluteFile()).exists()) {
                try {
                    return FileUtilities.readLines(file).stream().map(string -> {
                        if (string.startsWith("\"") && string.endsWith("\"")) {
                            return string.substring(1, string.length() - 1);
                        }
                        return string.trim();
                    });
                }
                catch (Exception exception) {
                    throw new CmdlineException("Invalid @file path", file, exception);
                }
            }
            return Stream.of(string2);
        }).toArray(String[]::new);
    }

    public static ArgumentBean parse(String ... stringArray) throws CmdLineException {
        try {
            return new ArgumentBean(stringArray, true);
        }
        catch (CmdLineException cmdLineException) {
            if (Boolean.parseBoolean(System.getProperty("apple.app.launcher"))) {
                return new ArgumentBean();
            }
            throw cmdLineException;
        }
    }
}


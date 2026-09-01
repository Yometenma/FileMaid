package net.filemaid.cli;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.filemaid.Language;
import net.filemaid.RenameAction;
import net.filemaid.cli.ConflictAction;
import net.filemaid.cli.ExecCommand;
import net.filemaid.format.ExpressionFileFormat;
import net.filemaid.format.ExpressionFilter;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.ExpressionMapper;
import net.filemaid.format.QueryExpression;
import net.filemaid.hash.HashType;
import net.filemaid.postprocess.Apply;
import net.filemaid.subtitle.SubtitleFormat;
import net.filemaid.subtitle.SubtitleNaming;
import net.filemaid.web.Datasource;
import net.filemaid.web.SortOrder;

public interface CmdlineInterface {
    public List<File> rename(Collection<File> var1, Datasource var2, QueryExpression var3, SortOrder var4, Locale var5, ExpressionFilter var6, ExpressionMapper var7, boolean var8, ExpressionFileFormat var9, File var10, RenameAction var11, ConflictAction var12, Apply[] var13, ExecCommand var14) throws Exception;

    public List<File> renameLinear(List<File> var1, Datasource var2, QueryExpression var3, SortOrder var4, Locale var5, ExpressionFilter var6, ExpressionMapper var7, ExpressionFileFormat var8, File var9, RenameAction var10, ConflictAction var11, Apply[] var12, ExecCommand var13) throws Exception;

    public List<File> rename(Map<File, File> var1, RenameAction var2, ConflictAction var3) throws Exception;

    public List<File> revert(Collection<File> var1, FileFilter var2, ExpressionFileFormat var3, File var4, RenameAction var5) throws Exception;

    public List<File> getSubtitles(Collection<File> var1, QueryExpression var2, Language var3, SubtitleFormat var4, Charset var5, SubtitleNaming var6, boolean var7) throws Exception;

    public List<File> getMissingSubtitles(Collection<File> var1, QueryExpression var2, Language var3, SubtitleFormat var4, Charset var5, SubtitleNaming var6, boolean var7) throws Exception;

    public void check(Collection<File> var1) throws Exception;

    public File compute(Collection<File> var1, HashType var2, File var3, Charset var4) throws Exception;

    public Stream<String> list(Datasource var1, QueryExpression var2, SortOrder var3, Locale var4, ExpressionFilter var5, ExpressionMapper var6, ExpressionFormat var7, boolean var8) throws Exception;

    public Stream<String> getMediaInfo(Collection<File> var1, FileFilter var2, ExpressionFormat var3) throws Exception;

    public IntStream execute(Collection<File> var1, FileFilter var2, ExpressionFormat var3, Apply[] var4, ExecCommand var5) throws Exception;

    public List<File> extract(Collection<File> var1, File var2, FileFilter var3, boolean var4) throws Exception;

    public File zip(Collection<File> var1, File var2, FileFilter var3) throws Exception;
}


package net.filemaid.cli;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.Logging;
import net.filemaid.Resource;
import net.filemaid.Settings;
import net.filemaid.cli.CmdlineException;
import net.filemaid.cli.ScriptBundle;
import net.filemaid.cli.ScriptProvider;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.WebRequest;
import org.tukaani.xz.XZInputStream;

public enum ScriptSource implements ScriptProvider
{
    GITHUB_STABLE{
        private final Resource<ScriptBundle> bundle = Resource.lazy(() -> new ScriptBundle(this.getCache().bytes("fn", string -> Settings.getApiResource("script/" + string + ".jar.xz").toURL(), XZInputStream::new).expire(Cache.ONE_WEEK)));

        @Override
        public boolean accept(String string) {
            return string.startsWith("fn:");
        }

        @Override
        public String getScript(String string) throws Exception {
            return this.bundle.get().getScript(string.substring(3));
        }

        @Override
        public ScriptProvider getResolver(String string) throws Exception {
            return this.bundle.get();
        }

        @Override
        public String getManifest() {
            try {
                Map<String, String> map = this.bundle.get().getManifest();
                String string = map.get("Build-Revision");
                String string2 = map.get("Build-Date");
                return "Script Bundle: " + string2 + " (r" + string + ")";
            }
            catch (Exception exception) {
                return "Script Bundle: " + Logging.cause(exception);
            }
        }
    }
    ,
    GITHUB_MASTER{

        @Override
        public boolean accept(String string) {
            return string.startsWith("dev:");
        }

        @Override
        public String getScript(String string) throws Exception {
            return this.fetch(string.substring(4));
        }

        public String fetch(String string2) throws Exception {
            return this.getCache().text(string2, string -> WebRequest.newURL(this.getRepository(), string + ".groovy")).fetch(CachedResource.fetchIfNoneMatch(string2, this.getCache())).expire(Cache.ONE_DAY).get();
        }

        public URI getRepository() {
            return URI.create(Settings.getApplicationProperty("github.master"));
        }

        @Override
        public ScriptProvider getResolver(String string) {
            return this::fetch;
        }

        @Override
        public String getManifest() {
            return "Github Repository: " + this.getRepository();
        }
    }
    ,
    INLINE_GROOVY{

        @Override
        public boolean accept(String string) {
            return string.startsWith("g:");
        }

        @Override
        public String getScript(String string) throws Exception {
            return string.substring(2);
        }

        @Override
        public ScriptProvider getResolver(String string) throws Exception {
            return GITHUB_STABLE.getResolver(string);
        }
    }
    ,
    REMOTE_URL{

        @Override
        public boolean accept(String string) {
            if (string.length() >= 10 && string.charAt(0) != '/' && string.charAt(1) != ':' && string.indexOf(58) > 0) {
                try {
                    return new URI(string).isAbsolute();
                }
                catch (Exception exception) {
                    Logging.debug.finest(Logging.cause(exception));
                }
            }
            return false;
        }

        @Override
        public String getScript(String string2) throws Exception {
            return this.getCache().text(string2, string -> WebRequest.newURL(string)).expire(Duration.ZERO).get();
        }

        @Override
        public ScriptProvider getResolver(String string) throws Exception {
            String string3 = string.substring(0, string.lastIndexOf(47));
            return string2 -> this.getScript(string3 + "/" + string2 + ".groovy");
        }
    }
    ,
    LOCAL_FILE{

        @Override
        public boolean accept(String string) {
            try {
                return new File(string).isFile();
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(exception));
                return false;
            }
        }

        @Override
        public String getScript(String string) throws Exception {
            return FileUtilities.readTextFile(new File(string));
        }

        @Override
        public ScriptProvider getResolver(String string2) throws Exception {
            File file = new File(string2).getParentFile();
            return string -> FileUtilities.readTextFile(new File(file, string + ".groovy"));
        }
    };


    public abstract boolean accept(String var1);

    public abstract ScriptProvider getResolver(String var1) throws Exception;

    public String getManifest() {
        return this.name();
    }

    public Cache getCache() {
        return Cache.getCache("script_bundle_" + this.ordinal(), CacheType.Persistent);
    }

    public static ScriptSource findScriptProvider(String string) throws Exception {
        return Arrays.stream(ScriptSource.values()).filter(scriptSource -> scriptSource.accept(string)).findFirst().orElseThrow(() -> new CmdlineException("Bad script source", string));
    }
}


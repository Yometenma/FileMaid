package net.filemaid.infrastructure.metadata;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;

public final class LegacyTmdbMetadataProvider implements MetadataProvider {
    private final Object movieClient;
    private final Object tvClient;
    private final String status;

    public LegacyTmdbMetadataProvider(String apiKey) {
        Object movies = null;
        Object television = null;
        String state;
        if (apiKey == null || apiKey.isBlank()) {
            state = "未配置 TMDB API Key（FILEMAID_TMDB_API_KEY）";
        } else {
            try {
                Class<?> coreType = Class.forName("net.filemaid.web.TMDbCore");
                Object core = coreType.getConstructor(String.class).newInstance(apiKey.trim());
                movies = Class.forName("net.filemaid.web.TMDbMovieClient").getConstructor(coreType).newInstance(core);
                television = Class.forName("net.filemaid.web.TMDbTVClient").getConstructor(coreType).newInstance(core);
                state = "旧引擎 TMDB 客户端已就绪";
            } catch (Throwable failure) {
                state = "当前构建未包含旧引擎 TMDB 客户端";
            }
        }
        movieClient = movies;
        tvClient = television;
        status = state;
    }

    @Override public String id() { return "tmdb"; }
    @Override public boolean available() { return movieClient != null && tvClient != null; }
    @Override public String status() { return status; }

    @Override
    public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        Object client = type == MetadataType.MOVIE ? movieClient : tvClient;
        String methodName = type == MetadataType.MOVIE ? "searchMovie" : "search";
        Method search = client.getClass().getMethod(methodName, String.class, Locale.class);
        List<?> values = (List<?>) search.invoke(client, query, locale);
        List<MetadataCandidate> result = new ArrayList<>();
        for (Object value : values) {
            if (result.size() >= limit) break;
            result.add(candidate(value, type));
        }
        return result;
    }

    private MetadataCandidate candidate(Object value, MetadataType type) throws Exception {
        String id = String.valueOf(invoke(value, "getId"));
        String title = (String) invoke(value, "getName");
        String[] aliases = (String[]) invoke(value, "getAliasNames");
        Integer year = null;
        String overview = null;
        if (type == MetadataType.MOVIE) {
            int valueYear = (Integer) invoke(value, "getYear");
            year = valueYear > 0 ? valueYear : null;
        } else {
            Object firstAired = invokeOptional(value, "getFirstAired");
            if (firstAired != null) year = (Integer) invoke(firstAired, "getYear");
            overview = (String) invokeOptional(value, "getOverview");
        }
        return new MetadataCandidate(id(), id, type, title, aliases == null ? List.of() : Arrays.asList(aliases), year, overview);
    }

    private Object invoke(Object target, String method) throws Exception {
        return target.getClass().getMethod(method).invoke(target);
    }

    private Object invokeOptional(Object target, String method) throws Exception {
        try { return invoke(target, method); } catch (NoSuchMethodException ignored) { return null; }
    }
}

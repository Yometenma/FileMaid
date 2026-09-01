package net.filemaid.application.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.core.model.MediaGroup;
import net.filemaid.core.model.MediaGroupMember;
import net.filemaid.core.model.MediaGroupType;
import net.filemaid.core.model.MediaKind;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;

public final class AnalyzeMediaGroupsService {
    private static final Set<String> SUBTITLES = Set.of(".srt", ".ass", ".ssa", ".sub", ".vtt", ".sup");
    private static final Set<String> IMAGES = Set.of(".jpg", ".jpeg", ".png", ".webp", ".avif");
    private static final Set<String> NFO = Set.of(".nfo");
    private static final Set<String> DIRECTORY_LEVEL_ARTWORK = Set.of("poster", "folder", "cover", "backdrop", "banner", "fanart", "thumb");
    private final MediaNameParser parser;

    public AnalyzeMediaGroupsService(MediaNameParser parser) { this.parser = parser; }

    public List<MediaGroup> analyze(List<String> relativePaths) {
        if (relativePaths == null || relativePaths.isEmpty()) return List.of();
        if (relativePaths.size() > 1_000) throw new IllegalArgumentException("A group analysis may contain at most 1000 paths");
        List<Item> items = relativePaths.stream().map(this::item).toList();
        List<Item> videos = items.stream().filter(item -> item.kind == MediaKind.VIDEO).toList();
        Map<String, MutableGroup> groups = new LinkedHashMap<>();
        for (Item item : videos) groups.computeIfAbsent(groupKey(item.media), ignored -> new MutableGroup(item.media)).members.add(member(item, null));
        for (Item subtitle : items.stream().filter(item -> item.kind == MediaKind.SUBTITLE).toList()) {
            Item video = bestVideo(subtitle, videos);
            if (video != null) {
                groups.computeIfAbsent(groupKey(video.media), ignored -> new MutableGroup(video.media)).members.add(member(subtitle, video.path));
            } else {
                MutableGroup group = groups.computeIfAbsent("unknown:" + subtitle.path.toLowerCase(Locale.ROOT), ignored -> new MutableGroup(subtitle.media));
                group.members.add(member(subtitle, null));
                group.warnings.add("发现未能关联到视频的字幕");
            }
        }
        for (Item companion : items.stream().filter(item -> item.kind == MediaKind.IMAGE || item.kind == MediaKind.NFO).toList()) {
            Item matched = bestVideo(companion, videos);
            if (matched == null && isDirectoryLevelArtwork(companion)) {
                matched = sameDirectoryVideo(companion, videos);
            }
            final Item video = matched;
            if (video != null) {
                groups.computeIfAbsent(groupKey(video.media), ignored -> new MutableGroup(video.media)).members.add(member(companion, video.path));
            } else {
                MutableGroup group = groups.computeIfAbsent("companion:" + companion.path.toLowerCase(Locale.ROOT), ignored -> new MutableGroup(companion.media));
                group.members.add(member(companion, null));
                group.warnings.add("发现未能关联到视频的伴随文件（封面/NFO）");
            }
        }
        return groups.entrySet().stream().map(entry -> entry.getValue().build(entry.getKey())).toList();
    }

    private boolean isDirectoryLevelArtwork(Item item) {
        return DIRECTORY_LEVEL_ARTWORK.contains(normalizedStem(item.path));
    }

    private Item sameDirectoryVideo(Item companion, List<Item> videos) {
        return videos.stream().filter(video -> parent(video.path).equals(parent(companion.path))).findFirst().orElse(null);
    }

    private Item bestVideo(Item subtitle, List<Item> videos) {
        String subtitleStem = normalizedStem(subtitle.path);
        Item best = null;
        int bestScore = 0;
        for (Item video : videos) {
            int score = 0;
            if (parent(subtitle.path).equals(parent(video.path))) score += 2;
            String videoStem = normalizedStem(video.path);
            if (subtitleStem.equals(videoStem)) score += 8;
            else if (subtitleStem.startsWith(videoStem)) score += 6;
            if (sameEpisode(subtitle.media, video.media)) score += 5;
            if (score > bestScore) { bestScore = score; best = video; }
        }
        return bestScore >= 6 ? best : null;
    }

    private boolean sameEpisode(ParsedMediaName a, ParsedMediaName b) {
        return a.type() == MediaType.EPISODE && b.type() == MediaType.EPISODE
                && java.util.Objects.equals(a.season(), b.season()) && a.episodes().equals(b.episodes());
    }

    private Item item(String source) {
        Path path = Path.of(source).normalize();
        if (path.isAbsolute() || path.startsWith("..")) throw new IllegalArgumentException("Analysis paths must stay relative to a storage root");
        String normalized = path.toString().replace('\\', '/');
        ParsedMediaName parsed = parser.parse(path.getFileName().toString());
        MediaKind kind = kindOf(parsed.extension());
        return new Item(normalized, kind, parsed);
    }

    private MediaKind kindOf(String extension) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (SUBTITLES.contains(ext)) return MediaKind.SUBTITLE;
        if (IMAGES.contains(ext)) return MediaKind.IMAGE;
        if (NFO.contains(ext)) return MediaKind.NFO;
        return MediaKind.VIDEO;
    }

    private String groupKey(ParsedMediaName media) {
        String title = media.title() == null ? "unknown" : media.title().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
        if (media.type() == MediaType.EPISODE) return "series:" + title;
        if (media.type() == MediaType.MOVIE) return "movie:" + title + ":" + (media.year() == null ? "" : media.year());
        return "unknown:" + title;
    }

    private String normalizedStem(String path) {
        String name = Path.of(path).getFileName().toString().toLowerCase(Locale.ROOT);
        name = name.replaceFirst("\\.[^.]+$", "").replaceFirst("[._ -](zh|zho|chi|chs|cht|en|eng|ja|jpn)([._ -].*)?$", "");
        return name.replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private String parent(String path) { Path parent = Path.of(path).getParent(); return parent == null ? "" : parent.normalize().toString(); }
    private MediaGroupMember member(Item item, String companionOf) { return new MediaGroupMember(item.path, item.kind, companionOf, item.media); }

    private record Item(String path, MediaKind kind, ParsedMediaName media) {}
    private static final class MutableGroup {
        final ParsedMediaName media; final List<MediaGroupMember> members = new ArrayList<>(); final List<String> warnings = new ArrayList<>();
        MutableGroup(ParsedMediaName media) { this.media = media; }
        MediaGroup build(String key) {
            MediaGroupType type = media.type() == MediaType.EPISODE ? MediaGroupType.SERIES : media.type() == MediaType.MOVIE ? MediaGroupType.MOVIE : MediaGroupType.UNKNOWN;
            long videos = members.stream().filter(member -> member.kind() == MediaKind.VIDEO).count();
            if (videos == 0 && warnings.isEmpty()) warnings.add("媒体组中没有视频文件");
            return new MediaGroup(key, type, media.title(), media.year(), members, warnings);
        }
    }
}

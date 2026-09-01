package net.filemaid.infrastructure.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.filemaid.core.model.MediaType;
import org.junit.jupiter.api.Test;

class RegexMediaNameParserTest {
    private final RegexMediaNameParser parser = new RegexMediaNameParser();

    @Test
    void parsesMultiEpisodeFile() {
        var parsed = parser.parse("The.Show.S02E03E04.1080p.WEB-DL.mkv");
        assertEquals(MediaType.EPISODE, parsed.type());
        assertEquals("The Show", parsed.title());
        assertEquals(2, parsed.season());
        assertEquals(List.of(3, 4), parsed.episodes());
        assertEquals(".mkv", parsed.extension());
    }

    @Test
    void parsesMovieWithYear() {
        var parsed = parser.parse("Example.Movie.2024.2160p.BluRay.mkv");
        assertEquals(MediaType.MOVIE, parsed.type());
        assertEquals("Example Movie", parsed.title());
        assertEquals(2024, parsed.year());
    }
}

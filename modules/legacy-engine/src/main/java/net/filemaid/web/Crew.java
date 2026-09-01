package net.filemaid.web;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.filemaid.web.Person;

public interface Crew {
    public List<Person> getCrew();

    default public List<Person> getCast() {
        return this.getCrew().stream().filter(Person::isActor).collect(Collectors.toList());
    }

    default public List<String> getActors() {
        return this.getCrewNames(Person::isActor);
    }

    default public List<String> getDirectors() {
        return this.getCrewNames(Person::isDirector);
    }

    default public List<String> getWriters() {
        return this.getCrewNames(Person::isWriter);
    }

    default public String getDirector() {
        return this.getCrewName(Person::isDirector);
    }

    default public String getWriter() {
        return this.getCrewName(Person::isWriter);
    }

    default public String getCrewName(Predicate<Person> predicate) {
        return this.getCrew().stream().filter(predicate).map(Person::getName).findFirst().orElse(null);
    }

    default public List<String> getCrewNames(Predicate<Person> predicate) {
        return this.getCrew().stream().filter(predicate).map(Person::getName).collect(Collectors.toList());
    }
}


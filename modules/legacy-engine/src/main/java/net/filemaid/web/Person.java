package net.filemaid.web;

import java.io.Serializable;
import java.net.URL;
import java.util.Comparator;

public class Person
implements Serializable {
    protected Integer id;
    protected String name;
    protected String character;
    protected String job;
    protected String department;
    protected Integer order;
    protected URL image;
    public static final String WRITER = "Writer";
    public static final String DIRECTOR = "Director";
    public static final String ACTOR = "Actor";
    public static final String GUEST_STAR = "Guest Star";
    public static final String WRITING_DEPARTMENT = "Writing";
    public static final String ACTING_DEPARTMENT = "Acting";
    public static final Comparator<Person> CREDIT_ORDER = Comparator.comparing(Person::getOrder, Comparator.nullsLast(Comparator.naturalOrder()));

    public Person() {
    }

    public Person(String string, String string2) {
        this(null, string, null, string2, null, null, null);
    }

    public Person(Integer n, String string, String string2, String string3, String string4, Integer n2, URL uRL) {
        this.id = n;
        this.name = string;
        this.character = string2 == null || string2.isEmpty() ? null : string2;
        this.job = string3 == null || string3.isEmpty() ? null : string3;
        this.department = string4 == null || string4.isEmpty() ? null : string4;
        this.order = n2;
        this.image = uRL;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getCharacter() {
        return this.character;
    }

    public String getJob() {
        return this.job;
    }

    public String getDepartment() {
        return this.department;
    }

    public Integer getOrder() {
        return this.order;
    }

    public URL getImage() {
        return this.image;
    }

    public boolean isActor() {
        return this.character != null || ACTOR.equals(this.job) || GUEST_STAR.equals(this.job) || ACTING_DEPARTMENT.equals(this.department);
    }

    public boolean isDirector() {
        return DIRECTOR.equals(this.job);
    }

    public boolean isWriter() {
        return WRITER.equals(this.job);
    }

    public boolean isWritingDepartment() {
        return WRITING_DEPARTMENT.equals(this.department);
    }

    public String toString() {
        return String.format("%s (%s)", this.name, this.character != null ? this.character : (this.job != null ? this.job : this.department));
    }
}


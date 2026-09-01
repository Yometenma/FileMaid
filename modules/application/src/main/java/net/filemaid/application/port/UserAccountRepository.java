package net.filemaid.application.port;

import java.util.Optional;

public interface UserAccountRepository {
    boolean exists();
    Optional<Account> findByUsername(String username);
    void create(String username, String passwordHash);
    record Account(String username, String passwordHash) { }
}

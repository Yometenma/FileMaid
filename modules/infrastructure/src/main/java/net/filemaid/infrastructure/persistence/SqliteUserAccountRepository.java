package net.filemaid.infrastructure.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Optional;
import net.filemaid.application.port.UserAccountRepository;

public final class SqliteUserAccountRepository implements UserAccountRepository {
    private final String jdbcUrl;
    public SqliteUserAccountRepository(String dbPath){String value=dbPath==null||dbPath.isBlank()?"./config/filemaid.db":dbPath;Path file=Path.of(value);try{if(file.getParent()!=null)Files.createDirectories(file.getParent());}catch(Exception failure){throw new IllegalStateException(failure);}jdbcUrl="jdbc:sqlite:"+value;try(var connection=DriverManager.getConnection(jdbcUrl);var statement=connection.createStatement()){statement.execute("CREATE TABLE IF NOT EXISTS user_account (id INTEGER PRIMARY KEY CHECK(id=1), username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, created_at TEXT NOT NULL)");}catch(Exception failure){throw new IllegalStateException("Failed to initialize user account",failure);}}
    @Override public boolean exists(){try(var connection=DriverManager.getConnection(jdbcUrl);var statement=connection.createStatement();var row=statement.executeQuery("SELECT 1 FROM user_account LIMIT 1")){return row.next();}catch(Exception failure){throw new IllegalStateException("Failed to inspect user account",failure);}}
    @Override public Optional<Account> findByUsername(String username){try(var connection=DriverManager.getConnection(jdbcUrl);var statement=connection.prepareStatement("SELECT username,password_hash FROM user_account WHERE username=?")){statement.setString(1,username);try(var row=statement.executeQuery()){return row.next()?Optional.of(new Account(row.getString(1),row.getString(2))):Optional.empty();}}catch(Exception failure){throw new IllegalStateException("Failed to load user account",failure);}}
    @Override public synchronized void create(String username,String hash){if(exists())throw new IllegalStateException("管理员账号已经存在");try(var connection=DriverManager.getConnection(jdbcUrl);var statement=connection.prepareStatement("INSERT INTO user_account(id,username,password_hash,created_at) VALUES(1,?,?,CURRENT_TIMESTAMP)")){statement.setString(1,username);statement.setString(2,hash);statement.executeUpdate();}catch(Exception failure){throw new IllegalStateException("Failed to create user account",failure);}}
    @Override public synchronized void updatePassword(String username,String hash){try(var connection=DriverManager.getConnection(jdbcUrl);var statement=connection.prepareStatement("UPDATE user_account SET password_hash=? WHERE username=?")){statement.setString(1,hash);statement.setString(2,username);int rows=statement.executeUpdate();if(rows==0)throw new IllegalStateException("账号不存在");}catch(Exception failure){throw new IllegalStateException("Failed to update user password",failure);}}
}

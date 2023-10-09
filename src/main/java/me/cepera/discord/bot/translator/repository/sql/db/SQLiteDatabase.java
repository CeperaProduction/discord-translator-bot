package me.cepera.discord.bot.translator.repository.sql.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SQLiteDatabase {

    private static final Logger LOGGER = LogManager.getLogger(SQLiteDatabase.class);

    private final String filePath;

    private final ReentrantLock lock = new ReentrantLock();

    public SQLiteDatabase(Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            LOGGER.error("Can't create working directory.", e);
            throw new RuntimeException(e);
        }
        this.filePath = path.toAbsolutePath().toString();
    }

    public <T> T connect(ConnectionHandler<T> connectionConsumer) {

        String url = "jdbc:sqlite:" + filePath;

        lock.lock();

        try (Connection conn = DriverManager.getConnection(url)) {
            return connectionConsumer.apply(conn);
        } catch (SQLException e) {
            LOGGER.error("Error while working with database", e);
            throw new RuntimeException(e);
        }finally {
            lock.unlock();
        }


    }

    public static interface ConnectionHandler<T> {

        T apply(Connection conn) throws SQLException;

    }

}

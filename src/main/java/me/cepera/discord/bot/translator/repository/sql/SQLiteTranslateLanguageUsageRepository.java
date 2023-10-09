package me.cepera.discord.bot.translator.repository.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.repository.sql.db.SQLiteDatabase;
import reactor.core.publisher.Mono;

public class SQLiteTranslateLanguageUsageRepository extends SQLiteRepository implements TranslateLanguageUsageRepository {

    public SQLiteTranslateLanguageUsageRepository(SQLiteDatabase database) {
        super(database);
        prepareTable();
    }

    private void prepareTable() {
        connect(c->c.createStatement().execute("CREATE TABLE IF NOT EXISTS used_languages ("
                    + "id integer PRIMARY KEY, "
                    + "userId integer NOT NULL, "
                    + "languageCode text NOT NULL, "
                    + "timestamp integer NOT NULL)"));
    }

    @Override
    public Mono<String> getLastLanguageCode(long userId) {
        return Mono.fromSupplier(()->connect(c->{
           PreparedStatement stm = c.prepareStatement("SELECT languageCode FROM used_languages WHERE userId = ?");
           stm.setLong(1, userId);
           ResultSet rs = stm.executeQuery();
           if(rs.next()) {
               return rs.getString(1);
           }
           return null;
        }));
    }

    @Override
    public Mono<Void> setLastLanguageCode(long userId, String languageCode) {
        return Mono.fromRunnable(()->connect(c->{
            if(languageCode == null || languageCode.isEmpty()) {
                PreparedStatement stm = c.prepareStatement("DELETE FROM used_languages WHERE userId = ?");
                stm.setLong(1, userId);
                stm.executeUpdate();
                return null;
            }
            PreparedStatement stm = c.prepareStatement("UPDATE used_languages SET languageCode = ?, timestamp = ? WHERE userId = ?");
            stm.setString(1, languageCode);
            stm.setLong(2, System.currentTimeMillis());
            stm.setLong(3, userId);
            int count = stm.executeUpdate();
            if(count == 0) {
                stm = c.prepareStatement("INSERT INTO used_languages (languageCode, userId, timestamp) VALUES (?, ?, ?)");
                stm.setString(1, languageCode);
                stm.setLong(2, userId);
                stm.setLong(3, System.currentTimeMillis());
                stm.executeUpdate();
            }
            return null;
        }));
    }

}

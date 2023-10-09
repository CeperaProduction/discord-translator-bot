package me.cepera.discord.bot.translator.repository.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import me.cepera.discord.bot.translator.repository.AutoTranslatingChannel;
import me.cepera.discord.bot.translator.repository.AutoTranslatingChannelRepository;
import me.cepera.discord.bot.translator.repository.sql.db.SQLiteDatabase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SQLiteAutoTranslatingChannelRepository extends SQLiteRepository implements AutoTranslatingChannelRepository {

    public SQLiteAutoTranslatingChannelRepository(SQLiteDatabase database) {
        super(database);
        prepareTable();
    }

    private void prepareTable() {
        connect(c->c.createStatement().execute("CREATE TABLE IF NOT EXISTS auto_channels ("
                    + "id integer PRIMARY KEY, "
                    + "channelId integer NOT NULL, "
                    + "languageCode text NOT NULL)"));
    }

    @Override
    public Flux<AutoTranslatingChannel> getAllChannels() {
        return Mono.fromSupplier(()->connect(c->{
                    List<AutoTranslatingChannel> channels = new ArrayList<>();
                    ResultSet rs = c.createStatement().executeQuery("SELECT channelId, languageCode FROM auto_channels");
                    while(rs.next()) {
                        channels.add(new AutoTranslatingChannel(rs.getLong(1), rs.getString(2)));
                    }
                    return channels;
                }))
                .flatMapIterable(result->result);
    }

    @Override
    public Mono<AutoTranslatingChannel> getChannel(long channelId) {
        return Mono.fromSupplier(()->connect(c->{
            PreparedStatement stm = c.prepareStatement("SELECT languageCode FROM auto_channels WHERE channelId = ?");
            stm.setLong(1, channelId);
            ResultSet rs = stm.executeQuery();
            if(rs.next()) {
                return new AutoTranslatingChannel(channelId, rs.getString(1));
            }
            return null;
        }));
    }

    @Override
    public Mono<Void> setChannel(AutoTranslatingChannel channel) {
        return Mono.fromRunnable(()->connect(c->{
            PreparedStatement stm = c.prepareStatement("UPDATE auto_channels SET languageCode = ? WHERE channelId = ?");
            stm.setString(1, channel.getLanguageCode());
            stm.setLong(2, channel.getChannelId());
            int count = stm.executeUpdate();
            if(count == 0) {
                stm = c.prepareStatement("INSERT INTO auto_channels (channelId, languageCode) VALUES (?, ?)");
                stm.setLong(1, channel.getChannelId());
                stm.setString(2, channel.getLanguageCode());
                stm.executeUpdate();
            }
            return null;
        }));
    }

    @Override
    public Mono<Void> removeChannel(long channelId) {
        return Mono.fromRunnable(()->connect(c->{
            PreparedStatement stm = c.prepareStatement("DELETE FROM auto_channels WHERE channelId = ?");
            stm.setLong(1, channelId);
            stm.executeUpdate();
            return null;
        }));
    }

}

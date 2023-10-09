package me.cepera.discord.bot.translator.di;

import java.nio.file.Paths;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.cepera.discord.bot.translator.repository.TranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.repository.sql.SQLiteTranslateLanguageUsageRepository;
import me.cepera.discord.bot.translator.repository.sql.db.SQLiteDatabase;

@Module
public class DataModule {

    @Provides
    @Singleton
    SQLiteDatabase sqLiteDatabase() {
        return new SQLiteDatabase(Paths.get("data", "data.sql"));
    }

    @Provides
    @Singleton
    TranslateLanguageUsageRepository translateLanguageUsageRepository(SQLiteDatabase sqLiteDatabase) {
        return new SQLiteTranslateLanguageUsageRepository(sqLiteDatabase);
    }

}

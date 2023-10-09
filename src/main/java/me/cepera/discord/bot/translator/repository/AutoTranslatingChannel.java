package me.cepera.discord.bot.translator.repository;

import java.util.Objects;

public class AutoTranslatingChannel {

    private final long channelId;

    private final String languageCode;

    public AutoTranslatingChannel(long channelId, String languageCode) {
        this.channelId = channelId;
        this.languageCode = languageCode;
    }

    public long getChannelId() {
        return channelId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, languageCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AutoTranslatingChannel other = (AutoTranslatingChannel) obj;
        return channelId == other.channelId && Objects.equals(languageCode, other.languageCode);
    }

    @Override
    public String toString() {
        return "AutoTranslatingChannel [channelId=" + channelId + ", languageCode=" + languageCode + "]";
    }

}

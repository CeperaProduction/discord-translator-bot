package me.cepera.discord.bot.translator.config;

import java.util.Objects;

import io.netty.util.internal.StringUtil;

public class DiscordBotConfig {

    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DiscordBotConfig other = (DiscordBotConfig) obj;
        return Objects.equals(key, other.key);
    }

    @Override
    public String toString() {
        return "DiscordBotConfig [key="+(StringUtil.isNullOrEmpty(key) ? "none" : "<key>")+"]";
    }

}

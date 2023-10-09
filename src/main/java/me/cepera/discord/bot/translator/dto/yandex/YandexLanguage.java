package me.cepera.discord.bot.translator.dto.yandex;

import java.util.Objects;

public class YandexLanguage {

    private String code;

    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        YandexLanguage other = (YandexLanguage) obj;
        return Objects.equals(code, other.code) && Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
        return "YandexLanguage [code=" + code + ", name=" + name + "]";
    }

}

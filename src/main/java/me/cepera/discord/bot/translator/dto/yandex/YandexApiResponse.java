package me.cepera.discord.bot.translator.dto.yandex;

import java.util.Objects;

public class YandexApiResponse {

    protected Integer code;

    protected String message;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "YandexApiResponse [code=" + code + ", message=" + message + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof YandexApiResponse))
            return false;
        YandexApiResponse other = (YandexApiResponse) obj;
        return Objects.equals(code, other.code) && Objects.equals(message, other.message);
    }

}

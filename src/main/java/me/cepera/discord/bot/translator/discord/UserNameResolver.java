package me.cepera.discord.bot.translator.discord;

import java.util.Optional;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;

public interface UserNameResolver {

    default String getUserName(Optional<Member> optMember, Optional<User> optUser) {
        return optMember.map(member->member.getDisplayName())
                .orElseGet(()->optUser.flatMap(User::getGlobalName).orElse(""));
    }

}

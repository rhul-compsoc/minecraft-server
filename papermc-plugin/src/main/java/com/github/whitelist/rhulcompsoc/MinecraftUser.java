package com.github.whitelist.rhulcompsoc;

public class MinecraftUser {
    private final String username;
    private final int verificationNumber;
    private final boolean banned;
    private final int verified;

    public MinecraftUser(final String username, final int verificationNumber, final boolean banned, final int verified) {
        this.username = username;
        this.verificationNumber = verificationNumber;
        this.banned = banned;
        this.verified = verified;
    }

    public String getUsername() {
        return username;
    }

    public int getVerificationNumber() {
        return verificationNumber;
    }

    public boolean isBanned() {
        return banned;
    }

    public int getVerified() {
        return verified;
    }
}

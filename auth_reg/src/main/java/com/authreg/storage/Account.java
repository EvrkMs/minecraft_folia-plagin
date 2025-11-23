package com.authreg.storage;

import java.util.UUID;

public class Account {
    private final UUID uuid;
    private final String name;
    private final String passwordHash;
    private final String lastIp;
    private final long createdAt;
    private final long updatedAt;
    private final String lastWorld;
    private final Double lastX;
    private final Double lastY;
    private final Double lastZ;
    private final Float lastYaw;
    private final Float lastPitch;

    public Account(UUID uuid, String name, String passwordHash, String lastIp, long createdAt, long updatedAt,
                   String lastWorld, Double lastX, Double lastY, Double lastZ, Float lastYaw, Float lastPitch) {
        this.uuid = uuid;
        this.name = name;
        this.passwordHash = passwordHash;
        this.lastIp = lastIp;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastWorld = lastWorld;
        this.lastX = lastX;
        this.lastY = lastY;
        this.lastZ = lastZ;
        this.lastYaw = lastYaw;
        this.lastPitch = lastPitch;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getLastIp() {
        return lastIp;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public String getLastWorld() {
        return lastWorld;
    }

    public Double getLastX() {
        return lastX;
    }

    public Double getLastY() {
        return lastY;
    }

    public Double getLastZ() {
        return lastZ;
    }

    public Float getLastYaw() {
        return lastYaw;
    }

    public Float getLastPitch() {
        return lastPitch;
    }
}

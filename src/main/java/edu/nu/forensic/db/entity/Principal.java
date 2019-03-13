package edu.nu.forensic.db.entity;

import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class Principal {
    @Id
    @Type(type = "uuid-char")
    private UUID uuid;

    private String type;

    private String userId;

    private String username;

    private String groupIds;

    public Principal(UUID uuid, String type, String userId, String username, String groupIds) {
        this.uuid = uuid;
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.groupIds = groupIds;
    }

    public Principal() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(String groupIds) {
        this.groupIds = groupIds;
    }
}
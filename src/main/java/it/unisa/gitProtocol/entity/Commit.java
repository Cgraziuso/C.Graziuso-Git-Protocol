package it.unisa.gitProtocol.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Commit implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String message;
    private final String repoName;
    private final Date timestamp;


    public Commit(String message, String repoName) {
        this.message = message;
        this.repoName = repoName;
        this.timestamp = new Date(System.currentTimeMillis());
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getMessage() {
        return message;
    }

    public String getRepoName() {
        return repoName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Commit{" +
                "message='" + message + '\'' +
                ", repoName='" + repoName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commit commit = (Commit) o;
        return Objects.equals(message, commit.message) &&
                Objects.equals(repoName, commit.repoName) &&
                Objects.equals(timestamp, commit.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, repoName, timestamp);
    }
}

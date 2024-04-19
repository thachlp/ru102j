package com.redislabs.university;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class RedisConfig {
    public static final String DEFAULT_HOST = "localhost";
    public static final Integer DEFAULT_PORT = 6379;
    public static final String DEFAULT_PASSWORD = "";
    private String host;
    private Integer port;
    private String password;

    @JsonProperty
    public String getHost() {
        return Objects.requireNonNullElse(host, DEFAULT_HOST);
    }

    @JsonProperty
    public void setHost(String host) {
        this.host = host;
    }

    @JsonProperty
    public Integer getPort() {
        return Objects.requireNonNullElse(port, DEFAULT_PORT);
    }

    @JsonProperty
    public void setPort(Integer port) {
        this.port = port;
    }

    @JsonProperty
    public String getPassword() {
        return Objects.requireNonNullElse(password, DEFAULT_PASSWORD);
    }

    @JsonProperty
    public void setPassword(String password) {
        this.password = password;
    }
}

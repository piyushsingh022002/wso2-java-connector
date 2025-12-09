package com.example.connector.dto;

public class DeleteUserRequest {
    private String code;
    private boolean is_terminated;
    private boolean is_active;

    // Getters and setters
    public String getCode() { return code; }
public void setCode(String code) { this.code = code; }
public boolean isIs_terminated() { return is_terminated; }
    public void setIs_terminated(boolean is_terminated) { this.is_terminated = is_terminated; }
    public boolean isIs_active() { return is_active; }
    public void setIs_active(boolean is_active) { this.is_active = is_active; }
}
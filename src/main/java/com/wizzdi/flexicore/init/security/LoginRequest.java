package com.wizzdi.flexicore.init.security;

public class LoginRequest {

  private String password;

  private String username;

  /**
   * @return password
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * @param password password to set
   * @return LoginRequest
   */
  public <T extends LoginRequest> T setPassword(String password) {
    this.password = password;
    return (T) this;
  }

  /**
   * @return username
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * @param username username to set
   * @return LoginRequest
   */
  public <T extends LoginRequest> T setUsername(String username) {
    this.username = username;
    return (T) this;
  }
}

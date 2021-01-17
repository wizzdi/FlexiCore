package com.flexicore.response;

import java.time.OffsetDateTime;

public class AuthenticationResponseCompatible {

	private String authenticationKey;
	private OffsetDateTime tokenExpirationDate;
	private String userId;

	public AuthenticationResponseCompatible(AuthenticationResponse other) {
		this.authenticationKey = other.getAuthenticationKey();
		this.tokenExpirationDate = other.getTokenExpirationDate();
		this.userId = other.getUserId();
	}

	public AuthenticationResponseCompatible() {
	}

	public String getAuthenticationKey() {
		return authenticationKey;
	}

	public <T extends AuthenticationResponseCompatible> T setAuthenticationKey(String authenticationKey) {
		this.authenticationKey = authenticationKey;
		return (T) this;
	}

	public OffsetDateTime getTokenExpirationDate() {
		return tokenExpirationDate;
	}

	public <T extends AuthenticationResponseCompatible> T setTokenExpirationDate(OffsetDateTime tokenExpirationDate) {
		this.tokenExpirationDate = tokenExpirationDate;
		return (T) this;
	}

	public String getUserId() {
		return userId;
	}

	public <T extends AuthenticationResponseCompatible> T setUserId(String userId) {
		this.userId = userId;
		return (T) this;
	}
}

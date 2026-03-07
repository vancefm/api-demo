package com.demo.application.security.token;

public class CreateTokenResponse {
    private String tokenId;
    private String tokenValue;

    public CreateTokenResponse() {}

    public CreateTokenResponse(String tokenId, String tokenValue) {
        this.tokenId = tokenId;
        this.tokenValue = tokenValue;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }
}

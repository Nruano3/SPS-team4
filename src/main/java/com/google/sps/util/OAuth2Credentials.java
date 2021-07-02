package com.google.sps.util;

/**
 * OAuth2 Credential class, designed to make 
 * easier access to the elements found 
 * inside an app client_secret.json file
 */


public class OAuth2Credentials {
    private String client_id;
    private String client_secret;
    private String project_id;
    private String auth_uri;
    private String token_uri;
    private String auth_provider_url;
    private String[] redirect_uris;

    public OAuth2Credentials() {}

    public void setClient_id(String client_id){this.client_id=client_id;}
    public String getClient_id(){
        return this.client_id;
    }

    public void setClient_secret(String client_secret){this.client_secret=client_secret;}
    public String getClient_secret() {
        return this.client_secret;
    }

    public void setProject_id(String project_id){this.project_id=project_id;}
    public String getProject_id() {
        return this.project_id;
    }
    public void setAuth_uri(String auth_uri){this.auth_uri=auth_uri;}
    public String getAuth_uri(){
        return this.auth_uri;
    }
    public void setToken_uri(String token_uri){this.token_uri=token_uri;}
    public String getToken_uri(){
        return this.token_uri;
    }

    public void setAuth_provider_url(String auth_provider_url){this.auth_provider_url = auth_provider_url;}
    public String getAuth_provider_url(){return this.auth_provider_url;}

    public void setRedirect_uris(String[] redirect_uris){this.redirect_uris = redirect_uris;}
    public String[] getRedirect_uris(){
        return this.redirect_uris;
    }
}

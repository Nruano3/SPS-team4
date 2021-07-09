package com.google.sps.util;


/**
 * This class is primarilly to package any necessary user information needed for the front end
 * Information will include:
 *          access_token
 *          Profile ID
 *          Username
 *          First Name
 *          Last Name
 *          email (if applicable)
 *          img src (if applicable)
 */
public class UserInfo {

    private String userName;
    private String firstName;
    private String lastName;
    private String access_token;
    private String profileId;
    private String userEmail;
    private String imgSrc;


    UserInfo(){}

    public void setUserName(String userName) {this.userName = userName;}
    public String getUserName() {return this.userName;}

    public void setFirstName(String firstName) {this.firstName = firstName;}
    public String getFirstName(){return this.firstName;}

    public void setLastName(String lastName) {this.lastName = lastName;}
    public String getLastName(){return this.lastName;}

    public void setAccess_token(String access_token) {this.access_token = access_token;}
    public String getAccess_token(){return this.access_token;}

    public void setProfileId(String profileId) {this.profileId = profileId;}
    public String getProfileId(){return this.profileId;}

    public void setUserEmail(String userEmail) {this.userEmail = userEmail;}
    public String getUserEmail(){return this.userEmail;}

    public void setImgSrc(String imgSrc) {this.imgSrc = imgSrc;}
    public String getImgSrc(){return this.imgSrc;}

}
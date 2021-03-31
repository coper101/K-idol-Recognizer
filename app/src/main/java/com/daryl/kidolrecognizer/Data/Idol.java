package com.daryl.kidolrecognizer.Data;

import java.util.ArrayList;

public class Idol {

    private String id, stageName, realName, group, entertainment,
            age, height, weight, bloodType, nationality;
    private boolean isFavorite;
    private ArrayList<Role> roles;
    private ArrayList<SNS> snsList;

    public Idol(String id, String stageName, String realName, String group, String entertainment,
                String age, String height, String weight, String bloodType, String nationality,
                boolean isFavorite,
                ArrayList<Role> roles, ArrayList<SNS> snsList) {
        this.id = id;
        this.stageName = stageName;
        this.realName = realName;
        this.group = group;
        this.entertainment = entertainment;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.bloodType = bloodType;
        this.nationality = nationality;
        this.isFavorite = isFavorite;
        this.roles = roles;
        this.snsList = snsList;
    }

    public Idol(String id, String stageName, String group) {
        this.id = id;
        this.stageName = stageName;
        this.group = group;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getEntertainment() {
        return entertainment;
    }

    public void setEntertainment(String entertainment) {
        this.entertainment = entertainment;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public ArrayList<Role> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<Role> roles) {
        this.roles = roles;
    }

    public ArrayList<SNS> getSnsList() {
        return snsList;
    }

    public void setSnsList(ArrayList<SNS> snsList) {
        this.snsList = snsList;
    }

}

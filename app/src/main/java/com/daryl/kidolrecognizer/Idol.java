package com.daryl.kidolrecognizer;

import java.util.ArrayList;

public class Idol {

    private String stageName, realName, group, entertainment,
            age, height, weight, bloodType;
    private ArrayList<Role> roles;

    public Idol(String stageName, String realName, String group, String entertainment,
                String age, String height, String weight, String bloodType, ArrayList<Role> roles) {
        this.stageName = stageName;
        this.realName = realName;
        this.group = group;
        this.entertainment = entertainment;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.bloodType = bloodType;
        this.roles = roles;
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

    public ArrayList<Role> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<Role> roles) {
        this.roles = roles;
    }
}

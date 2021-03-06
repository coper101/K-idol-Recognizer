package com.daryl.kidolrecognizer;

public class Idol {

    private String
            stageName,
            realName,
            dateOfBirth,
            height,
            weight,
            bloodType,
            zodiac,
            groupName,
            role,
            description;

    public Idol(String stageName,
                String realName,
                String dateOfBirth,
                String height,
                String weight,
                String bloodType,
                String zodiac,
                String groupName,
                String role,
                String description) {
        this.stageName = stageName;
        this.realName = realName;
        this.dateOfBirth = dateOfBirth;
        this.height = height;
        this.weight = weight;
        this.bloodType = bloodType;
        this.zodiac = zodiac;
        this.groupName = groupName;
        this.role = role;
        this.description = description;
    }

    public String getStageName() {
        return stageName;
    }

    public String getRealName() {
        return realName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getHeight() {
        return height;
    }

    public String getWeight() {
        return weight;
    }

    public String getBloodType() {
        return bloodType;
    }

    public String getZodiac() {
        return zodiac;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getRole() {
        return role;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Idol{" +
                "stageName='" + stageName + '\'' +
                ", realName='" + realName + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", height='" + height + '\'' +
                ", weight='" + weight + '\'' +
                ", bloodType='" + bloodType + '\'' +
                ", zodiac='" + zodiac + '\'' +
                ", groupName='" + groupName + '\'' +
                ", role='" + role + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

package com.backstage.system.domain.site;

public enum SiteResourceType {

    COURSE("course", "课程"),
    ;

    private final String type;
    private final String description;

    SiteResourceType(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public static SiteResourceType fromType(String type) {
        for (SiteResourceType value : SiteResourceType.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unknown type:" + type);
    }
}

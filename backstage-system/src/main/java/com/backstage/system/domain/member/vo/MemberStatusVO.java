package com.backstage.system.domain.member.vo;

public class MemberStatusVO {
    private String memberType;
    private String memberName;
    private Integer roleId;
    private String expireTime;
    private Boolean active;
    private Long remainingDays;

    public String getMemberType() { return memberType; }
    public void setMemberType(String memberType) { this.memberType = memberType; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }
    public String getExpireTime() { return expireTime; }
    public void setExpireTime(String expireTime) { this.expireTime = expireTime; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Long getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Long remainingDays) { this.remainingDays = remainingDays; }
}

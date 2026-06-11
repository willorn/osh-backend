package com.backstage.system.domain.member.vo;

import com.backstage.system.domain.member.OshMemberPlan;
import java.util.List;

public class MemberCenterVO {
    private MemberStatusVO current;
    private MemberStatusVO vip;
    private MemberStatusVO smallClass;
    private List<OshMemberPlan> plans;
    private Boolean founder;

    public MemberStatusVO getCurrent() { return current; }
    public void setCurrent(MemberStatusVO current) { this.current = current; }
    public MemberStatusVO getVip() { return vip; }
    public void setVip(MemberStatusVO vip) { this.vip = vip; }
    public MemberStatusVO getSmallClass() { return smallClass; }
    public void setSmallClass(MemberStatusVO smallClass) { this.smallClass = smallClass; }
    public List<OshMemberPlan> getPlans() { return plans; }
    public void setPlans(List<OshMemberPlan> plans) { this.plans = plans; }
    public Boolean getFounder() { return founder; }
    public void setFounder(Boolean founder) { this.founder = founder; }
}

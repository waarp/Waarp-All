package org.waarp.openr66.pojo;

/**
 * Business data object
 */
public class Business {

    private String hostid;

    private String business;

    private String roles;

    private String aliases;

    private String others;

    private UpdatedInfo updatedInfo = UpdatedInfo.UNKNOWN;

    /**
     * Empty constructor for compatibility issues
     */
    @Deprecated
    public Business() {}

    public Business(String hostid, String business, String roles, 
            String aliases, String others, UpdatedInfo updatedInfo) {
        this(hostid, business, roles, aliases, others);
        this.updatedInfo = updatedInfo;
    }

    public Business(String hostid, String business, String roles, 
            String aliases, String others) {
        this.hostid = hostid;
        this.business = business;
        this.roles = roles;
        this.aliases = aliases;
        this.others = others;
    }

    public String getHostid() {
        return this.hostid;
    }

    public void setHostid(String hostid) {
        this.hostid = hostid;
    }

    public String getBusiness() {
        return this.business;
    }

    public void setBusiness(String business) {
        this.business = business;
    }

    public String getRoles() {
        return this.roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getAliases() {
        return this.aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    public String getOthers() {
        return this.others;
    }

    public void setOthers(String others) {
        this.others = others;
    }

    public UpdatedInfo getUpdatedInfo() {
        return this.updatedInfo;
    }

    public void setUpdatedInfo(UpdatedInfo info) {
        this.updatedInfo = info;
    }
}

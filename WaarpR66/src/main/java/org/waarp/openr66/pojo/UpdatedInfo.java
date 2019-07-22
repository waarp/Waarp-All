package org.waarp.openr66.pojo;

import org.waarp.common.database.data.AbstractDbData;

import java.util.HashMap;
import java.util.Map;

public enum UpdatedInfo {
    /**
     * Unknown run status
     */
    UNKNOWN(0),
    /**
     * Not updated run status
     */
    NOTUPDATED(1),
    /**
     * Interrupted status (stop or cancel)
     */
    INTERRUPTED(2),
    /**
     * Updated run status meaning ready to be submitted
     */
    TOSUBMIT(3),
    /**
     * In error run status
     */
    INERROR(4),
    /**
     * Running status
     */
    RUNNING(5),
    /**
     * All done run status
     */
    DONE(6);

    private int id;

    private static Map<Integer, UpdatedInfo> map
            = new HashMap<Integer, UpdatedInfo>();

    static {
        for (UpdatedInfo updatedInfo: UpdatedInfo.values()) {
            map.put(updatedInfo.id, updatedInfo);
        }
    }

    UpdatedInfo(final int updatedInfo) {
        id = updatedInfo;
    }

    public static UpdatedInfo valueOf(int updatedInfo) {
        if (!map.containsKey(updatedInfo)) {
            return UNKNOWN;
        }
        return map.get(updatedInfo);
    }

    public boolean equals(AbstractDbData.UpdatedInfo legacy) {
        return this.ordinal() == legacy.ordinal();
    }

    public static UpdatedInfo fromLegacy(AbstractDbData.UpdatedInfo info) {
        return valueOf(info.name());
    }

    public AbstractDbData.UpdatedInfo getLegacy() {
        return AbstractDbData.UpdatedInfo.valueOf(this.name());
    }
}

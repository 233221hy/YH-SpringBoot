package cn.xfywz.guozespring.constant;

public enum DataAuth {
    ALL(1),       // 所有数据
    COLLEGE(2),   // 院级数据
    OWN(3);       // 普通数据

    private final int value;

    DataAuth(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DataAuth fromValue(Integer value) {
        if (value == null) return null;
        for (DataAuth auth : values()) {
            if (auth.value == value) {
                return auth;
            }
        }
        throw new IllegalArgumentException("Invalid DataAuth value: " + value);
    }
}
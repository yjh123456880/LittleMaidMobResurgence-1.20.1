package net.sistr.littlemaidmobresurgence.entity.util;

/**
 * [zh] 女仆移动模式枚举：自由行动 / 跟随（护卫）/ 沿激活红石追踪。
 * [en] Maid moving-mode enum: free-move / escort (follow) / tracer (redstone trace).
 * [ja] メイドの移動状態の enum：自由行動 / 追従（護衛）/ レッドストーン追跡。
 */
public enum MovingMode {
    FREEDOM("Freedom", 0),
    ESCORT("Escort", 1),
    TRACER("Tracer", 2);
    private final String name;
    private final int id;

    MovingMode(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public static MovingMode fromName(String name) {
        for (MovingMode state : MovingMode.values()) {
            if (state.getName().equals(name)) {
                return state;
            }
        }
        throw new IllegalArgumentException("存在しないMovingStateです。 : " + name);
    }

    public static MovingMode fromId(int id) {
        for (MovingMode state : MovingMode.values()) {
            if (state.getId() == id) {
                return state;
            }
        }
        throw new IllegalArgumentException("存在しないMovingStateです。 : " + id);
    }
}

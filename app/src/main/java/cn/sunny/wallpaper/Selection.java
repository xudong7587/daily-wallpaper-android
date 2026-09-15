package cn.sunny.wallpaper;
import java.util.*;
public final class Selection {
    public static String choose(List<String> ids, String previous, Random random) {
        if (ids.isEmpty()) throw new IllegalArgumentException("图库为空，请先导入图片或刷新链接");
        List<String> choices = new ArrayList<>(ids);
        if (choices.size() > 1) choices.remove(previous);
        return choices.get(random.nextInt(choices.size()));
    }
}

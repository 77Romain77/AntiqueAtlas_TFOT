package hunternif.mc.impl.atlas.client;

import java.lang.reflect.Method;

/** Optional Iris/Oculus bridge without a compile-time or runtime dependency. */
public final class ShaderCompat {
    private static final String[] IRIS_API_CLASSES = {
            "net.irisshaders.iris.api.v0.IrisApi",
            "net.coderbot.iris.api.v0.IrisApi"
    };
    private static final ApiAccess API = findApi();

    private ShaderCompat() {
    }

    /** Returns true only while Iris or Oculus is actively running a shader pack. */
    public static boolean isShaderPackInUse() {
        if (API == null) return false;
        try {
            Object api = API.getInstance().invoke(null);
            return Boolean.TRUE.equals(API.isShaderPackInUse().invoke(api));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private static ApiAccess findApi() {
        for (String className : IRIS_API_CLASSES) {
            try {
                Class<?> apiClass = Class.forName(className, false, ShaderCompat.class.getClassLoader());
                return new ApiAccess(apiClass.getMethod("getInstance"),
                        apiClass.getMethod("isShaderPackInUse"));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Iris/Oculus is optional; try the other historical package.
            }
        }
        return null;
    }

    private record ApiAccess(Method getInstance, Method isShaderPackInUse) {
    }
}

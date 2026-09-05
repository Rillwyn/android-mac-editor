# libxposed (Modern Xposed API / API 101) —— 官方推荐的混淆规则
# 来源：https://github.com/libxposed/api
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keepattributes RuntimeVisibleAnnotations
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

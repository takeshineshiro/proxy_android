
-dontusemixedcaseclassnames

-keepattributes SourceFile,LineNumberTable,EnclosingMethod,InnerClasses

-keep class android.net.Network {
	public final int netId;
}

-dontnote com.subao.common.parallel.NetworkWatcherImpl_Support$NetworkImpl

-keepnames public interface com.subao.vpn.JniCallback

-keep class com.subao.vpn.VPNJni {
    public static <methods>;
    native <methods>;
}

-keep public class com.subao.gamemaster.GMKey

-keepclassmembers enum com.subao.common.net.NetTypeDetector$NetType {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepattributes InnerClasses
-keep interface com.subao.gamemaster.GameMaster$OnAccelSwitchListener {
    public <methods>;
}
-keep interface com.subao.gamemaster.GameMaster$I1 {
    public <methods>;
}
-keep interface com.subao.gamemaster.GameMaster$I2 {
    public <methods>;
}

-keep public class com.subao.gamemaster.GameMaster {
    public <fields>;
    public <methods>;

    static void xy(android.content.Context);
    static android.util.Pair x1();
    static void x2(java.lang.String, java.lang.String);
    static java.lang.String x3(android.content.Context);
    static void x4();
    static java.io.File x5(java.io.File, boolean);
    static java.io.File x6(boolean);
    static boolean x7(com.subao.gamemaster.GameMaster$OnAccelSwitchListener);
    static java.io.File x8(android.content.Context);
    static void x9(java.lang.String, com.subao.gamemaster.GameMaster$I1);
    static java.lang.Object x10(android.content.Context, com.subao.gamemaster.GameMaster$I2);
    static void x11(java.lang.Object);
}


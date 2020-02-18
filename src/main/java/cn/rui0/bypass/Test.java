package cn.rui0.bypass;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;

import javax.management.BadAttributeValueExpException;
import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by ruilin on 2020/2/18.
 * 测试是否拦截成功
 */
public class Test {
    public static void main(String[] args) throws Exception {

        //CC5
        final String[] execArgs = new String[] { "open /System/Applications/Calculator.app" };
        // inert chain for setup
        final Transformer transformerChain = new ChainedTransformer(
                new Transformer[]{ new ConstantTransformer(1) });
        // real chain for after setup
        final Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[] {
                        String.class, Class[].class }, new Object[] {
                        "getRuntime", new Class[0] }),
                new InvokerTransformer("invoke", new Class[] {
                        Object.class, Object[].class }, new Object[] {
                        null, new Object[0] }),
                new InvokerTransformer("exec",
                        new Class[] { String.class }, execArgs),
                new ConstantTransformer(1) };

        final Map innerMap = new HashMap();

        final Map lazyMap = LazyMap.decorate(innerMap, transformerChain);

        TiedMapEntry entry = new TiedMapEntry(lazyMap, "foo");

        BadAttributeValueExpException val = new BadAttributeValueExpException(null);
        Field valfield = val.getClass().getDeclaredField("val");
        SocketServer.setAccessible(valfield);
        valfield.set(val, entry);

        SocketServer.setFieldValue(transformerChain, "iTransformers", transformers); // arm with actual transformer chain


        FileOutputStream fos = new FileOutputStream("testBlackList.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(val);
        oos.flush();

        Set blacklist = new HashSet() {{
            add("javax.management.BadAttributeValueExpException");
            add("org.apache.commons.collections.keyvalue.TiedMapEntry");
            add("org.apache.commons.collections.functors.ChainedTransformer");
        }};



        BlacklistObjectInputStream ois = new BlacklistObjectInputStream(new FileInputStream("testBlackList.ser"),blacklist);
        ois.readObject();
    }
}

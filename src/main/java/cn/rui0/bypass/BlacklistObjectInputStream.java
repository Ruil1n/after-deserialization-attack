package cn.rui0.bypass;

import java.io.*;
import java.util.Set;

/**
 * Created by ruilin on 2020/2/18.
 */
class BlacklistObjectInputStream extends ObjectInputStream {
    public Set blacklist;

    public BlacklistObjectInputStream(InputStream inputStream, Set bl) throws IOException {
        super(inputStream);
        blacklist = bl;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass cls) throws IOException, ClassNotFoundException {
        // System.out.println(cls.getName());
        if (blacklist.contains(cls.getName())) {
            throw new InvalidClassException("Unexpected serialized class", cls.getName());
        }
        return super.resolveClass(cls);
    }
}

package sentry_tiny;

/** To be used as:
 *  final BytesLoader classLoader = new BytesLoader(bytecode);
 *  final Class<?> newClass = classLoader.loadClass("com.example.myClazz");
 */
public class BytesLoader extends ClassLoader {
    private byte[] bytes;

    public BytesLoader(byte[] bytes) {
        super();
        this.bytes = bytes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return defineClass(name, bytes, 0, bytes.length);
    }
}

/**
 * Or directly from Clojure
(def dcl (clojure.lang.DynamicClassLoader.))

(defn dynamically-load-class!
  [class-loader class-name]
  (let [class-reader (clojure.asm.ClassReader. class-name)]
    (when class-reader
      (let [bytes (.-b class-reader)]
        (.defineClass class-loader
                      class-name
                      bytes
                      "")))))

(dynamically-load-class! dcl "java.lang.Long")
(dynamically-load-class! dcl 'org.joda.time.DateTime)
 */

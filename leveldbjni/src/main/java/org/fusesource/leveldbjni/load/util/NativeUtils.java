package org.fusesource.leveldbjni.load.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class NativeUtils {
 
    /**
     * The minimum length a prefix for a file has to have according to {@link File#createTempFile(String, String)}}.
     */
    private static final int MIN_PREFIX_LENGTH = 3;
    public static final String NATIVE_FOLDER_PATH_PREFIX = "nativeutils";

    /**
     * Temporary directory which will contain the DLLs.
     */
    private static File temporaryDir;

    /**
     * Private constructor - this class will never be instanced
     */
    private NativeUtils() {
    }

    public static synchronized void loadLibraryFromJar(String path) throws IOException {
        loadLibraryFromJar(path, null);
    }

    /**
     * 从jar包中加载动态库
     * 先将jar包中的动态库复制到系统临时文件夹，然后加载动态库，并且在JVM退出时自动删除。
     * The file from JAR is copied into system temporary directory and then loaded. The temporary file is deleted after
     * exiting.
     * Method uses String as filename because the pathname is "abstract", not system-dependent.
     * 
     * @param path 要加载动态库的路径，必须以'/'开始,比如 /lib/mylib.so，必须以'/'开始
     * @param loadClass 用于提供{@link ClassLoader}加载动态库的类，如果为null,则使用NativeUtils.class
     * @throws IOException 动态库读写错误
     * @throws FileNotFoundException 没有在jar包中找到指定的文件
     */
    public static synchronized void loadLibraryFromJar(String path, Class<?> loadClass) throws IOException {
 
        if (null == path || !path.startsWith("/")) {
            throw new IllegalArgumentException("The path has to be absolute (start with '/').");
        }
 
        // Obtain filename from path
        String[] parts = path.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;
 
        // Check if the filename is okay
        if (filename == null || filename.length() < MIN_PREFIX_LENGTH) {
            throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
        }
 
        // 创建临时文件夹
        if (temporaryDir == null) {
            temporaryDir = createTempDirectory(NATIVE_FOLDER_PATH_PREFIX);
            temporaryDir.deleteOnExit();
        }
		    // 临时文件夹下的动态库名
        File temp = new File(temporaryDir, filename);
        Class<?> clazz = loadClass == null ? NativeUtils.class	: loadClass;
        // 从jar包中复制文件到系统临时文件夹
        try (InputStream is = clazz.getResourceAsStream(path)) {
            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            temp.delete();
            throw e;
        } catch (NullPointerException e) {
            temp.delete();
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }
		    // 加载临时文件夹中的动态库
        try {
            System.load(temp.getAbsolutePath());
        } finally {
        	// 设置在JVM结束时删除临时文件
        	temp.deleteOnExit();
        }
    }

    private static File createTempDirectory(String prefix) throws IOException {
        String tempDir = "/tmp";
        File generatedDir = new File(tempDir, prefix + System.nanoTime());
        
        if (!generatedDir.mkdir())
            throw new IOException("Failed to create temp directory " + generatedDir.getName());
        return generatedDir;
    }
}
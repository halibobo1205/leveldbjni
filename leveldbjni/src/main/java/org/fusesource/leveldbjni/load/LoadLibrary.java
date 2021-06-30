package org.fusesource.leveldbjni.load;



import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import org.fusesource.leveldbjni.load.util.NativeUtils;
import org.fusesource.leveldbjni.load.util.OSinfo;

public class LoadLibrary {

  private static final String LIB_STD_X86_64 = "/usr/lib64/libstdc++.so.6.0.22";
  private static final String JAR_PATH_X86_64 = "/lib/linux64/libstdc++_6.0.22.so";
  //TODO
  private static final String LIB_STD_X86 = "/usr/lib/libstdc++.so.6.0.22";
  private static final String JAR_PATH_X86 = "/lib/linux32/libstdc++_6.0.22.so";

  private LoadLibrary() {
    throw new AccessControlException("access deny");
  }

  private static volatile boolean isLoaded = false;

  public synchronized static void load() {
    if (!isLoaded) {
      try {
        if (OSinfo.isLinux() && OSinfo.isX64()) {
          File file = new File(LIB_STD_X86_64);
          if (!file.exists()) {
            NativeUtils.loadLibraryFromJar(JAR_PATH_X86_64);
          }
        }
        isLoaded = true;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

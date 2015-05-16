package org.silvertunnel_ng.androidsample.dexutil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import dalvik.system.DexFile;

public class DexHelper {
    private AssetManager assetManager;
    // public static final String NAME_DEX = "classes2.dex";
    private static final String NAME_DIR_INTERN = "dex", NAME_DIR_OPTIMIZED = "optDex", SUFFIX_ODEX = ".odex";
    private static final int BUF_SIZE = 1024;
    private final File dirInternal;
    private final File dirOptimized;
    private final ClassLoader loader;
    private final Set<String> jarsToLoad;

    public DexHelper(Context ctx, ClassLoader loader) {
        this.assetManager = ctx.getAssets();
        dirInternal = ctx.getDir(NAME_DIR_INTERN, Context.MODE_PRIVATE);
        dirOptimized = ctx.getDir(NAME_DIR_OPTIMIZED, Context.MODE_PRIVATE);
        this.loader = loader;
        jarsToLoad = new HashSet<String>();
    }

    public void copyDexFiles() throws IOException {
        for (String str : assetManager.list(".")) {
            if (str.endsWith(".dex")) {
                JarOutputStream jOut = new JarOutputStream(new FileOutputStream(dirInternal + File.separator + str));
                copyDexFile(str, jOut);
            }
        }
    }

    private void copyDexFile(String fName, JarOutputStream bOut) throws RuntimeException {
        BufferedInputStream bin = null;
        try {
            bin = new BufferedInputStream(assetManager.open(fName));

            JarEntry e = new JarEntry("classes.dex");
            bOut.putNextEntry(e);
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while ((len = bin.read(buf, 0, BUF_SIZE)) > 0) {
                bOut.write(buf, 0, len);
            }
            bOut.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                if (bOut != null)
                    bOut.close();
                if (bin != null)
                    bin.close();
            } catch (Exception e) {
                // nothing to do here...
            }
        }
    }

    public void loadClasses() {
        try {
            for (String jar : jarsToLoad) {
                DexFile dexFile = DexFile.loadDex(dirInternal + File.separator + jar, dirOptimized + File.separator + jar
                        + SUFFIX_ODEX, 0);
                for (Enumeration<String> classNames = dexFile.entries(); classNames.hasMoreElements(); ) {
                    String className = classNames.nextElement();
                    Log.d("SilverTunnel-NG", "Loading Class: " + className);
                    dexFile.loadClass(className, loader);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

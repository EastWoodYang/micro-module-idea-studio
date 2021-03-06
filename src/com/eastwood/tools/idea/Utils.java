package com.eastwood.tools.idea;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.*;

public class Utils {

    public static void createMicroModule(File moduleDir, String microModuleName, String packageName) {
        File microModuleDir = new File(moduleDir, microModuleName);
        microModuleDir.mkdirs();

        File buildFile = new File(microModuleDir, "build.gradle");
        Utils.addMicroModuleBuildScript(buildFile);

        File libs = new File(microModuleDir, "libs");
        libs.mkdirs();

        File srcDir = new File(microModuleDir, "src");
        srcDir.mkdirs();
        String packagePath = packageName.replace(".", "/");
        String[] types = new String[]{"androidTest", "main", "test"};
        for (String type : types) {
            new File(srcDir, type + File.separator + "java" + File.separator + packagePath).mkdirs();
        }

        File resDir = new File(srcDir, "main/res");
        resDir.mkdir();

        String[] resDirs = new String[]{"drawable", "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi", "layout", "values"};
        for (String type : resDirs) {
            new File(resDir, type).mkdirs();
        }

        File manifestFile = new File(srcDir, "main/AndroidManifest.xml");
        String content = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    package=\"" + packageName + "\">\n        <application>\n            \n        </application>\n</manifest>";
        Utils.write(manifestFile, content);
    }

    public static void includeMicroModule(File buildFile, String microModuleName) {
        if (!buildFile.exists()) {
            return;
        }

        StringBuilder result = new StringBuilder();
        boolean include = false;
        try {
            BufferedReader br = new BufferedReader(new FileReader(buildFile));
            String s = null;
            while ((s = br.readLine()) != null) {
                if (s.trim().equals("microModule {")) {
                    include = true;
                    s = s + System.lineSeparator() + "    include ':" + microModuleName + "'";
                }
                result.append(s + System.lineSeparator());
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (include) {
            Utils.write(buildFile, result.toString());
        } else {
            addMicroModuleExtension(buildFile, microModuleName);
        }
    }

    public static void applyMicroModulePlugin(File buildFile) {
        if (!buildFile.exists()) {
            return;
        }
        String buildScript = Utils.read(buildFile);
        buildScript = "apply plugin: 'micro-module'\n" + buildScript;
        write(buildFile, buildScript);
    }

    public static void moveSrcDir(File moduleDir) {
        File targetDir = new File(moduleDir, "main/src");
        File sourceDir = new File(moduleDir, "src");
        copy(sourceDir, sourceDir.getAbsolutePath(), targetDir);
        delete(sourceDir);

        File libs = new File(moduleDir, "main/libs");
        libs.mkdirs();

        File buildFile = new File(moduleDir, "main/build.gradle");
        addMicroModuleBuildScript(buildFile);
    }

    public static void addMicroModuleBuildScript(File buildFile) {
        String buildScript = "// MicroModule build file where you can declare MicroModule dependencies.\n" +
                "\n" +
                "dependencies {\n" +
                "    implementation fileTree(dir: '" + buildFile.getParentFile().getName() + "/libs', include: ['*.jar'])\n" +
                "}\n";
        Utils.write(buildFile, buildScript);
    }

    public static void addMicroModuleExtension(File buildFile) {
        addMicroModuleExtension(buildFile, null);
    }

    public static void addMicroModuleExtension(File buildFile, String microModuleName) {
        if (!buildFile.exists()) {
            return;
        }
        String extension = "microModule {\n" +
                (microModuleName == null ? "\n" : "    include ':" + microModuleName + "'\n") +
                "}\n";

        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(buildFile));
            String s = null;
            while ((s = br.readLine()) != null) {
                if ("dependencies {".equals(s)) {
                    s = extension + System.lineSeparator() + s;
                }
                result.append(s + System.lineSeparator());
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Utils.write(buildFile, result.toString());
    }

    public static void copy(File file, String prefix, File targetDir) {
        if (file.isDirectory()) {

            String packageName = file.getAbsolutePath().replace(prefix, "");
            File target = new File(targetDir, packageName);
            if (!target.exists()) {
                target.mkdir();
            }

            for (File childFile : file.listFiles()) {
                copy(childFile, prefix, targetDir);
            }
        } else {
            String packageName = file.getParent().replace(prefix, "");
            File targetParent = new File(targetDir, packageName);
            if (!targetParent.exists()) targetParent.mkdirs();

            File target = new File(targetParent, file.getName());

            InputStream input = null;
            OutputStream output = null;
            try {
                input = new FileInputStream(file);
                output = new FileOutputStream(target);
                byte[] buf = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buf)) > 0) {
                    output.write(buf, 0, bytesRead);
                }
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void delete(File file) {
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                delete(childFile);
            }
        }
        file.delete();
    }

    public static void write(File target, String content) {
        try {
            Writer writer = null;
            writer = new FileWriter(target);
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(content);
            bw.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String read(File target) {
        if (!target.exists()) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(target));
            String s = null;
            while ((s = br.readLine()) != null) {
                result.append(s + System.lineSeparator());
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static File getModuleDir(Module module) {
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        if (contentRoots.length > 0) {
            return new File(contentRoots[0].getCanonicalPath());
        } else {
            return new File(module.getModuleFile().getParent().getCanonicalPath());
        }
    }

}

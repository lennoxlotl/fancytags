/*
 * Copyright (c) 2021 Lennox
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.lennox.fancytags.inject;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Contains a lot of hacky tricks to make this work in the first place
 * <p>
 * DO NOT blame the code, this is just messy due to hacky tricks
 *
 * @author Lennox
 */
public class FancyTagsTransformer implements IClassTransformer {

  private static final List<String> CLASSES_TO_MAP = Lists.newArrayList(
    "de.lennox.fancytags.inject.mixin.entity.MixinRendererLivingEntity",
    "de.lennox.fancytags.inject.mixin.labymod.MixinRenderPlayerImpl");
  private static final File ADDON_PATH = new File("LabyMod/addons-1.8/FancyTags-1.0.jar");
  private boolean forge;

  public FancyTagsTransformer() {
    // Check for forge, this is required because on forge classes have to be transformed
    try {
      Class.forName("net.minecraftforge.fml.relauncher.FMLSecurityManager");
      forge = true;
    } catch (ClassNotFoundException ignored) {
    } catch (ExceptionInInitializerError e) {
      forge = true;
    }
    System.out.println("Injecting mixins...");
    // Get the correct class loader
    ClassLoader classLoader = Launch.class.getClassLoader();
    try {
      // Call the addURL method in the class loader
      Method method = classLoader.getClass().getSuperclass().getDeclaredMethod("addURL", URL.class);
      method.setAccessible(true);
      method.invoke(classLoader, ADDON_PATH.toURI().toURL());
      /// Load all classes to the class loader
      for (String s : classesToLoad()) {
        System.out.println("Loading class " + s + ".class");
        try {
          classLoader.loadClass(s);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
          e.printStackTrace();
        }
      }
      System.out.println("Injected " + ADDON_PATH.getName() + " to class path.");
    } catch (MalformedURLException | NoSuchMethodException | InvocationTargetException |
             IllegalAccessException e) {
      e.printStackTrace();
    }
    // Initialize mixins
    MixinBootstrap.init();
    // Add the mixin configuration
    Mixins.addConfiguration("client.mixins.json");
    // Set the environment to client
    MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
    System.out.println("Injected mixins successfully!");
  }

  public static List<String> classesToLoad() {
    return jarContentAsList("asm.txt");
  }

  private static List<String> jarContentAsList(String path) {
    return new BufferedReader(
      new InputStreamReader(FancyTagsTransformer.class.getResourceAsStream("/" + path))).lines()
      .collect(Collectors.toList());
  }


  @Override
  public byte[] transform(String name, String transformedName, byte[] basicClass) {
    // Transform required classes
    if (CLASSES_TO_MAP.contains(name)) {
      // Only transform on forge
      if (forge) {
        try {
          InputStream resourceAsStream = getClass().getResourceAsStream("/srg/" + name);
          return IOUtils.toByteArray(resourceAsStream);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return basicClass;
  }
}

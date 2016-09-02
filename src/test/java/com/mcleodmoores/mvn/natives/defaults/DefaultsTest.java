/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives.defaults;

import static org.testng.Assert.assertEquals;

import java.util.Properties;

import org.testng.annotations.Test;

import com.mcleodmoores.mvn.natives.ArchSource;
import com.mcleodmoores.mvn.natives.DynamicLib;
import com.mcleodmoores.mvn.natives.Executable;
import com.mcleodmoores.mvn.natives.HeaderFile;
import com.mcleodmoores.mvn.natives.Source;
import com.mcleodmoores.mvn.natives.StaticLib;
import com.mcleodmoores.mvn.natives.defaults.Defaults;

/**
 * Tests the {@link Defaults} class.
 */
@Test
public class DefaultsTest {

  public void testLoadAndSaveResource () {
    final Defaults instance = Defaults.get ("windows");
    assertEquals (instance.getIdentifier (), "windows");
    final Properties properties = new Properties ();
    instance.save (properties);
    // for (final Object key : properties.keySet ()) {
    // System.out.println (key + " = \"" + properties.get (key) + "\"");
    // }
    assertEquals (properties.size (), 24);
  }

  public void testMissingResource () {
    final Defaults instance = Defaults.get ("foo");
    assertEquals (instance.getIdentifier (), "none");
  }

  public void testNullResource () {
    final Defaults instance = Defaults.get (null);
    assertEquals (instance.getIdentifier (), "none");
  }

  public void testSourceDefaults_path () {
    final Properties properties = new Properties ();
    properties.setProperty ("header.path", "Bar");
    final Defaults defaults = new Defaults ("test", properties);
    final Source bean1 = new HeaderFile ();
    bean1.setPath ("Foo");
    bean1.setPattern ("*");
    final Source bean2 = new HeaderFile ();
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.toString (), "HeaderFile, path:Foo, pattern:*");
    assertEquals (bean2.toString (), "HeaderFile, path:Bar");
  }

  public void testSourceDefaults_pattern () {
    final Properties properties = new Properties ();
    properties.setProperty ("header.pattern", "*.bar");
    final Defaults defaults = new Defaults ("test", properties);
    final Source bean1 = new HeaderFile ();
    bean1.setPath ("Foo");
    bean1.setPattern ("*.foo");
    final Source bean2 = new HeaderFile ();
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.toString (), "HeaderFile, path:Foo, pattern:*.foo");
    assertEquals (bean2.toString (), "HeaderFile, pattern:*.bar");
  }

  public void testSourceDefaults_save () {
    final Properties properties = new Properties ();
    final Defaults.SourceDefaults defaults = new Defaults.ArchSourceDefaults ();
    defaults.save (properties, "x");
    assertEquals (properties.toString (), "{}");
    properties.clear ();
    defaults.setPath ("Foo");
    defaults.setPattern ("*.foo");
    defaults.save (properties, "x");
    assertEquals (properties.toString (), "{x.path=Foo, x.pattern=*.foo}");
  }

  public void testArchSourceDefaults_arch () {
    final Properties properties = new Properties ();
    properties.setProperty ("static.arch", "i386");
    final Defaults defaults = new Defaults ("test", properties);
    final ArchSource bean1 = new StaticLib ();
    bean1.setPath ("Foo");
    bean1.setArch ("x64");
    final ArchSource bean2 = new StaticLib ();
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.toString (), "StaticLib, path:Foo, arch:x64");
    assertEquals (bean2.toString (), "StaticLib, arch:i386");
  }

  public void testArchSourceDefaults_save () {
    final Properties properties = new Properties ();
    final Defaults.ArchSourceDefaults defaults = new Defaults.ArchSourceDefaults ();
    defaults.save (properties, "x");
    assertEquals (properties.toString (), "{}");
    properties.clear ();
    defaults.setPath ("foo");
    defaults.setArch ("i386");
    defaults.save (properties, "x");
    assertEquals (properties.toString (), "{x.path=foo, x.arch=i386}");
  }

  public void testDynamicLibDefaults_headers () {
    final Properties properties = new Properties ();
    properties.setProperty ("dynamic.header", "all;nothing;partial");
    properties.setProperty ("all.path", "A");
    properties.setProperty ("all.pattern", "*.a");
    properties.setProperty ("partial.path", "C");
    properties.setProperty ("header.path", "include");
    properties.setProperty ("header.pattern", "*.h");
    final Defaults defaults = new Defaults ("test", properties);
    final DynamicLib bean1 = new DynamicLib ();
    bean1.setArch ("i386");
    bean1.setHeaders (new HeaderFile[0]);
    final DynamicLib bean2 = new DynamicLib ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getHeaders (), null);
    final HeaderFile all = new HeaderFile ();
    all.setPath ("A");
    all.setPattern ("*.a");
    final HeaderFile nothing = new HeaderFile ();
    nothing.setPath ("include");
    nothing.setPattern ("*.h");
    final HeaderFile partial = new HeaderFile ();
    partial.setPath ("C");
    partial.setPattern ("*.h");
    assertEquals (bean2.getHeaders (), new HeaderFile[] { all, nothing, partial });
  }

  public void testDynamicLibDefaults_noHeaders () {
    final Properties properties = new Properties ();
    properties.setProperty ("dynamic.header", "");
    properties.setProperty ("header.path", "include");
    properties.setProperty ("header.pattern", "*.h");
    final Defaults defaults = new Defaults ("test", properties);
    final DynamicLib bean1 = new DynamicLib ();
    bean1.setArch ("i386");
    bean1.setHeaders (new HeaderFile[0]);
    final DynamicLib bean2 = new DynamicLib ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getHeaders (), null);
    assertEquals (bean2.getHeaders (), null);
  }

  public void testDynamicLibDefaults_implibs () {
    final Properties properties = new Properties ();
    properties.setProperty ("dynamic.implib", "all;nothing;partial");
    properties.setProperty ("all.path", "A");
    properties.setProperty ("all.pattern", "*.a");
    properties.setProperty ("all.arch", "i386");
    properties.setProperty ("partial.path", "C");
    properties.setProperty ("static.path", "lib");
    properties.setProperty ("static.pattern", "*.lib");
    final Defaults defaults = new Defaults ("test", properties);
    final DynamicLib bean1 = new DynamicLib ();
    bean1.setArch ("i386");
    bean1.setImplibs (new StaticLib[0]);
    final DynamicLib bean2 = new DynamicLib ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getImplibs (), null);
    final StaticLib all = new StaticLib ();
    all.setPath ("A");
    all.setPattern ("*.a");
    all.setArch ("i386");
    final StaticLib nothing = new StaticLib ();
    nothing.setPath ("lib");
    nothing.setPattern ("*.lib");
    final StaticLib partial = new StaticLib ();
    partial.setPath ("C");
    partial.setPattern ("*.lib");
    assertEquals (bean2.getImplibs (), new StaticLib[] { all, nothing, partial });
  }

  public void testDynamicLibDefaults_noImplibs () {
    final Properties properties = new Properties ();
    properties.setProperty ("dynamic.implib", "");
    properties.setProperty ("static.path", "lib");
    properties.setProperty ("static.pattern", "*.lib");
    final Defaults defaults = new Defaults ("test", properties);
    final DynamicLib bean1 = new DynamicLib ();
    bean1.setArch ("i386");
    bean1.setHeaders (new HeaderFile[0]);
    final DynamicLib bean2 = new DynamicLib ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getImplibs (), null);
    assertEquals (bean2.getImplibs (), null);
  }

  public void testDynamicLibDefaults_save () {
    final Properties properties = new Properties ();
    final Defaults.DynamicLibDefaults defaults = new Defaults.DynamicLibDefaults ();
    defaults.save (properties, "x");
    assertEquals (properties.toString (), "{}");
    properties.clear ();
    defaults.setArch ("i386");
    final Defaults.HeaderFileDefaults headerA = new Defaults.HeaderFileDefaults ();
    headerA.setPattern ("*.h");
    final Defaults.HeaderFileDefaults headerB = new Defaults.HeaderFileDefaults ();
    headerB.setPattern ("*.hh");
    defaults.setHeaders (new Defaults.HeaderFileDefaults[] { headerA, headerB });
    final Defaults.StaticLibDefaults implib = new Defaults.StaticLibDefaults ();
    implib.setPattern ("*.lib");
    defaults.setImplibs (new Defaults.StaticLibDefaults[] { implib });
    defaults.save (properties, "x");
    assertEquals (properties.toString (),
        "{x.implib=i3, x.header=h1;h2, i3.pattern=*.lib, h2.pattern=*.hh, h1.pattern=*.h, x.arch=i386}");
  }

  public void testDefaultDynamicLibs () {
    final Properties properties = new Properties ();
    properties.setProperty ("dynamic", "a;b");
    properties.setProperty ("a.path", "A");
    properties.setProperty ("b.path", "B");
    final Defaults defaults = new Defaults ("test", properties);
    final DynamicLib a = new DynamicLib ();
    a.setPath ("A");
    final DynamicLib b = new DynamicLib ();
    b.setPath ("B");
    assertEquals (defaults.createDefaultDynamicLibs (), new DynamicLib[] { a, b });
  }

  public void testExecutableDefaults_headers () {
    final Properties properties = new Properties ();
    properties.setProperty ("exec.header", "all;nothing;partial");
    properties.setProperty ("all.path", "A");
    properties.setProperty ("all.pattern", "*.a");
    properties.setProperty ("partial.path", "C");
    properties.setProperty ("header.path", "include");
    properties.setProperty ("header.pattern", "*.h");
    final Defaults defaults = new Defaults ("test", properties);
    final Executable bean1 = new Executable ();
    bean1.setArch ("i386");
    bean1.setHeaders (new HeaderFile[0]);
    final Executable bean2 = new Executable ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getHeaders (), null);
    final HeaderFile all = new HeaderFile ();
    all.setPath ("A");
    all.setPattern ("*.a");
    final HeaderFile nothing = new HeaderFile ();
    nothing.setPath ("include");
    nothing.setPattern ("*.h");
    final HeaderFile partial = new HeaderFile ();
    partial.setPath ("C");
    partial.setPattern ("*.h");
    assertEquals (bean2.getHeaders (), new HeaderFile[] { all, nothing, partial });
  }

  public void testExecutableDefaults_noHeaders () {
    final Properties properties = new Properties ();
    properties.setProperty ("exec.header", "");
    final Defaults defaults = new Defaults ("test", properties);
    final Executable bean1 = new Executable ();
    bean1.setArch ("i386");
    bean1.setHeaders (new HeaderFile[0]);
    final Executable bean2 = new Executable ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getHeaders (), null);
    assertEquals (bean2.getHeaders (), null);
  }

  public void testExecutableDefaults_libraries () {
    final Properties properties = new Properties ();
    properties.setProperty ("exec.library", "dynamic-lib;static-lib;misc-lib");
    properties.setProperty ("dynamic-lib.type", "dynamic");
    properties.setProperty ("dynamic-lib.path", "A");
    properties.setProperty ("dynamic-lib.pattern", "*.a");
    properties.setProperty ("static-lib.type", "static");
    properties.setProperty ("misc-lib.path", "C");
    properties.setProperty ("misc-lib.pattern", "*.c");
    properties.setProperty ("static.path", "lib");
    properties.setProperty ("static.pattern", "*.lib");
    final Defaults defaults = new Defaults ("test", properties);
    final Executable bean1 = new Executable ();
    bean1.setArch ("i386");
    bean1.setLibraries (new ArchSource[0]);
    final Executable bean2 = new Executable ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getLibraries (), null);
    final DynamicLib dynamicLib = new DynamicLib ();
    dynamicLib.setPath ("A");
    dynamicLib.setPattern ("*.a");
    final StaticLib staticLib = new StaticLib ();
    staticLib.setPath ("lib");
    staticLib.setPattern ("*.lib");
    final ArchSource miscLib = new ArchSource ();
    miscLib.setPath ("C");
    miscLib.setPattern ("*.c");
    assertEquals (bean2.getLibraries (), new ArchSource[] { dynamicLib, staticLib, miscLib });
  }

  public void testExecutableDefaults_noLibraries () {
    final Properties properties = new Properties ();
    properties.setProperty ("exec.library", "");
    final Defaults defaults = new Defaults ("test", properties);
    final Executable bean1 = new Executable ();
    bean1.setArch ("i386");
    bean1.setHeaders (new HeaderFile[0]);
    final Executable bean2 = new Executable ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getLibraries (), null);
    assertEquals (bean2.getLibraries (), null);
  }

  public void testExecutableDefaults_save () {
    final Properties properties = new Properties ();
    final Defaults.ExecutableDefaults defaults = new Defaults.ExecutableDefaults ();
    defaults.save (properties, "x");
    assertEquals (properties.toString (), "{}");
    properties.clear ();
    defaults.setArch ("i386");
    final Defaults.HeaderFileDefaults headerA = new Defaults.HeaderFileDefaults ();
    headerA.setPattern ("*.h");
    final Defaults.HeaderFileDefaults headerB = new Defaults.HeaderFileDefaults ();
    headerB.setPattern ("*.hh");
    defaults.setHeaders (new Defaults.HeaderFileDefaults[] { headerA, headerB });
    final Defaults.StaticLibDefaults implib = new Defaults.StaticLibDefaults ();
    implib.setPattern ("*.lib");
    final Defaults.DynamicLibDefaults dynlib = new Defaults.DynamicLibDefaults ();
    implib.setPattern ("*.dll");
    final Defaults.ArchSourceDefaults misclib = new Defaults.ArchSourceDefaults ();
    implib.setPattern ("*.foo");
    defaults.setLibraries (new Defaults.ArchSourceDefaults[] { implib, dynlib, misclib });
    defaults.save (properties, "x");
    assertEquals (
        properties.toString (),
        "{l3.pattern=*.foo, x.library=l3;l4;l5, x.header=h1;h2, l4.type=dynamic, l3.type=static, h2.pattern=*.hh, h1.pattern=*.h, x.arch=i386}");
  }

  public void testDefaultExecutables () {
    final Properties properties = new Properties ();
    properties.setProperty ("exec", "a;b");
    properties.setProperty ("a.path", "A");
    properties.setProperty ("b.path", "B");
    final Defaults defaults = new Defaults ("test", properties);
    final Executable a = new Executable ();
    a.setPath ("A");
    final Executable b = new Executable ();
    b.setPath ("B");
    assertEquals (defaults.createDefaultExecutables (), new Executable[] { a, b });
  }

  public void testStaticLibDefaults_headers () {
    final Properties properties = new Properties ();
    properties.setProperty ("static.header", "all;nothing;partial");
    properties.setProperty ("all.path", "A");
    properties.setProperty ("all.pattern", "*.a");
    properties.setProperty ("partial.path", "C");
    properties.setProperty ("header.path", "include");
    properties.setProperty ("header.pattern", "*.h");
    final Defaults defaults = new Defaults ("test", properties);
    final StaticLib bean1 = new StaticLib ();
    bean1.setArch ("i386");
    final HeaderFile header = new HeaderFile ();
    header.setPattern ("*.inc");
    bean1.setHeaders (new HeaderFile[] { header });
    final StaticLib bean2 = new StaticLib ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getHeaders (), new HeaderFile[] { header });
    assertEquals (header.getPath (), "include");
    final HeaderFile all = new HeaderFile ();
    all.setPath ("A");
    all.setPattern ("*.a");
    final HeaderFile nothing = new HeaderFile ();
    nothing.setPath ("include");
    nothing.setPattern ("*.h");
    final HeaderFile partial = new HeaderFile ();
    partial.setPath ("C");
    partial.setPattern ("*.h");
    assertEquals (bean2.getHeaders (), new HeaderFile[] { all, nothing, partial });
  }

  public void testStaticLibDefaults_noHeaders () {
    final Properties properties = new Properties ();
    properties.setProperty ("static.header", "");
    final Defaults defaults = new Defaults ("test", properties);
    final DynamicLib bean1 = new DynamicLib ();
    bean1.setArch ("i386");
    bean1.setHeaders (new HeaderFile[0]);
    final DynamicLib bean2 = new DynamicLib ();
    bean2.setArch ("x64");
    defaults.applyTo (bean1);
    defaults.applyTo (bean2);
    assertEquals (bean1.getHeaders (), null);
    assertEquals (bean2.getHeaders (), null);
  }

  public void testStaticLibDefaults_save () {
    final Properties properties = new Properties ();
    final Defaults.StaticLibDefaults defaults = new Defaults.StaticLibDefaults ();
    defaults.save (properties, "x");
    assertEquals (properties.toString (), "{}");
    properties.clear ();
    defaults.setArch ("i386");
    final Defaults.HeaderFileDefaults headerA = new Defaults.HeaderFileDefaults ();
    headerA.setPattern ("*.h");
    final Defaults.HeaderFileDefaults headerB = new Defaults.HeaderFileDefaults ();
    headerB.setPattern ("*.hh");
    defaults.setHeaders (new Defaults.HeaderFileDefaults[] { headerA, headerB });
    defaults.save (properties, "x");
    assertEquals (properties.toString (),
        "{x.header=h1;h2, h2.pattern=*.hh, h1.pattern=*.h, x.arch=i386}");
  }

  public void testDefaultStaticLibs () {
    final Properties properties = new Properties ();
    properties.setProperty ("static", "a;b");
    properties.setProperty ("a.path", "A");
    properties.setProperty ("b.path", "B");
    final Defaults defaults = new Defaults ("test", properties);
    final StaticLib a = new StaticLib ();
    a.setPath ("A");
    final StaticLib b = new StaticLib ();
    b.setPath ("B");
    assertEquals (defaults.createDefaultStaticLibs (), new StaticLib[] { a, b });
  }

  public void testDefaultHeaderFiles () {
    final Properties properties = new Properties ();
    properties.setProperty ("header", "a;b");
    properties.setProperty ("a.path", "A");
    properties.setProperty ("b.path", "B");
    final Defaults defaults = new Defaults ("test", properties);
    final HeaderFile a = new HeaderFile ();
    a.setPath ("A");
    final HeaderFile b = new HeaderFile ();
    b.setPath ("B");
    assertEquals (defaults.createDefaultHeaderFiles (), new HeaderFile[] { a, b });
  }

}
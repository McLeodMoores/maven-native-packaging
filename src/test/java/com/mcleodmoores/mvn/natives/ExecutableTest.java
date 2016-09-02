/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.mcleodmoores.mvn.natives.ArchSource;
import com.mcleodmoores.mvn.natives.DynamicLib;
import com.mcleodmoores.mvn.natives.Executable;
import com.mcleodmoores.mvn.natives.HeaderFile;
import com.mcleodmoores.mvn.natives.StaticLib;
import com.mcleodmoores.mvn.natives.defaults.Defaults;

/**
 * Tests the {@link Executable} class.
 */
@Test
public class ExecutableTest {

  public void testSetAndGet () {
    final Executable file = new Executable ();
    file.setArch ("i386");
    file.setPath ("foo");
    file.setPattern ("*.exe");
    file.setHeaders (new HeaderFile[] { new HeaderFile () });
    file.setLibraries (new StaticLib[] { new StaticLib () });
    assertEquals (file.getArch (), "i386");
    assertEquals (file.getPath (), "foo");
    assertEquals (file.getPattern (), "*.exe");
    assertEquals (file.getHeaders (), new HeaderFile[] { new HeaderFile () });
    assertEquals (file.getLibraries (), new StaticLib[] { new StaticLib () });
  }

  public void testToString () {
    final Executable file = new Executable ();
    assertEquals (file.toString (), "Executable");
    file.setArch ("i386");
    file.setPath ("foo");
    file.setPattern ("*.exe");
    file.setHeaders (new HeaderFile[] { new HeaderFile () });
    file.setLibraries (new StaticLib[] { new StaticLib () });
    assertEquals (file.toString (),
        "Executable, path:foo, pattern:*.exe, arch:i386, headers:[HeaderFile], libraries:[StaticLib]");
  }

  public void testEquality () {
    final Executable a = new Executable ();
    final Executable b = new Executable ();
    assertTrue (a.equals (a));
    assertFalse (a.equals (null));
    assertFalse (a.equals (new DynamicLib ()));
    assertTrue (a.equals (b));
    assertTrue (b.equals (a));
  }

  public void testInequality_arch () {
    final Executable a = new Executable ();
    final Executable b = new Executable ();
    b.setArch ("i386");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_path () {
    final Executable a = new Executable ();
    final Executable b = new Executable ();
    b.setPath ("foo");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_pattern () {
    final Executable a = new Executable ();
    final Executable b = new Executable ();
    b.setPattern ("*.exe");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_headers () {
    final Executable a = new Executable ();
    final Executable b = new Executable ();
    b.setHeaders (new HeaderFile[] { new HeaderFile () });
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_libraries () {
    final Executable a = new Executable ();
    final Executable b = new Executable ();
    b.setLibraries (new ArchSource[] { new DynamicLib () });
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testHashing () {
    final Executable a = new Executable ();
    final Executable b = new Executable ();
    assertEquals (a.hashCode (), b.hashCode ());
    b.setHeaders (new HeaderFile[] { new HeaderFile () });
    assertNotEquals (a.hashCode (), b.hashCode ());
  }

  public void testWindowsDefaults () {
    final Executable file = new Executable ();
    assertNull (file.getArch ());
    assertNull (file.getPath ());
    assertNull (file.getPattern ());
    assertNull (file.getHeaders ());
    assertNull (file.getLibraries ());
    final Defaults defaults = Defaults.get ("windows");
    defaults.applyTo (file);
    assertNull (file.getArch ());
    assertEquals (file.getPath (), "target\\bin");
    assertEquals (file.getPattern (), "*.exe");
    assertNull (file.getHeaders ());
    final StaticLib implib = new StaticLib ();
    implib.setPath ("target\\bin");
    implib.setPattern ("*.lib");
    final DynamicLib dll = new DynamicLib ();
    dll.setPath ("target\\bin");
    dll.setPattern ("*.dll");
    assertEquals (file.getLibraries (), new ArchSource[] { implib, dll });
  }

}
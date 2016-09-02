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

import com.mcleodmoores.mvn.natives.DynamicLib;
import com.mcleodmoores.mvn.natives.HeaderFile;
import com.mcleodmoores.mvn.natives.StaticLib;
import com.mcleodmoores.mvn.natives.defaults.Defaults;

/**
 * Tests the {@link DynamicLib} class.
 */
@Test
public class DynamicLibTest {

  public void testSetAndGet () {
    final DynamicLib file = new DynamicLib ();
    file.setArch ("i386");
    file.setPath ("foo");
    file.setPattern ("*.dll");
    file.setHeaders (new HeaderFile[] { new HeaderFile () });
    file.setImplibs (new StaticLib[] { new StaticLib () });
    assertEquals (file.getArch (), "i386");
    assertEquals (file.getPath (), "foo");
    assertEquals (file.getPattern (), "*.dll");
    assertEquals (file.getHeaders (), new HeaderFile[] { new HeaderFile () });
    assertEquals (file.getImplibs (), new StaticLib[] { new StaticLib () });
  }

  public void testToString () {
    final DynamicLib file = new DynamicLib ();
    assertEquals (file.toString (), "DynamicLib");
    file.setArch ("i386");
    file.setPath ("foo");
    file.setPattern ("*.dll");
    file.setHeaders (new HeaderFile[] { new HeaderFile () });
    file.setImplibs (new StaticLib[] { new StaticLib () });
    assertEquals (file.toString (),
        "DynamicLib, path:foo, pattern:*.dll, arch:i386, headers:[HeaderFile], implibs:[StaticLib]");
  }

  public void testEquality () {
    final DynamicLib a = new DynamicLib ();
    final DynamicLib b = new DynamicLib ();
    assertTrue (a.equals (a));
    assertFalse (a.equals (new StaticLib ()));
    assertFalse (a.equals (null));
    assertTrue (a.equals (b));
    assertTrue (b.equals (a));
  }

  public void testInequality_arch () {
    final DynamicLib a = new DynamicLib ();
    final DynamicLib b = new DynamicLib ();
    b.setArch ("i386");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_path () {
    final DynamicLib a = new DynamicLib ();
    final DynamicLib b = new DynamicLib ();
    b.setPath ("foo");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_pattern () {
    final DynamicLib a = new DynamicLib ();
    final DynamicLib b = new DynamicLib ();
    b.setPattern ("*.dll");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_headers () {
    final DynamicLib a = new DynamicLib ();
    final DynamicLib b = new DynamicLib ();
    b.setHeaders (new HeaderFile[] { new HeaderFile () });
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_implibs () {
    final DynamicLib a = new DynamicLib ();
    final DynamicLib b = new DynamicLib ();
    b.setImplibs (new StaticLib[] { new StaticLib () });
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testHashing () {
    final DynamicLib a = new DynamicLib ();
    final DynamicLib b = new DynamicLib ();
    assertEquals (a.hashCode (), b.hashCode ());
    b.setHeaders (new HeaderFile[] { new HeaderFile () });
    assertNotEquals (a.hashCode (), b.hashCode ());
  }

  public void testWindowsDefaults () {
    final DynamicLib file = new DynamicLib ();
    assertNull (file.getArch ());
    assertNull (file.getPath ());
    assertNull (file.getPattern ());
    assertNull (file.getHeaders ());
    assertNull (file.getImplibs ());
    final Defaults defaults = Defaults.get ("windows");
    defaults.applyTo (file);
    assertNull (file.getArch ());
    assertEquals (file.getPath (), "target\\dll");
    assertEquals (file.getPattern (), "*.dll");
    final HeaderFile include = new HeaderFile ();
    include.setPath ("target\\include");
    include.setPattern ("*.h");
    assertEquals (file.getHeaders (), new HeaderFile[] { include });
    final StaticLib implib = new StaticLib ();
    implib.setPath ("target\\dll");
    implib.setPattern ("*.lib");
    assertEquals (file.getImplibs (), new StaticLib[] { implib });
  }

}
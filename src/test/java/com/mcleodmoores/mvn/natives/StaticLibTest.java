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
 * Tests the {@link StaticLib} class.
 */
@Test
public class StaticLibTest {

  public void testGetAndSet () {
    final StaticLib file = new StaticLib ();
    file.setArch ("i386");
    file.setPath ("foo");
    file.setPattern ("*.lib");
    file.setHeaders (new HeaderFile[] { new HeaderFile () });
    assertEquals (file.getArch (), "i386");
    assertEquals (file.getPath (), "foo");
    assertEquals (file.getPattern (), "*.lib");
    assertEquals (file.getHeaders (), new HeaderFile[] { new HeaderFile () });
  }

  public void testToString () {
    final StaticLib file = new StaticLib ();
    assertEquals (file.toString (), "StaticLib");
    file.setArch ("i386");
    file.setPath ("foo");
    file.setPattern ("*.lib");
    file.setHeaders (new HeaderFile[] { new HeaderFile () });
    assertEquals (file.toString (),
        "StaticLib, path:foo, pattern:*.lib, arch:i386, headers:[HeaderFile]");
  }

  public void testEquality () {
    final StaticLib a = new StaticLib ();
    final StaticLib b = new StaticLib ();
    assertTrue (a.equals (a));
    assertFalse (a.equals (null));
    assertFalse (a.equals (new DynamicLib ()));
    assertTrue (a.equals (b));
    assertTrue (b.equals (a));
  }

  public void testInequality_arch () {
    final StaticLib a = new StaticLib ();
    final StaticLib b = new StaticLib ();
    b.setArch ("i386");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_path () {
    final StaticLib a = new StaticLib ();
    final StaticLib b = new StaticLib ();
    b.setPath ("foo");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_pattern () {
    final StaticLib a = new StaticLib ();
    final StaticLib b = new StaticLib ();
    b.setPattern ("*.exe");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_headers () {
    final StaticLib a = new StaticLib ();
    final StaticLib b = new StaticLib ();
    b.setHeaders (new HeaderFile[] { new HeaderFile () });
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testHashing () {
    final StaticLib a = new StaticLib ();
    final StaticLib b = new StaticLib ();
    assertEquals (a.hashCode (), b.hashCode ());
    b.setHeaders (new HeaderFile[] { new HeaderFile () });
    assertNotEquals (a.hashCode (), b.hashCode ());
  }

  public void testWindowsDefaults () {
    final StaticLib file = new StaticLib ();
    assertNull (file.getArch ());
    assertNull (file.getPath ());
    assertNull (file.getPattern ());
    assertNull (file.getHeaders ());
    final Defaults defaults = Defaults.get ("windows");
    defaults.applyTo (file);
    assertNull (file.getArch ());
    assertEquals (file.getPath (), "target\\lib");
    assertEquals (file.getPattern (), "*.lib");
    final HeaderFile include = new HeaderFile ();
    include.setPath ("target\\include");
    include.setPattern ("*.h");
    assertEquals (file.getHeaders (), new HeaderFile[] { include });
  }

}
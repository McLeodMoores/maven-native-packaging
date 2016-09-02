/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.mcleodmoores.mvn.natives.HeaderFile;
import com.mcleodmoores.mvn.natives.Source;
import com.mcleodmoores.mvn.natives.defaults.Defaults;

/**
 * Tests the {@link HeaderFile} class.
 */
@Test
public class HeaderFileTest {

  public void testExplicit () {
    final HeaderFile file = new HeaderFile ();
    file.setPath ("foo");
    file.setDest ("sys");
    file.setPattern ("*.inc");
    assertEquals (file.getPath (), "foo");
    assertEquals (file.getDest (), "sys");
    assertEquals (file.getPattern (), "*.inc");
  }

  public void testEquality () {
    final HeaderFile a = new HeaderFile ();
    final HeaderFile b = new HeaderFile ();
    assertTrue (a.equals (a));
    assertFalse (a.equals (null));
    assertFalse (a.equals (new Source ()));
    assertTrue (a.equals (b));
    assertTrue (b.equals (a));
  }

  public void testInequality_path () {
    final HeaderFile a = new HeaderFile ();
    final HeaderFile b = new HeaderFile ();
    b.setPath ("foo");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_dest () {
    final HeaderFile a = new HeaderFile ();
    final HeaderFile b = new HeaderFile ();
    b.setDest ("foo");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testInequality_pattern () {
    final HeaderFile a = new HeaderFile ();
    final HeaderFile b = new HeaderFile ();
    b.setPattern ("*.inc");
    assertFalse (a.equals (b));
    assertFalse (b.equals (a));
  }

  public void testWindowsDefaults () {
    final HeaderFile file = new HeaderFile ();
    assertNull (file.getPath ());
    assertNull (file.getPattern ());
    final Defaults defaults = Defaults.get ("windows");
    defaults.applyTo (file);
    assertEquals (file.getPath (), "target\\include");
    assertEquals (file.getPattern (), "*.h");
  }

}
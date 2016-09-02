/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import static org.testng.Assert.assertEquals;

import java.util.LinkedList;
import java.util.Queue;

import org.testng.annotations.Test;

import com.mcleodmoores.mvn.natives.ArchSource;
import com.mcleodmoores.mvn.natives.DynamicLib;
import com.mcleodmoores.mvn.natives.Executable;
import com.mcleodmoores.mvn.natives.HeaderFile;
import com.mcleodmoores.mvn.natives.Source;
import com.mcleodmoores.mvn.natives.SourceVisitor;
import com.mcleodmoores.mvn.natives.StaticLib;

/**
 * Tests the {@link SourceVisitor} class.
 */
@Test
public class SourceVisitorTest {

  private class VisitorImpl extends SourceVisitor {

    private final Queue<String> _events = new LinkedList<String> ();

    @Override
    protected void visitSource (final Source instance) {
      _events.add ("Source:" + instance);
    }

    @Override
    protected void visitArchSource (final ArchSource instance) {
      _events.add ("ArchSource:" + instance);
      super.visitArchSource (instance);
    }

    @Override
    protected void visitDynamicLib (final DynamicLib instance) {
      _events.add ("DynamicLib:" + instance);
      super.visitDynamicLib (instance);
    }

    @Override
    protected void visitExecutable (final Executable instance) {
      _events.add ("Executable:" + instance);
      super.visitExecutable (instance);
    }

    @Override
    protected void visitStaticLib (final StaticLib instance) {
      _events.add ("StaticLib:" + instance);
      super.visitStaticLib (instance);
    }

    @Override
    protected void visitHeaderFile (final HeaderFile instance) {
      _events.add ("HeaderFile:" + instance);
      super.visitHeaderFile (instance);
    }

  }

  public void testSource () {
    final Source instance = new Source ();
    final VisitorImpl visitor = new VisitorImpl ();
    visitor.applyTo (instance);
    assertEquals (visitor._events.poll (), "Source:" + instance);
  }

  public void testArchSource () {
    final Source instance = new ArchSource ();
    final VisitorImpl visitor = new VisitorImpl ();
    visitor.applyTo (instance);
    assertEquals (visitor._events.poll (), "ArchSource:" + instance);
    assertEquals (visitor._events.poll (), "Source:" + instance);
  }

  public void testDynamicLib () {
    final Source instance = new DynamicLib ();
    final VisitorImpl visitor = new VisitorImpl ();
    visitor.applyTo (instance);
    assertEquals (visitor._events.poll (), "DynamicLib:" + instance);
    assertEquals (visitor._events.poll (), "ArchSource:" + instance);
    assertEquals (visitor._events.poll (), "Source:" + instance);
  }

  public void testExecutable () {
    final Source instance = new Executable ();
    final VisitorImpl visitor = new VisitorImpl ();
    visitor.applyTo (instance);
    assertEquals (visitor._events.poll (), "Executable:" + instance);
    assertEquals (visitor._events.poll (), "ArchSource:" + instance);
    assertEquals (visitor._events.poll (), "Source:" + instance);
  }

  public void testStaticLib () {
    final Source instance = new StaticLib ();
    final VisitorImpl visitor = new VisitorImpl ();
    visitor.applyTo (instance);
    assertEquals (visitor._events.poll (), "StaticLib:" + instance);
    assertEquals (visitor._events.poll (), "ArchSource:" + instance);
    assertEquals (visitor._events.poll (), "Source:" + instance);
  }

  public void testHeaderFile () {
    final Source instance = new HeaderFile ();
    final VisitorImpl visitor = new VisitorImpl ();
    visitor.applyTo (instance);
    assertEquals (visitor._events.poll (), "HeaderFile:" + instance);
    assertEquals (visitor._events.poll (), "Source:" + instance);
  }

  @Test (expectedExceptions = UnsupportedOperationException.class)
  public void testUnimplemented () {
    (new SourceVisitor ()).applyTo (new Executable ());
  }

}
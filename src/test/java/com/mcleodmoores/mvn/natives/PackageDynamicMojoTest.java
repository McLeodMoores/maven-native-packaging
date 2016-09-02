/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.mcleodmoores.misc.IOCallback;
import com.mcleodmoores.misc.OutputStreamOpener;
import com.mcleodmoores.mvn.natives.DynamicLib;
import com.mcleodmoores.mvn.natives.HeaderFile;
import com.mcleodmoores.mvn.natives.PackageDynamicMojo;
import com.mcleodmoores.mvn.natives.PackageMojo;
import com.mcleodmoores.mvn.natives.Source;
import com.mcleodmoores.mvn.natives.StaticLib;

/**
 * Tests the {@link PackageDynamicMojo} class.
 */
@Test
public class PackageDynamicMojoTest {

  public void testGetAndSet () {
    final PackageDynamicMojo instance = new PackageDynamicMojo ();
    instance.setSkip (true);
    instance.setDefaults ("package-mojo-test");
    instance.setSources (new Source[] { new Source () });
    instance.setDynamicLibs (new DynamicLib[] { new DynamicLib () });
    assertTrue (instance.isSkip ());
    assertEquals (instance.getDefaults (), "package-mojo-test");
    assertEquals (instance.getSources (), new Source[] { new Source () });
    assertEquals (instance.getDynamicLibs (), new DynamicLib[] { new DynamicLib () });
  }

  public void testDefaultsApplied_nothing () {
    final PackageDynamicMojo instance = new PackageDynamicMojo ();
    instance.setDefaults ("package-mojo-test");
    instance.applyDefaults ();
    assertNull (instance.getSources ());
    final HeaderFile headerFile = new HeaderFile ();
    headerFile.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "include");
    headerFile.setPattern ("*.h");
    final DynamicLib dynamic32 = new DynamicLib ();
    dynamic32.setArch ("i386");
    dynamic32.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "dll32");
    dynamic32.setPattern ("*.dll");
    dynamic32.setHeaders (new HeaderFile[] { headerFile });
    final StaticLib implib32 = new StaticLib ();
    implib32.setPattern ("*.lib");
    dynamic32.setImplibs (new StaticLib[] { implib32 });
    final DynamicLib dynamic64 = new DynamicLib ();
    dynamic64.setArch ("x64");
    dynamic64.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "dll64");
    dynamic64.setPattern ("*.dll");
    dynamic64.setHeaders (new HeaderFile[] { headerFile });
    final StaticLib implib64 = new StaticLib ();
    implib64.setPattern ("*.lib");
    dynamic64.setImplibs (new StaticLib[] { implib64 });
    assertEquals (instance.getDynamicLibs (), new DynamicLib[] { dynamic32, dynamic64 });
  }

  public void testDefaultsApplied_all () {
    final PackageDynamicMojo instance = new PackageDynamicMojo ();
    instance.setDefaults ("package-mojo-test");
    instance.setSources (new Source[] { new Source (), new HeaderFile () });
    instance.setDynamicLibs (new DynamicLib[] { new DynamicLib () });
    instance.applyDefaults ();
    final HeaderFile headerFile = new HeaderFile ();
    headerFile.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "include");
    headerFile.setPattern ("*.h");
    final DynamicLib dynamicLib = new DynamicLib ();
    dynamicLib.setPattern ("*.dll");
    dynamicLib.setHeaders (new HeaderFile[] { headerFile });
    assertEquals (instance.getSources (), new Source[] { new Source (), headerFile });
    assertEquals (instance.getDynamicLibs (), new DynamicLib[] { dynamicLib });
  }

  public void testNoDefaults () {
    final PackageDynamicMojo instance = new PackageDynamicMojo ();
    instance.applyDefaults ();
    assertFalse (instance.isSkip ());
    assertNull (instance.getSources ());
    assertNull (instance.getDynamicLibs ());
  }

  public void testPackageEquivalent () {
    final PackageDynamicMojo instance = new PackageDynamicMojo ();
    instance.setSkip (true);
    instance.setDefaults ("windows");
    instance.setSources (new Source[] { new Source () });
    instance.setDynamicLibs (new DynamicLib[] { new DynamicLib () });
    final PackageMojo delegate = instance.delegate ();
    assertTrue (delegate.isSkip ());
    assertNull (delegate.getDefaults ());
    assertEquals (delegate.getSources (), new Source[] { new Source () });
    assertNull (delegate.getHeaderFiles ());
    assertNull (delegate.getStaticLibs ());
    assertEquals (delegate.getDynamicLibs (), new DynamicLib[] { new DynamicLib () });
    assertNull (delegate.getExecutables ());
  }

  private PackageDynamicMojo executeInstance (final File output) {
    final PackageDynamicMojo instance = new PackageDynamicMojo ();
    instance.setLog (Mockito.mock (Log.class));
    final MavenProject project = new MavenProject ();
    project.setArtifactId (output.getName ().substring (0, output.getName ().length () - 4));
    project.getBuild ().setDirectory (output.getParent ());
    project.setArtifact (Mockito.mock (Artifact.class));
    instance.setPluginContext (Collections.singletonMap ("project", project));
    instance.setDefaults ("package-mojo-test");
    return instance;
  }

  public void testSkipExecute () throws Exception {
    final File file = File.createTempFile ("test", ".zip");
    try {
      final PackageDynamicMojo instance = executeInstance (file);
      instance.setSkip (true);
      final PackageMojo delegate = instance.delegate ();
      final OutputStreamOpener outputStreams = Mockito.mock (OutputStreamOpener.class);
      delegate.setOutputStreams (outputStreams);
      delegate.execute ();
      Mockito.verifyZeroInteractions (outputStreams);
    } finally {
      file.delete ();
    }
  }

  public void testExecute () throws Exception {
    final File file = File.createTempFile ("test", ".zip");
    try {
      final PackageDynamicMojo instance = executeInstance (file);
      instance.execute ();
      assertNotNull ((new IOCallback<InputStream, Boolean> (new FileInputStream (file)) {

        @Override
        protected Boolean apply (final InputStream input) throws IOException {
          final ZipInputStream zip = new ZipInputStream (new BufferedInputStream (input));
          final Set<String> files = new HashSet<String> ();
          ZipEntry entry;
          while ((entry = zip.getNextEntry ()) != null) {
            files.add (entry.getName ());
          }
          assertEquals (
              files,
              ImmutableSet.of ("lib-i386" + File.separator + "3.lib", "bin-i386" + File.separator
                  + "3.dll", "lib-x64" + File.separator + "4.lib", "bin-x64" + File.separator
                  + "4.dll", "include" + File.separator + "5.h"));
          return Boolean.TRUE;
        }

      }).callWithAssertion ());
    } finally {
      file.delete ();
    }
  }

}
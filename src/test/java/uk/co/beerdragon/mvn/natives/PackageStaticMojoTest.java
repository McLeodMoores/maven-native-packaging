/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package uk.co.beerdragon.mvn.natives;

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

import uk.co.beerdragon.misc.IOCallback;
import uk.co.beerdragon.misc.OutputStreamOpener;

import com.google.common.collect.ImmutableSet;

/**
 * Tests the {@link PackageStaticMojo} class.
 */
@Test
public class PackageStaticMojoTest {

  public void testGetAndSet () {
    final PackageStaticMojo instance = new PackageStaticMojo ();
    instance.setSkip (true);
    instance.setDefaults ("package-mojo-test");
    instance.setSources (new Source[] { new Source () });
    instance.setStaticLibs (new StaticLib[] { new StaticLib () });
    assertTrue (instance.isSkip ());
    assertEquals (instance.getDefaults (), "package-mojo-test");
    assertEquals (instance.getSources (), new Source[] { new Source () });
    assertEquals (instance.getStaticLibs (), new StaticLib[] { new StaticLib () });
  }

  public void testDefaultsApplied_nothing () {
    final PackageStaticMojo instance = new PackageStaticMojo ();
    instance.setDefaults ("package-mojo-test");
    instance.applyDefaults ();
    assertNull (instance.getSources ());
    final HeaderFile headerFile = new HeaderFile ();
    headerFile.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "include");
    headerFile.setPattern ("*.h");
    final StaticLib static32 = new StaticLib ();
    static32.setArch ("i386");
    static32.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "lib32");
    static32.setPattern ("*.lib");
    final HeaderFile header32 = new HeaderFile ();
    header32.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "lib32");
    header32.setPattern ("*.h");
    static32.setHeaders (new HeaderFile[] { headerFile, header32 });
    final StaticLib static64 = new StaticLib ();
    static64.setArch ("x64");
    static64.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "lib64");
    static64.setPattern ("*.lib");
    final HeaderFile header64 = new HeaderFile ();
    header64.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "lib64");
    header64.setPattern ("*.h");
    static64.setHeaders (new HeaderFile[] { headerFile, header64 });
    assertEquals (instance.getStaticLibs (), new StaticLib[] { static32, static64 });
  }

  public void testDefaultsApplied_all () {
    final PackageStaticMojo instance = new PackageStaticMojo ();
    instance.setDefaults ("package-mojo-test");
    instance.setSources (new Source[] { new Source (), new HeaderFile () });
    instance.setStaticLibs (new StaticLib[] { new StaticLib () });
    instance.applyDefaults ();
    final HeaderFile headerFile = new HeaderFile ();
    headerFile.setPath ("src" + File.separator + "test" + File.separator + "files" + File.separator
        + "include");
    headerFile.setPattern ("*.h");
    final StaticLib staticLib = new StaticLib ();
    staticLib.setPattern ("*.lib");
    staticLib.setHeaders (new HeaderFile[] { headerFile });
    assertEquals (instance.getSources (), new Source[] { new Source (), headerFile });
    assertEquals (instance.getStaticLibs (), new StaticLib[] { staticLib });
  }

  public void testNoDefaults () {
    final PackageStaticMojo instance = new PackageStaticMojo ();
    instance.applyDefaults ();
    assertFalse (instance.isSkip ());
    assertNull (instance.getSources ());
    assertNull (instance.getStaticLibs ());
  }

  public void testPackageEquivalent () {
    final PackageStaticMojo instance = new PackageStaticMojo ();
    instance.setSkip (true);
    instance.setDefaults ("windows");
    instance.setSources (new Source[] { new Source () });
    instance.setStaticLibs (new StaticLib[] { new StaticLib () });
    final PackageMojo delegate = instance.delegate ();
    assertTrue (delegate.isSkip ());
    assertNull (delegate.getDefaults ());
    assertEquals (delegate.getSources (), new Source[] { new Source () });
    assertNull (delegate.getHeaderFiles ());
    assertEquals (delegate.getStaticLibs (), new StaticLib[] { new StaticLib () });
    assertNull (delegate.getDynamicLibs ());
    assertNull (delegate.getExecutables ());
  }

  private PackageStaticMojo executeInstance (final File output) {
    final PackageStaticMojo instance = new PackageStaticMojo ();
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
      final PackageStaticMojo instance = executeInstance (file);
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
      final PackageStaticMojo instance = executeInstance (file);
      instance.applyDefaults ();
      instance.getStaticLibs ()[0].getHeaders ()[1].setDest ("i386");
      instance.getStaticLibs ()[1].getHeaders ()[1].setDest ("x64");
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
          assertEquals (files, ImmutableSet.of ("include" + File.separator + "5.h", "lib-i386"
              + File.separator + "6.lib", "include" + File.separator + "i386" + File.separator
              + "6.h", "lib-x64" + File.separator + "7.lib", "include" + File.separator + "x64"
              + File.separator + "7.h"));
          return Boolean.TRUE;
        }

      }).callWithAssertion ());
    } finally {
      file.delete ();
    }
  }
}
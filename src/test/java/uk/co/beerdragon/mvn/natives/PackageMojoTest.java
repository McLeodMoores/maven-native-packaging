/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package uk.co.beerdragon.mvn.natives;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import uk.co.beerdragon.misc.InputStreamOpener;
import uk.co.beerdragon.misc.OutputStreamOpener;

/**
 * Tests the {@link PackageMojo} class.
 */
@Test
public class PackageMojoTest {

  public void testGetAndSet () {
    final PackageMojo instance = new PackageMojo ();
    instance.setSkip (true);
    instance.setDefaults ("windows");
    instance.setSources (new Source[] { new Source () });
    instance.setHeaderFiles (new HeaderFile[] { new HeaderFile () });
    instance.setStaticLibs (new StaticLib[] { new StaticLib () });
    instance.setDynamicLibs (new DynamicLib[] { new DynamicLib () });
    instance.setExecutables (new Executable[] { new Executable () });
    assertTrue (instance.isSkip ());
    assertEquals (instance.getDefaults (), "windows");
    assertEquals (instance.getSources (), new Source[] { new Source () });
    assertEquals (instance.getHeaderFiles (), new HeaderFile[] { new HeaderFile () });
    assertEquals (instance.getStaticLibs (), new StaticLib[] { new StaticLib () });
    assertEquals (instance.getDynamicLibs (), new DynamicLib[] { new DynamicLib () });
    assertEquals (instance.getExecutables (), new Executable[] { new Executable () });
  }

  public void testDefaultsApplied_nothing () {
    final PackageMojo instance = new PackageMojo ();
    instance.setDefaults ("windows");
    instance.applyDefaults ();
    assertNull (instance.getSources ());
    assertNull (instance.getHeaderFiles ());
    assertNull (instance.getDynamicLibs ());
    assertNull (instance.getStaticLibs ());
    assertNull (instance.getExecutables ());
  }

  public void testDefaultsApplied_all () {
    final PackageMojo instance = new PackageMojo ();
    instance.setDefaults ("windows");
    instance.setSources (new Source[] { new Source (), new HeaderFile (), new StaticLib (),
        new DynamicLib (), new Executable () });
    instance.setHeaderFiles (new HeaderFile[] { new HeaderFile () });
    instance.setStaticLibs (new StaticLib[] { new StaticLib () });
    instance.setDynamicLibs (new DynamicLib[] { new DynamicLib () });
    instance.setExecutables (new Executable[] { new Executable () });
    instance.applyDefaults ();
    final HeaderFile headerFile = new HeaderFile ();
    headerFile.setPath ("target\\include");
    headerFile.setPattern ("*.h");
    final StaticLib staticLib = new StaticLib ();
    staticLib.setPath ("target\\lib");
    staticLib.setPattern ("*.lib");
    staticLib.setHeaders (new HeaderFile[] { headerFile });
    final DynamicLib dynamicLib = new DynamicLib ();
    dynamicLib.setPath ("target\\dll");
    dynamicLib.setPattern ("*.dll");
    final StaticLib implib = new StaticLib ();
    implib.setPath ("target\\dll");
    implib.setPattern ("*.lib");
    dynamicLib.setImplibs (new StaticLib[] { implib });
    dynamicLib.setHeaders (new HeaderFile[] { headerFile });
    final Executable executable = new Executable ();
    executable.setPath ("target\\bin");
    executable.setPattern ("*.exe");
    final StaticLib execStaticLib = new StaticLib ();
    execStaticLib.setPath ("target\\bin");
    execStaticLib.setPattern ("*.lib");
    final DynamicLib execDynamicLib = new DynamicLib ();
    execDynamicLib.setPath ("target\\bin");
    execDynamicLib.setPattern ("*.dll");
    executable.setLibraries (new ArchSource[] { execStaticLib, execDynamicLib });
    assertEquals (instance.getSources (), new Source[] { new Source (), headerFile, staticLib,
        dynamicLib, executable });
    assertEquals (instance.getHeaderFiles (), new HeaderFile[] { headerFile });
    assertEquals (instance.getStaticLibs (), new StaticLib[] { staticLib });
    assertEquals (instance.getDynamicLibs (), new DynamicLib[] { dynamicLib });
    assertEquals (instance.getExecutables (), new Executable[] { executable });
  }

  public void testNoDefaults () {
    final PackageMojo instance = new PackageMojo ();
    instance.applyDefaults ();
    assertFalse (instance.isSkip ());
    assertNull (instance.getSources ());
    assertNull (instance.getHeaderFiles ());
    assertNull (instance.getDynamicLibs ());
    assertNull (instance.getStaticLibs ());
    assertNull (instance.getExecutables ());
  }

  public void testGatherSources () {
    final PackageMojo instance = new PackageMojo ();
    final Source text = new Source ();
    text.setPath (".");
    text.setPattern ("*.txt");
    final ArchSource text32 = new ArchSource ();
    text32.setArch ("i386");
    text32.setPath ("src32");
    text32.setPattern ("*.doc");
    final ArchSource text64 = new ArchSource ();
    text64.setArch ("x64");
    text64.setPath ("src64");
    text64.setPattern ("*.doc");
    final ArchSource textNoarch = new ArchSource ();
    text64.setPath ("src");
    text64.setPattern ("*.doc");
    final Executable executable = new Executable ();
    executable.setPath ("bin");
    executable.setPattern ("*.exe");
    final Executable executable32 = new Executable ();
    executable32.setArch ("i386");
    executable32.setPath ("bin32");
    executable32.setPattern ("*.exe");
    final Executable executable64 = new Executable ();
    executable64.setArch ("x64");
    executable64.setPath ("bin");
    executable64.setPattern ("*.exe");
    instance.setSources (new Source[] { text, text32, text64, textNoarch });
    instance.setExecutables (new Executable[] { executable, executable32, executable64 });
    final Map<Source, String> sources = instance.gatherSources ();
    assertEquals (sources.get (text), "");
    assertEquals (sources.get (text32), "i386" + File.separatorChar);
    assertEquals (sources.get (text64), "x64" + File.separatorChar);
    assertEquals (sources.get (textNoarch), "");
    assertEquals (sources.get (executable), "bin" + File.separatorChar);
    assertEquals (sources.get (executable32), "bin-i386" + File.separatorChar);
    assertEquals (sources.get (executable64), "bin-x64" + File.separatorChar);
  }

  public void testRegex () {
    assertEquals (PackageMojo.regex (null), "^.*$");
    assertEquals (PackageMojo.regex (""), "^.*$");
    assertEquals (PackageMojo.regex ("*.*"), "^.*\\..*$");
    assertEquals (PackageMojo.regex ("foo\\bar?"), "^foo\\\\bar.$");
  }

  @Test (expectedExceptions = MojoFailureException.class)
  public void testCantOpenTarget () throws Exception {
    final PackageMojo instance = new PackageMojo ();
    instance.setDefaults ("package-mojo-test");
    instance.setLog (Mockito.mock (Log.class));
    final MavenProject project = new MavenProject ();
    project.setArtifactId ("test");
    project.getBuild ().setDirectory ("target");
    project.setArtifact (Mockito.mock (Artifact.class));
    instance.setPluginContext (Collections.singletonMap ("project", project));
    final OutputStreamOpener outputStreams = Mockito.mock (OutputStreamOpener.class);
    Mockito.when (outputStreams.open (new File ("target" + File.separator + "test.zip")))
        .thenThrow (new FileNotFoundException ("target" + File.separator + "test.zip"));
    instance.setOutputStreams (outputStreams);
    instance.execute ();
  }

  @Test (expectedExceptions = MojoFailureException.class)
  public void testCantOpenSource () throws Exception {
    final PackageMojo instance = new PackageMojo ();
    instance.setDefaults ("package-mojo-test");
    instance.setLog (Mockito.mock (Log.class));
    final MavenProject project = new MavenProject ();
    project.setArtifactId ("test");
    project.getBuild ().setDirectory ("target");
    project.setArtifact (Mockito.mock (Artifact.class));
    instance.setPluginContext (Collections.singletonMap ("project", project));
    final OutputStreamOpener outputStreams = Mockito.mock (OutputStreamOpener.class);
    Mockito.when (outputStreams.open (new File ("target" + File.separator + "test.zip")))
        .thenReturn (new ByteArrayOutputStream ());
    instance.setOutputStreams (outputStreams);
    instance.setInputStreams (new InputStreamOpener () {

      @Override
      public InputStream open (final File file) throws IOException {
        if (("src" + File.separator + "test" + File.separator + "files" + File.separator
            + "include" + File.separator + "5.h").equals (file.getPath ())) {
          throw new FileNotFoundException (file.getPath ());
        } else {
          return super.open (file);
        }
      }

    });
    instance.execute ();
  }

  public void testEmptySourceDir () throws Exception {
    final PackageMojo instance = new PackageMojo ();
    final Source empty = new Source ();
    empty.setPath ("missing-folder");
    empty.setPattern ("*");
    instance.setSources (new Source[] { empty });
    instance.setLog (Mockito.mock (Log.class));
    final MavenProject project = new MavenProject ();
    project.setArtifactId ("test");
    project.getBuild ().setDirectory ("target");
    project.setArtifact (Mockito.mock (Artifact.class));
    instance.setPluginContext (Collections.singletonMap ("project", project));
    final OutputStreamOpener outputStreams = Mockito.mock (OutputStreamOpener.class);
    Mockito.when (outputStreams.open (new File ("target" + File.separator + "test.zip")))
        .thenReturn (new ByteArrayOutputStream ());
    instance.setOutputStreams (outputStreams);
    instance.execute ();
  }

  @Test (expectedExceptions = MojoFailureException.class)
  public void testCantReadFromSource () throws Exception {
    final PackageMojo instance = new PackageMojo ();
    instance.setDefaults ("package-mojo-test");
    instance.setLog (Mockito.mock (Log.class));
    final MavenProject project = new MavenProject ();
    project.setArtifactId ("test");
    project.getBuild ().setDirectory ("target");
    project.setArtifact (Mockito.mock (Artifact.class));
    instance.setPluginContext (Collections.singletonMap ("project", project));
    final OutputStreamOpener outputStreams = Mockito.mock (OutputStreamOpener.class);
    Mockito.when (outputStreams.open (new File ("target" + File.separator + "test.zip")))
        .thenReturn (new ByteArrayOutputStream ());
    instance.setOutputStreams (outputStreams);
    instance.setInputStreams (new InputStreamOpener () {

      @Override
      public InputStream open (final File file) throws IOException {
        if (("src" + File.separator + "test" + File.separator + "files" + File.separator
            + "include" + File.separator + "5.h").equals (file.getPath ())) {
          return new FileInputStream (file) {

            @Override
            public int read (final byte[] buffer, final int ofs, final int len) throws IOException {
              throw new IOException ();
            }

          };
        } else {
          return super.open (file);
        }
      }

    });
    instance.execute ();
  }

  @Test (expectedExceptions = MojoFailureException.class)
  public void testCantWriteToTarget () throws Exception {
    final PackageMojo instance = new PackageMojo ();
    instance.setDefaults ("package-mojo-test");
    instance.setLog (Mockito.mock (Log.class));
    final MavenProject project = new MavenProject ();
    project.setArtifactId ("test");
    project.getBuild ().setDirectory ("target");
    project.setArtifact (Mockito.mock (Artifact.class));
    instance.setPluginContext (Collections.singletonMap ("project", project));
    final OutputStreamOpener outputStreams = Mockito.mock (OutputStreamOpener.class);
    Mockito.when (outputStreams.open (new File ("target" + File.separator + "test.zip")))
        .thenReturn (new ByteArrayOutputStream () {

          @Override
          public void close () throws IOException {
            throw new IOException ();
          }

        });
    instance.setOutputStreams (outputStreams);
    instance.execute ();
  }

}
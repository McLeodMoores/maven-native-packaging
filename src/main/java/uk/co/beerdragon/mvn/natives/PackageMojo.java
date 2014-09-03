/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package uk.co.beerdragon.mvn.natives;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import uk.co.beerdragon.misc.IOCallback;
import uk.co.beerdragon.misc.IOCallback.IOExceptionHandler;
import uk.co.beerdragon.misc.InputStreamOpener;
import uk.co.beerdragon.misc.OutputStreamOpener;
import uk.co.beerdragon.mvn.natives.defaults.Defaults;

import com.google.common.io.PatternFilenameFilter;

/**
 * Implementation of the {@code package} goal.
 */
public class PackageMojo extends AbstractMojo {

  private InputStreamOpener _inputStreams = new InputStreamOpener ();

  private OutputStreamOpener _outputStreams = new OutputStreamOpener ();

  private boolean _skip;

  private String _defaults;

  private Source[] _sources;

  private HeaderFile[] _headerFiles;

  private StaticLib[] _staticLibs;

  private DynamicLib[] _dynamicLibs;

  private Executable[] _executables;

  /* package */InputStreamOpener getInputStreams () {
    return _inputStreams;
  }

  /* package */void setInputStreams (final InputStreamOpener inputStreams) {
    _inputStreams = Objects.requireNonNull (inputStreams);
  }

  /* package */OutputStreamOpener getOutputStreams () {
    return _outputStreams;
  }

  /* package */void setOutputStreams (final OutputStreamOpener outputStreams) {
    _outputStreams = Objects.requireNonNull (outputStreams);
  }

  public boolean isSkip () {
    return _skip;
  }

  public void setSkip (final boolean skip) {
    _skip = skip;
  }

  public String getDefaults () {
    return _defaults;
  }

  public void setDefaults (final String defaults) {
    _defaults = defaults;
  }

  public Source[] getSources () {
    return ArrayUtils.clone (_sources);
  }

  public void setSources (final Source[] sources) {
    _sources = ArrayUtils.clone (sources);
  }

  public HeaderFile[] getHeaderFiles () {
    return ArrayUtils.clone (_headerFiles);
  }

  public void setHeaderFiles (final HeaderFile[] headerFiles) {
    _headerFiles = ArrayUtils.clone (headerFiles);
  }

  public StaticLib[] getStaticLibs () {
    return ArrayUtils.clone (_staticLibs);
  }

  public void setStaticLibs (final StaticLib[] staticLibs) {
    _staticLibs = ArrayUtils.clone (staticLibs);
  }

  public DynamicLib[] getDynamicLibs () {
    return ArrayUtils.clone (_dynamicLibs);
  }

  public void setDynamicLibs (final DynamicLib[] dynamicLibs) {
    _dynamicLibs = ArrayUtils.clone (dynamicLibs);
  }

  public Executable[] getExecutables () {
    return ArrayUtils.clone (_executables);
  }

  public void setExecutables (final Executable[] executables) {
    _executables = ArrayUtils.clone (executables);
  }

  /* package */void applyDefaults () {
    final Defaults defaults = Defaults.get (getDefaults ());
    defaults.applyTo (getSources ());
    if (getHeaderFiles () != null) {
      defaults.applyTo (getHeaderFiles ());
    } else {
      setHeaderFiles (defaults.createDefaultHeaderFiles ());
    }
    if (getDynamicLibs () != null) {
      defaults.applyTo (getDynamicLibs ());
    } else {
      setDynamicLibs (defaults.createDefaultDynamicLibs ());
    }
    if (getStaticLibs () != null) {
      defaults.applyTo (getStaticLibs ());
    } else {
      setStaticLibs (defaults.createDefaultStaticLibs ());
    }
    if (getExecutables () != null) {
      defaults.applyTo (getExecutables ());
    } else {
      setExecutables (defaults.createDefaultExecutables ());
    }
  }

  private static class SourceGatherer extends SourceVisitor {

    private final Map<Source, String> _found;

    private final String _arch;

    private final String _path;

    public SourceGatherer () {
      _found = new HashMap<Source, String> ();
      _arch = null;
      _path = null;
    }

    private SourceGatherer (final SourceGatherer parent, final ArchSource inherit) {
      _found = parent._found;
      _arch = parent.getArch (inherit);
      _path = parent.getPath (inherit);
    }

    private String getPath (final Source source) {
      return ObjectUtils.defaultIfNull (source.getPath (), _path);
    }

    private String getArch (final ArchSource source) {
      return ObjectUtils.defaultIfNull (source.getArch (), _arch);
    }

    private String getArchLabel (final ArchSource source) {
      return ObjectUtils.defaultIfNull (getArch (source), "");
    }

    private String getArchSuffix (final ArchSource source) {
      final String arch = getArchLabel (source);
      if (arch.length () > 0) {
        return "-" + arch;
      } else {
        return arch;
      }
    }

    private void storePath (final Source source, String path) {
      if (path.length () > 0) path = path + File.separator;
      if (source.getDest () != null) {
        path = path + source.getDest () + File.separator;
      }
      _found.put (source, path);
    }

    @Override
    protected void visitSource (final Source source) {
      source.setPath (ObjectUtils.defaultIfNull (source.getPath (), _path));
      storePath (source, "");
    }

    @Override
    protected void visitArchSource (final ArchSource source) {
      source.setArch (ObjectUtils.defaultIfNull (source.getArch (), _arch));
      super.visitArchSource (source);
      storePath (source, getArchLabel (source));
    }

    @Override
    protected void visitDynamicLib (final DynamicLib library) {
      final SourceVisitor nested = new SourceGatherer (this, library);
      nested.applyTo (library.getHeaders ());
      nested.applyTo (library.getImplibs ());
      super.visitDynamicLib (library);
      storePath (library, "bin" + getArchSuffix (library));
    }

    @Override
    protected void visitExecutable (final Executable executable) {
      final SourceVisitor nested = new SourceGatherer (this, executable);
      nested.applyTo (executable.getHeaders ());
      nested.applyTo (executable.getLibraries ());
      super.visitExecutable (executable);
      storePath (executable, "bin" + getArchSuffix (executable));
    }

    @Override
    protected void visitStaticLib (final StaticLib library) {
      final SourceVisitor nested = new SourceGatherer (this, library);
      nested.applyTo (library.getHeaders ());
      super.visitStaticLib (library);
      storePath (library, "lib" + getArchSuffix (library));
    }

    @Override
    protected void visitHeaderFile (final HeaderFile header) {
      super.visitHeaderFile (header);
      storePath (header, "include");
    }

  }

  /* package */Map<Source, String> gatherSources () {
    final SourceGatherer visitor = new SourceGatherer ();
    visitor.applyTo (getSources ());
    visitor.applyTo (getHeaderFiles ());
    visitor.applyTo (getDynamicLibs ());
    visitor.applyTo (getStaticLibs ());
    visitor.applyTo (getExecutables ());
    return visitor._found;
  }

  /* package */static String regex (final String pattern) {
    if ((pattern == null) || (pattern.length () == 0)) return "^.*$";
    final StringBuilder sb = new StringBuilder ();
    sb.append ('^');
    for (int i = 0; i < pattern.length (); i++) {
      final char c = pattern.charAt (i);
      switch (c) {
      case '.':
        sb.append ('\\').append ('.');
        break;
      case '\\':
        sb.append ('\\').append ('\\');
        break;
      case '?':
        sb.append ('.');
        break;
      case '*':
        sb.append ('.').append ('*');
        break;
      default:
        sb.append (c);
        break;
      }
    }
    sb.append ('$');
    return sb.toString ();
  }

  // Mojo

  @Override
  public void execute () throws MojoExecutionException, MojoFailureException {
    if (isSkip ()) {
      getLog ().debug ("Skipping step");
      return;
    }
    applyDefaults ();
    final MavenProject project = (MavenProject)getPluginContext ().get ("project");
    final File targetDir = new File (project.getBuild ().getDirectory ());
    targetDir.mkdirs ();
    final File targetFile = new File (targetDir, project.getArtifactId () + ".zip");
    getLog ().debug ("Writing to " + targetFile);
    final OutputStream output;
    try {
      output = getOutputStreams ().open (targetFile);
    } catch (final IOException e) {
      throw new MojoFailureException ("Can't write to " + targetFile);
    }
    final IOExceptionHandler errorLog = new MojoLoggingErrorCallback (this);
    if ((new IOCallback<OutputStream, Boolean> (output) {

      @Override
      protected Boolean apply (final OutputStream output) throws IOException {
        final byte[] buffer = new byte[4096];
        final ZipOutputStream zip = new ZipOutputStream (new BufferedOutputStream (output));
        for (final Map.Entry<Source, String> sourceInfo : gatherSources ().entrySet ()) {
          final Source source = sourceInfo.getKey ();
          getLog ().info (
              "Processing " + source.getPath () + " into " + sourceInfo.getValue () + " ("
                  + source.getPattern () + ")");
          final File folder = new File (source.getPath ());
          final String[] files = folder.list (new PatternFilenameFilter (regex (source
              .getPattern ())));
          if (files != null) {
            for (final String file : files) {
              getLog ().debug ("Adding " + file + " to archive");
              final ZipEntry entry = new ZipEntry (sourceInfo.getValue () + file);
              zip.putNextEntry (entry);
              if ((new IOCallback<InputStream, Boolean> (getInputStreams ().open (
                  new File (folder, file))) {

                @Override
                protected Boolean apply (final InputStream input) throws IOException {
                  int bytes;
                  while ((bytes = input.read (buffer, 0, buffer.length)) > 0) {
                    zip.write (buffer, 0, bytes);
                  }
                  return Boolean.TRUE;
                }

              }).call (errorLog) != Boolean.TRUE) {
                return Boolean.FALSE;
              }
              zip.closeEntry ();
            }
          } else {
            getLog ().debug ("Source folder is empty or does not exist");
          }
        }
        zip.close ();
        return Boolean.TRUE;
      }

    }).call (errorLog) != Boolean.TRUE) {
      throw new MojoFailureException ("Error writing to " + targetFile);
    }
    project.getArtifact ().setFile (targetFile);
  }
}
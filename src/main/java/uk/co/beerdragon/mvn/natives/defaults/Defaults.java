/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package uk.co.beerdragon.mvn.natives.defaults;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import uk.co.beerdragon.misc.IOCallback;
import uk.co.beerdragon.mvn.natives.ArchSource;
import uk.co.beerdragon.mvn.natives.DynamicLib;
import uk.co.beerdragon.mvn.natives.Executable;
import uk.co.beerdragon.mvn.natives.HeaderFile;
import uk.co.beerdragon.mvn.natives.Source;
import uk.co.beerdragon.mvn.natives.SourceVisitor;
import uk.co.beerdragon.mvn.natives.StaticLib;

/**
 * Default values for plugin configuration. Configurations are supplied for common platform and
 * build conventions to simplify POM authoring.
 */
public class Defaults {

  private static final String IDENTIFIER_KEY = "identifier";

  private static final String DYNAMIC_LIB_KEY = "dynamic";

  private static final String EXECUTABLE_KEY = "exec";

  private static final String STATIC_LIB_KEY = "static";

  private static final String HEADER_FILE_KEY = "header";

  private static final String PATH_KEY = "path";

  private static final String PATTERN_KEY = "pattern";

  private static final String ARCH_KEY = "arch";

  private static final String IMPLIB_KEY = "implib";

  private static final String LIBRARY_KEY = "library";

  private static final String TYPE_KEY = "type";

  private static final String BUILD_COMMAND_KEY = "build";

  public static abstract class SourceDefaults {

    private String _path;

    private String _pattern;

    public void setPath (final String path) {
      _path = path;
    }

    public String getPath () {
      return _path;
    }

    public void setPattern (final String pattern) {
      _pattern = pattern;
    }

    public String getPattern () {
      return _pattern;
    }

    public void load (final Properties properties, final String prefix) {
      setPath (getSingle (properties, prefix, PATH_KEY));
      setPattern (getSingle (properties, prefix, PATTERN_KEY));
    }

    /* package */void saveImpl (final Properties properties, final String prefix,
        final AtomicInteger identifier) {
      setSingle (properties, prefix, PATH_KEY, getPath ());
      setSingle (properties, prefix, PATTERN_KEY, getPattern ());
    }

    public final void save (final Properties properties, final String prefix) {
      saveImpl (properties, prefix, new AtomicInteger ());
    }

    /* package */void apply (final Source bean) {
      bean.setPath (ObjectUtils.defaultIfNull (bean.getPath (), getPath ()));
      bean.setPattern (ObjectUtils.defaultIfNull (bean.getPattern (), getPattern ()));
    }

    /* package */abstract Source create ();

  }

  public static class ArchSourceDefaults extends SourceDefaults {

    private String _arch;

    public String getArch () {
      return _arch;
    }

    public void setArch (final String arch) {
      _arch = arch;
    }

    @Override
    public void load (final Properties properties, final String prefix) {
      setArch (getSingle (properties, prefix, ARCH_KEY));
      super.load (properties, prefix);
    }

    @Override
    /* package */void saveImpl (final Properties properties, final String prefix,
        final AtomicInteger identifier) {
      super.saveImpl (properties, prefix, identifier);
      setSingle (properties, prefix, ARCH_KEY, getArch ());
    }

    /* package */void apply (final ArchSource bean) {
      super.apply (bean);
      if (bean.getArch () == null) {
        bean.setArch (getArch ());
      }
    }

    @Override
    /* package */ArchSource create () {
      final ArchSource instance = new ArchSource ();
      apply (instance);
      return instance;
    }

  }

  public static class DynamicLibDefaults extends ArchSourceDefaults {

    private HeaderFileDefaults[] _headers;

    private StaticLibDefaults[] _implibs;

    public HeaderFileDefaults[] getHeaders () {
      return ArrayUtils.clone (_headers);
    }

    public void setHeaders (final HeaderFileDefaults[] headers) {
      _headers = ArrayUtils.clone (headers);
    }

    public StaticLibDefaults[] getImplibs () {
      return ArrayUtils.clone (_implibs);
    }

    public void setImplibs (final StaticLibDefaults[] implibs) {
      _implibs = ArrayUtils.clone (implibs);
    }

    @Override
    public void load (final Properties properties, final String prefix) {
      setHeaders (loadHeaderDefaults (properties, prefix));
      final String[] implibIdentifiers = getMultiple (properties, prefix, IMPLIB_KEY);
      if (implibIdentifiers != null) {
        final StaticLibDefaults[] implibs = new StaticLibDefaults[implibIdentifiers.length];
        for (int i = 0; i < implibIdentifiers.length; i++) {
          implibs[i] = new StaticLibDefaults ();
          implibs[i].load (properties, implibIdentifiers[i]);
        }
        setImplibs (implibs);
      }
      super.load (properties, prefix);
    }

    @Override
    /* package */void saveImpl (final Properties properties, final String prefix,
        final AtomicInteger identifier) {
      super.saveImpl (properties, prefix, identifier);
      saveMultiple (properties, prefix, HEADER_FILE_KEY, getHeaders (), identifier);
      saveMultiple (properties, prefix, IMPLIB_KEY, getImplibs (), identifier);
      if (prefix.charAt (0) == 'l') setSingle (properties, prefix, TYPE_KEY, DYNAMIC_LIB_KEY);
    }

    /* package */void applyLocal (final DynamicLib bean) {
      super.apply (bean);
      if (bean.getHeaders () == null) {
        bean.setHeaders (createHeaderDefaults (getHeaders ()));
      }
      if (bean.getImplibs () == null) {
        final StaticLibDefaults[] implibDefaults = getImplibs ();
        if (implibDefaults != null) {
          final StaticLib[] implibs = new StaticLib[implibDefaults.length];
          for (int i = 0; i < implibDefaults.length; i++) {
            implibs[i] = implibDefaults[i].create ();
          }
          bean.setImplibs (implibs);
        }
      }
    }

    /* package */void apply (final DynamicLib bean, final SourceVisitor visitor) {
      applyLocal (bean);
      visitor.applyTo (bean.getHeaders ());
      visitor.applyTo (bean.getImplibs ());
    }

    @Override
    /* package */DynamicLib create () {
      final DynamicLib instance = new DynamicLib ();
      applyLocal (instance);
      return instance;
    }

  }

  public static class ExecutableDefaults extends ArchSourceDefaults {

    private HeaderFileDefaults[] _headers;

    private ArchSourceDefaults[] _libraries;

    public HeaderFileDefaults[] getHeaders () {
      return ArrayUtils.clone (_headers);
    }

    public void setHeaders (final HeaderFileDefaults[] headers) {
      _headers = ArrayUtils.clone (headers);
    }

    public ArchSourceDefaults[] getLibraries () {
      return ArrayUtils.clone (_libraries);
    }

    public void setLibraries (final ArchSourceDefaults[] libraries) {
      _libraries = ArrayUtils.clone (libraries);
    }

    @Override
    public void load (final Properties properties, final String prefix) {
      setHeaders (loadHeaderDefaults (properties, prefix));
      final String[] libraryIdentifiers = getMultiple (properties, prefix, LIBRARY_KEY);
      if (libraryIdentifiers != null) {
        final ArchSourceDefaults[] libraries = new ArchSourceDefaults[libraryIdentifiers.length];
        for (int i = 0; i < libraryIdentifiers.length; i++) {
          final String type = getSingle (properties, libraryIdentifiers[i], TYPE_KEY);
          if (STATIC_LIB_KEY.equals (type)) {
            libraries[i] = new StaticLibDefaults ();
          } else if (DYNAMIC_LIB_KEY.equals (type)) {
            libraries[i] = new DynamicLibDefaults ();
          } else {
            libraries[i] = new ArchSourceDefaults ();
          }
          libraries[i].load (properties, libraryIdentifiers[i]);
        }
        setLibraries (libraries);
      }
      super.load (properties, prefix);
    }

    @Override
    /* package */void saveImpl (final Properties properties, final String prefix,
        final AtomicInteger identifier) {
      super.saveImpl (properties, prefix, identifier);
      saveMultiple (properties, prefix, HEADER_FILE_KEY, getHeaders (), identifier);
      saveMultiple (properties, prefix, LIBRARY_KEY, getLibraries (), identifier);
    }

    /* package */void applyLocal (final Executable bean) {
      super.apply (bean);
      if (bean.getHeaders () == null) {
        bean.setHeaders (createHeaderDefaults (getHeaders ()));
      }
      if (bean.getLibraries () == null) {
        final ArchSourceDefaults[] libraryDefaults = getLibraries ();
        if (libraryDefaults != null) {
          final ArchSource[] libraries = new ArchSource[libraryDefaults.length];
          for (int i = 0; i < libraries.length; i++) {
            libraries[i] = libraryDefaults[i].create ();
          }
          bean.setLibraries (libraries);
        }
      }
    }

    /* package */void apply (final Executable bean, final SourceVisitor visitor) {
      applyLocal (bean);
      visitor.applyTo (bean.getHeaders ());
      visitor.applyTo (bean.getLibraries ());
    }

    @Override
    /* package */Executable create () {
      final Executable instance = new Executable ();
      applyLocal (instance);
      return instance;
    }

  }

  public static class StaticLibDefaults extends ArchSourceDefaults {

    private HeaderFileDefaults[] _headers;

    public HeaderFileDefaults[] getHeaders () {
      return ArrayUtils.clone (_headers);
    }

    public void setHeaders (final HeaderFileDefaults[] headers) {
      _headers = ArrayUtils.clone (headers);
    }

    @Override
    public void load (final Properties properties, final String prefix) {
      super.load (properties, prefix);
      setHeaders (loadHeaderDefaults (properties, prefix));
    }

    @Override
    /* package */void saveImpl (final Properties properties, final String prefix,
        final AtomicInteger identifier) {
      super.saveImpl (properties, prefix, identifier);
      saveMultiple (properties, prefix, HEADER_FILE_KEY, getHeaders (), identifier);
      if (prefix.charAt (0) == 'l') setSingle (properties, prefix, TYPE_KEY, STATIC_LIB_KEY);
    }

    /* package */void applyLocal (final StaticLib bean) {
      super.apply (bean);
      if (bean.getHeaders () == null) {
        bean.setHeaders (createHeaderDefaults (getHeaders ()));
      }
    }

    /* package */void apply (final StaticLib bean, final SourceVisitor visitor) {
      applyLocal (bean);
      visitor.applyTo (bean.getHeaders ());
    }

    @Override
    /* package */StaticLib create () {
      final StaticLib instance = new StaticLib ();
      applyLocal (instance);
      return instance;
    }

  }

  public static class HeaderFileDefaults extends SourceDefaults {

    @Override
    /* package */HeaderFile create () {
      final HeaderFile instance = new HeaderFile ();
      super.apply (instance);
      return instance;
    }

  }

  private class ApplyDefaults extends SourceVisitor {

    private <T> T[] nullIfEmpty (final T[] arr) {
      if (arr != null) {
        if (arr.length > 0) {
          return arr;
        } else {
          return null;
        }
      } else {
        return null;
      }
    }

    @Override
    protected void visitSource (final Source instance) {
      instance.setPath (StringUtils.defaultIfEmpty (instance.getPath (), null));
      instance.setPattern (StringUtils.defaultIfEmpty (instance.getPattern (), null));
    }

    @Override
    protected void visitArchSource (final ArchSource instance) {
      instance.setArch (StringUtils.defaultIfEmpty (instance.getArch (), null));
      super.visitArchSource (instance);
    }

    @Override
    protected void visitDynamicLib (final DynamicLib instance) {
      getDynamicLibDefaults ().apply (instance, this);
      instance.setHeaders (nullIfEmpty (instance.getHeaders ()));
      instance.setImplibs (nullIfEmpty (instance.getImplibs ()));
      super.visitDynamicLib (instance);
    }

    @Override
    protected void visitExecutable (final Executable instance) {
      getExecutableDefaults ().apply (instance, this);
      instance.setHeaders (nullIfEmpty (instance.getHeaders ()));
      instance.setLibraries (nullIfEmpty (instance.getLibraries ()));
      super.visitExecutable (instance);
    }

    @Override
    protected void visitStaticLib (final StaticLib instance) {
      getStaticLibDefaults ().apply (instance, this);
      instance.setHeaders (nullIfEmpty (instance.getHeaders ()));
      super.visitStaticLib (instance);
    }

    @Override
    protected void visitHeaderFile (final HeaderFile instance) {
      getHeaderFileDefaults ().apply (instance);
      super.visitHeaderFile (instance);
    }

  };

  /**
   * The default document identifier.
   */
  private final String _identifier;

  /**
   * The dynamic library defaults.
   */
  private final DynamicLibDefaults _dynamicLibDefaults = new DynamicLibDefaults ();

  /**
   * The default dynamic libraries.
   */
  private DynamicLibDefaults[] _defaultDynamicLibs;

  /**
   * The executable defaults.
   */
  private final ExecutableDefaults _executableDefaults = new ExecutableDefaults ();

  /**
   * The default executables.
   */
  private ExecutableDefaults[] _defaultExecutables;

  /**
   * The static library defaults.
   */
  private final StaticLibDefaults _staticLibDefaults = new StaticLibDefaults ();

  /**
   * The default static libraries.
   */
  private StaticLibDefaults[] _defaultStaticLibs;

  /**
   * The header file defaults.
   */
  private final HeaderFileDefaults _headerFileDefaults = new HeaderFileDefaults ();

  /**
   * The default header files.
   */
  private HeaderFileDefaults[] _defaultHeaderFiles;

  /**
   * The build command.
   */
  private String _buildCommand;

  /**
   * Creates a new instance.
   * <p>
   * Note that instances are typically obtained through a call to {@link #get} instead of created
   * directly.
   * 
   * @param identifier
   *          the default document identifier, not {@code null}
   * @param properties
   *          the values to set into this instance
   */
  public Defaults (final String identifier, final Properties properties) {
    _identifier = properties.getProperty (IDENTIFIER_KEY, Objects.requireNonNull (identifier));
    _dynamicLibDefaults.load (properties, DYNAMIC_LIB_KEY);
    String[] ids = getMultiple (getSingle (properties, DYNAMIC_LIB_KEY));
    if (ids != null) {
      final DynamicLibDefaults[] defaults = new DynamicLibDefaults[ids.length];
      for (int i = 0; i < ids.length; i++) {
        (defaults[i] = new DynamicLibDefaults ()).load (properties, ids[i]);
      }
      setDefaultDynamicLibs (defaults);
    }
    _executableDefaults.load (properties, EXECUTABLE_KEY);
    ids = getMultiple (getSingle (properties, EXECUTABLE_KEY));
    if (ids != null) {
      final ExecutableDefaults[] defaults = new ExecutableDefaults[ids.length];
      for (int i = 0; i < ids.length; i++) {
        (defaults[i] = new ExecutableDefaults ()).load (properties, ids[i]);
      }
      setDefaultExecutables (defaults);
    }
    _staticLibDefaults.load (properties, STATIC_LIB_KEY);
    ids = getMultiple (getSingle (properties, STATIC_LIB_KEY));
    if (ids != null) {
      final StaticLibDefaults[] defaults = new StaticLibDefaults[ids.length];
      for (int i = 0; i < ids.length; i++) {
        (defaults[i] = new StaticLibDefaults ()).load (properties, ids[i]);
      }
      setDefaultStaticLibs (defaults);
    }
    _headerFileDefaults.load (properties, HEADER_FILE_KEY);
    ids = getMultiple (getSingle (properties, HEADER_FILE_KEY));
    if (ids != null) {
      final HeaderFileDefaults[] defaults = new HeaderFileDefaults[ids.length];
      for (int i = 0; i < ids.length; i++) {
        (defaults[i] = new HeaderFileDefaults ()).load (properties, ids[i]);
      }
      setDefaultHeaderFiles (defaults);
    }
    setDefaultBuildCommand (getSingle (properties, BUILD_COMMAND_KEY));
  }

  /**
   * Returns a stock instance.
   * <p>
   * Note that the named configuration might be an alias of the actual configuration document. If
   * the named configuration does not exist then an empty default document will be returned.
   * 
   * @param identifier
   *          the named configuration, or {@code null} for none
   * @return a configuration document, never {@code null}
   */
  public static Defaults get (final String identifier) {
    if (identifier == null) return new Defaults ("none", new Properties ());
    final Properties properties = (new IOCallback<InputStream, Properties> (
        Defaults.class.getResourceAsStream (identifier + ".defaults")) {

      @Override
      protected Properties apply (final InputStream resource) throws IOException {
        if (resource == null) {
          return null;
        } else {
          final Properties p = new Properties ();
          p.load (resource);
          return p;
        }
      }

    }).callWithAssertion ();
    if (properties != null) {
      return new Defaults (identifier, properties);
    } else {
      return new Defaults ("none", new Properties ());
    }
  }

  /**
   * Updates any uninitialised values in the bean with defaults if they are available.
   * 
   * @param bean
   *          the object to update, not {@code null}
   */
  public void applyTo (final Source bean) {
    new ApplyDefaults ().applyTo (bean);
  }

  /**
   * Applies {@link #applyTo(Source)} to all members of the array.
   * 
   * @param beans
   *          the objects to update, may be {@code null} but may not contain {@code null}
   */
  public void applyTo (final Source[] beans) {
    new ApplyDefaults ().applyTo (beans);
  }

  /**
   * Returns the identifier of this default document. Note that this might not match the name that
   * was requested by a call to {@link #get}.
   * 
   * @return the document identifier, never {@code null}
   */
  public String getIdentifier () {
    return _identifier;
  }

  /**
   * Returns the default settings used for incompletely initialised {@link DynamicLib} entries.
   * 
   * @return the default values, never {@code null}
   */
  public DynamicLibDefaults getDynamicLibDefaults () {
    return _dynamicLibDefaults;
  }

  /**
   * Returns the settings used to create default {@link DynamicLib} entries.
   * 
   * @return the default settings, or {@code null} if none
   */
  public DynamicLibDefaults[] getDefaultDynamicLibs () {
    return ArrayUtils.clone (_defaultDynamicLibs);
  }

  /**
   * Sets the settings used to create default {@link DynamicLib} entries.
   * 
   * @param defaults
   *          the settings to use, or {@code null} for none
   */
  public void setDefaultDynamicLibs (final DynamicLibDefaults[] defaults) {
    _defaultDynamicLibs = ArrayUtils.clone (defaults);
  }

  /**
   * Creates, configures, and returns any default {@link DynamicLib} entries from the configuration.
   * 
   * @return the default instances, or {@code null} for none
   */
  public DynamicLib[] createDefaultDynamicLibs () {
    final DynamicLibDefaults[] defaults = getDefaultDynamicLibs ();
    if (defaults == null) return null;
    final DynamicLib[] dynamicLibs = new DynamicLib[defaults.length];
    for (int i = 0; i < defaults.length; i++) {
      dynamicLibs[i] = defaults[i].create ();
    }
    applyTo (dynamicLibs);
    return dynamicLibs;
  }

  /**
   * Returns the default settings used for incompletely initialised {@link Executable} entries.
   * 
   * @return the default values, never {@code null}
   */
  public ExecutableDefaults getExecutableDefaults () {
    return _executableDefaults;
  }

  /**
   * Returns the settings used to create default {@link Executable} entries.
   * 
   * @return the default settings, or {@code null} if none
   */
  public ExecutableDefaults[] getDefaultExecutables () {
    return ArrayUtils.clone (_defaultExecutables);
  }

  /**
   * Sets the settings used to create default {@link Executable} entries.
   * 
   * @param defaults
   *          the settings to use, or {@code null} for none
   */
  public void setDefaultExecutables (final ExecutableDefaults[] defaults) {
    _defaultExecutables = ArrayUtils.clone (defaults);
  }

  /**
   * Creates, configures, and returns any default {@link Executable} entries from the configuration.
   * 
   * @return the default instances, or {@code null} for none
   */
  public Executable[] createDefaultExecutables () {
    final ExecutableDefaults[] defaults = getDefaultExecutables ();
    if (defaults == null) return null;
    final Executable[] executables = new Executable[defaults.length];
    for (int i = 0; i < defaults.length; i++) {
      executables[i] = defaults[i].create ();
    }
    applyTo (executables);
    return executables;
  }

  /**
   * Returns the default settings used for incompletely initialised {@link StaticLib} entries.
   * 
   * @return the default values, never {@code null}
   */
  public StaticLibDefaults getStaticLibDefaults () {
    return _staticLibDefaults;
  }

  /**
   * Returns the settings used to create default {@link StaticLib} entries.
   * 
   * @return the default settings, or {@code null} if none
   */
  public StaticLibDefaults[] getDefaultStaticLibs () {
    return ArrayUtils.clone (_defaultStaticLibs);
  }

  /**
   * Sets the settings used to create default {@link StaticLib} entries.
   * 
   * @param defaults
   *          the settings to use, or {@code null} for none
   */
  public void setDefaultStaticLibs (final StaticLibDefaults[] defaults) {
    _defaultStaticLibs = ArrayUtils.clone (defaults);
  }

  /**
   * Creates, configures, and returns any default {@link StaticLib} entries from the configuration.
   * 
   * @return the default instances, or {@code null} for none
   */
  public StaticLib[] createDefaultStaticLibs () {
    final StaticLibDefaults[] defaults = getDefaultStaticLibs ();
    if (defaults == null) return null;
    final StaticLib[] staticLibs = new StaticLib[defaults.length];
    for (int i = 0; i < defaults.length; i++) {
      staticLibs[i] = defaults[i].create ();
    }
    applyTo (staticLibs);
    return staticLibs;
  }

  /**
   * Returns the default settings used for incompletely initialised {@link HeaderFile} entries.
   * 
   * @return the default values, never {@code null}
   */
  public HeaderFileDefaults getHeaderFileDefaults () {
    return _headerFileDefaults;
  }

  /**
   * Returns the settings used to create default {@link HeaderFile} entries.
   * 
   * @return the default settings, or {@code null} if none
   */
  public HeaderFileDefaults[] getDefaultHeaderFiles () {
    return ArrayUtils.clone (_defaultHeaderFiles);
  }

  /**
   * Sets the settings used to create default {@link HeaderFile} entries.
   * 
   * @param defaults
   *          the settings to use, or {@code null} for none
   */
  public void setDefaultHeaderFiles (final HeaderFileDefaults[] defaults) {
    _defaultHeaderFiles = ArrayUtils.clone (defaults);
  }

  /**
   * Creates, configures, and returns any default {@link HeaderFile} entries from the configuration.
   * 
   * @return the default instances, or {@code null} for none
   */
  public HeaderFile[] createDefaultHeaderFiles () {
    final HeaderFileDefaults[] defaults = getDefaultHeaderFiles ();
    if (defaults == null) return null;
    final HeaderFile[] headerFiles = new HeaderFile[defaults.length];
    for (int i = 0; i < defaults.length; i++) {
      headerFiles[i] = defaults[i].create ();
    }
    applyTo (headerFiles);
    return headerFiles;
  }

  /**
   * Returns the default build command.
   * 
   * @return the default value, or {@code null} if none
   */
  public String getDefaultBuildCommand () {
    return _buildCommand;
  }

  /**
   * Sets the default build command.
   * 
   * @param buildCommand
   *          the build command, or {@code null} for none
   */
  public void setDefaultBuildCommand (final String buildCommand) {
    _buildCommand = buildCommand;
  }

  private static String getSingle (final Properties properties, final String key) {
    final String value = properties.getProperty (key);
    if (value != null) {
      return value.replace ('/', File.separatorChar);
    } else {
      return null;
    }
  }

  private static String getSingle (final Properties properties, final String prefix,
      final String key) {
    return getSingle (properties, prefix + "." + key);
  }

  private static void setSingle (final Properties properties, final String prefix,
      final String key, final String value) {
    if (value != null) {
      properties.setProperty (prefix + "." + key, value);
    }
  }

  private static String[] getMultiple (String valueString) {
    if (valueString == null) return null;
    valueString = valueString.trim ();
    if (valueString.length () == 0) return ArrayUtils.EMPTY_STRING_ARRAY;
    return valueString.split (";");
  }

  private static String[] getMultiple (final Properties properties, final String prefix,
      final String key) {
    return getMultiple (getSingle (properties, prefix, key));
  }

  private static void saveMultiple (final Properties properties, final String prefix,
      final String key, final SourceDefaults[] sources, final AtomicInteger identifier) {
    if (sources != null) {
      final String[] values = new String[sources.length];
      for (int i = 0; i < sources.length; i++) {
        sources[i].saveImpl (properties, values[i] = new StringBuilder ().append (key.charAt (0))
            .append (identifier.incrementAndGet ()).toString (), identifier);
      }
      setSingle (properties, prefix, key, StringUtils.join (values, ';'));
    }
  }

  private static HeaderFileDefaults[] loadHeaderDefaults (final Properties properties,
      final String prefix) {
    final String[] headers = getMultiple (properties, prefix, HEADER_FILE_KEY);
    if (headers == null) return null;
    final HeaderFileDefaults[] defaults = new HeaderFileDefaults[headers.length];
    for (int i = 0; i < headers.length; i++) {
      defaults[i] = new HeaderFileDefaults ();
      defaults[i].load (properties, headers[i]);
    }
    return defaults;
  }

  private static HeaderFile[] createHeaderDefaults (final HeaderFileDefaults[] headerDefaults) {
    if (headerDefaults == null) return null;
    final HeaderFile[] headers = new HeaderFile[headerDefaults.length];
    for (int i = 0; i < headers.length; i++) {
      headers[i] = headerDefaults[i].create ();
    }
    return headers;
  }

  /**
   * Writes the default document to a properties file, in the form that it can be restored from
   * later.
   * 
   * @param properties
   *          the buffer to receive the property entries, not {@code null}
   */
  public void save (final Properties properties) {
    properties.put (IDENTIFIER_KEY, getIdentifier ());
    getDynamicLibDefaults ().save (properties, DYNAMIC_LIB_KEY);
    getExecutableDefaults ().save (properties, EXECUTABLE_KEY);
    getStaticLibDefaults ().save (properties, STATIC_LIB_KEY);
    getHeaderFileDefaults ().save (properties, HEADER_FILE_KEY);
    properties.put (BUILD_COMMAND_KEY, getDefaultBuildCommand ());
  }

}
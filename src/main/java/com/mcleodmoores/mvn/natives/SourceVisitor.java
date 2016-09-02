/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

/**
 * Visitor pattern for handling different {@link Source} instance types.
 */
public class SourceVisitor {

  /**
   * Applies the visitor to a {@link Source} instance.
   * <p>
   * Sub-classes should normally override this method as the default implementation here will throw
   * {@link UnsupportedOperationException}.
   * 
   * @param instance
   *          the object to apply the visitor to, never {@code null}
   */
  protected void visitSource (final Source instance) {
    throw new UnsupportedOperationException ();
  }

  /**
   * Applies the visitor to a {@link ArchSource} instance.
   * <p>
   * If not overridden in a sub-class, the default implementation will fall back to calling
   * {@link #visitSource}.
   * 
   * @param instance
   *          the object to apply the visitor to, never {@code null}
   */
  protected void visitArchSource (final ArchSource instance) {
    visitSource (instance);
  }

  /**
   * Applies the visitor to a {@link DynamicLib} instance.
   * <p>
   * If not overridden in a sub-class, the default implementation will fall back to calling
   * {@link #visitArchSource}.
   * 
   * @param instance
   *          the object to apply the visitor to, never {@code null}
   */
  protected void visitDynamicLib (final DynamicLib instance) {
    visitArchSource (instance);
  }

  /**
   * Applies the visitor to a {@link Executable} instance.
   * <p>
   * If not overridden in a sub-class, the default implementation will fall back to calling
   * {@link #visitArchSource}.
   * 
   * @param instance
   *          the object to apply the visitor to, never {@code null}
   */
  protected void visitExecutable (final Executable instance) {
    visitArchSource (instance);
  }

  /**
   * Applies the visitor to a {@link StaticLib} instance.
   * <p>
   * If not overridden in a sub-class, the default implementation will fall back to calling
   * {@link #visitArchSource}.
   * 
   * @param instance
   *          the object to apply the visitor to, never {@code null}
   */
  protected void visitStaticLib (final StaticLib instance) {
    visitArchSource (instance);
  }

  /**
   * Applies the visitor to a {@link HeaderFile} instance.
   * <p>
   * If not overridden in a sub-class, the default implementation will fall back to calling
   * {@link #visitSource}.
   * 
   * @param instance
   *          the object to apply the visitor to, never {@code null}
   */
  protected void visitHeaderFile (final HeaderFile instance) {
    visitSource (instance);
  }

  /**
   * Applies this visitor to an instance. The object instance will call back into the appropriate
   * {@code visitXXX} method depending on its type.
   * 
   * @param instance
   *          the object to apply the visitor to, not {@code null}
   */
  public void applyTo (final Source instance) {
    instance.accept (this);
  }

  /**
   * Applies this visitor to multiple instances. This is equivalent to iterating over the array and
   * calling {@link #applyTo} for each member.
   * 
   * @param instances
   *          the object instances to apply the visitor to, may be {@code null} but may not contain
   *          {@code null}
   */
  public void applyTo (final Source[] instances) {
    if (instances != null) {
      for (final Source instance : instances) {
        applyTo (instance);
      }
    }
  }

}
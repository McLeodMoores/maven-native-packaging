/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.mvn.natives;

/**
 * Specialisation of {@link Source} to describe header files.
 */
public class HeaderFile extends Source {

  // Source

  @Override
  /* package */void accept (final SourceVisitor visitor) {
    visitor.visitHeaderFile (this);
  }

}
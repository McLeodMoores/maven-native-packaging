/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapper for {@link FileOutputStream} to simplify code coverage reporting and avoid working with
 * real files during tests.
 */
public class OutputStreamOpener {

  /**
   * Opens a file for writing.
   * 
   * @param file
   *          the file to write to, not {@code null}
   * @return the output stream, never {@code null}
   * @throws IOException
   *           if the output stream couldn't be opened
   */
  public OutputStream open (final File file) throws IOException {
    return new FileOutputStream (file);
  }

}
/*
 * Maven tools for native builds
 *
 * Copyright 2014 by Andrew Ian William Griffin <griffin@beerdragon.co.uk>.
 * Released under the GNU General Public License.
 */

package com.mcleodmoores.misc;

import java.io.Closeable;
import java.io.IOException;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Callback wrapper for I/O operations to simplify code coverage reporting.
 * <p>
 * A pattern along the lines of:
 * 
 * <pre class="java code">
 * <span class="i">Result foo</span> (<span class="i">parameters</span> ...) {
 *   <span class="k">try</span> (<span class="i">Resource x</span> = <span class="k">new</span> <span class="i">ResourceAllocation</span> (<span class="i">parameters</span>)) {
 *     <span class="c">// do something with x to create result ...</span>
 *     <span class="k">return</span> <span class="i">result</span>;
 *   } <span class="k">catch</span> (<span class="i">IOException e</span>) {
 *     <span class="c">// can't really do anything; ignore exception</span>
 *     <span class="k">return null</span>;
 *   }
 * }
 * </pre>
 * Can be re-written as:
 * 
 * <pre class="java code">
 * <span class="i">Result foo</span> (<span class="i">parameters</span> ...) {
 *   <span class="k">return</span> (<span class="k">new</span> <span class="i">IOCallback</span> (<span class="k">new</span> <span class="i">ResourceAllocation</span> (<span class="i">parameters</span>)) {
 *     <span class="k">protected</span> <span class="i">Result apply</span> (<span class="i">Resource x</span>) <span class="k">throws</span> <span class="i">IOException</span> {
 *       <span class="c">// do something with x to create result ...</span>
 *       <span class="k">return</span> <span class="i">result</span>;
 *     }
 *   }).<span class="i">callIgnoringException</span> ();
 * }
 * </pre>
 */
public abstract class IOCallback<Resource extends Closeable, Result> {

  /**
   * Callback for translating/handling an exception if thrown.
   */
  public interface IOExceptionHandler {

    /**
     * Called when there is an I/O exception.
     * 
     * @param e
     *          the exception that was thrown, never {@code null}
     */
    void exception (IOException e);

  }

  private static final IOExceptionHandler WRITE_TO_STDERR = new IOExceptionHandler () {

    @Override
    public void exception (final IOException e) {
      e.printStackTrace ();
    }

  };

  private static final IOExceptionHandler SILENT = new IOExceptionHandler () {

    @Override
    public void exception (final IOException e) {
      // No-op
    }

  };

  private static final IOExceptionHandler ASSERTION_ERROR = new IOExceptionHandler () {

    @Override
    public void exception (final IOException e) {
      throw new AssertionError ("Uncaught I/O exception", e);
    }

  };

  private static final IOExceptionHandler RUNTIME_EXCEPTION = new IOExceptionHandler () {

    @Override
    public void exception (final IOException e) {
      throw new RuntimeException ("Uncaught I/O exception", e);
    }

  };

  private final Resource _resource;

  /**
   * Create a new instance for making a callback on the given resource.
   * <p>
   * One of the {@code callXXX} methods must be made on the instance or the resource may never be
   * closed.
   * 
   * @param resource
   *          the resource to manage, possibly {@code null}
   */
  public IOCallback (final Resource resource) {
    _resource = resource;
  }

  /**
   * Applies the user operation on the resource. The resource will be closed after this method
   * returns. The handling of any exceptions thrown is determined by the {@code callXXX} method
   * used.
   * 
   * @param parameter
   *          the resource being managed by this callback, possibly {@code null}
   * @return the user result to return from the {@code callXXX} method
   */
  protected abstract Result apply (Resource parameter) throws IOException;

  /**
   * Applies the user operation with a custom exception handler.
   * 
   * @param handler
   *          the exception handler, or {@code null} for none
   */
  public final Result call (final IOExceptionHandler handler) {
    try {
      return apply (_resource);
    } catch (final IOException e) {
      ObjectUtils.defaultIfNull (handler, SILENT).exception (e);
      return null;
    } finally {
      if (_resource != null) {
        try {
          _resource.close ();
        } catch (final IOException e) {
          ObjectUtils.defaultIfNull (handler, SILENT).exception (e);
        }
      }
    }
  }

  /**
   * Applies the user operation on the resource, ignoring any I/O exceptions.
   * 
   * @return the user result or {@code null} if an I/O exception was thrown
   */
  public final Result callIgnoringException () {
    return call (SILENT);
  }

  /**
   * Applies the user operation on the resource, printing any I/O exceptions to stderr (see
   * {@link Throwable#printStackTrace}).
   * 
   * @return the user result or {@code null} if an I/O exception was thrown
   */
  public final Result callDisplayingException () {
    return call (WRITE_TO_STDERR);
  }

  /**
   * Applies the user operation on the resource, translating any I/O exceptions to
   * {@link AssertionError}.
   * 
   * @return the user result
   * @throws AssertionError
   *           if the user operation throws an I/O exception
   */
  public final Result callWithAssertion () {
    return call (ASSERTION_ERROR);
  }

  /**
   * Applies the user operation on the resource, translating any I/O exceptions to
   * {@link RuntimeException}.
   * 
   * @return the user result
   * @throws RuntimeException
   *           if the user operation throws an I/O exception
   */
  public final Result callWithRuntimeException () {
    return call (RUNTIME_EXCEPTION);
  }

}
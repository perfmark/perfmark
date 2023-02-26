/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * PerfMark is a very low overhead tracing library, designed for in-process use.
 * To use PerfMark, see the docs on {@link io.perfmark.PerfMark}.
 *
 * <p>The PerfMark project is split into several, cohesive pieces, that allow
 * users to selectively include what they need.  For users that want to add the
 * tracing hooks into their application, but not include the full implementation,
 * only the {@code io.perfmark} package is needed.  For consuming the data, the
 * {@code io.perfmark.tracewriter} package can be used to write to the Chrome
 * Trace Event format.
 *
 * <p>PerfMark includes several runtime-only dependencies that can be included
 * for fast recording.  The library will attempt to load these packages if they
 * are found on the classpath, falling back if they are unavailable.  To work
 * properly, a <i>generator</i> and a <i>recorder</i> are needed.  Generators
 * tell PerfMark if it is enabled or not.  Recorders tell PerfMark how to store
 * the data, if it is enabled.  The stable implementations are:
 *
 * <ul>
 *   <li>
 *     <pre>io.perfmark.java9</pre> - a wait-free recorder and low-overhead
 *     generator.  The recorder in this package has the lowest and most
 *     consistent amount of overhead.  The generator in this package is not
 *     the fastest, but has low overhead to enable / disable.
 *   </li>
 *   <li>
 *     <pre>io.perfmark.java7</pre> - a zero-overhead generator.  The generator
 *     here is more efficient than the one in the <pre>io.perfmark.java9</pre>
 *     package, but has a high overhead to enable / disable it.
 *   </li>
 *   <li>
 *     <pre>io.perfmark.java6</pre> - a blocking recorder and medium-overhead
 *     generator.  This package is included as a safe fallback for when advanced
 *     VM features are unavailable, or when running on Android.
 *   </li>
 * </ul>
 *
 * <p>The overhead of tracing is very low in all implementations, and has been
 * custom tailored for no-allocation.  See the documentation in README.md in the
 * root directory for more info.
 * 
 * @author Carl Mastrangelo
 * @see io.perfmark.tracewriter
 * @see <a href="https://www.perfmark.io/">PerfMark.io</a>
 * @see <a href="https://github.com/perfmark/perfmark">https://github.com/perfmark</a>
 *
 */
package io.perfmark;

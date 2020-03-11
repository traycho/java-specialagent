/* Copyright 2019 The OpenTracing Authors
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

package io.opentracing.contrib.specialagent;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public final class MavenUtil {
  public static DefaultArtifact clone(final Artifact artifact) {
    final DefaultArtifact clone = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getScope(), artifact.getType(), artifact.getClassifier(), artifact.getArtifactHandler());
    clone.setAvailableVersions(artifact.getAvailableVersions());
    clone.setBaseVersion(artifact.getBaseVersion());
    clone.setDependencyFilter(artifact.getDependencyFilter());
    clone.setDependencyTrail(artifact.getDependencyTrail());
    clone.setDownloadUrl(artifact.getDownloadUrl());
    clone.setFile(artifact.getFile());
    clone.setOptional(artifact.isOptional());
    clone.setRelease(artifact.isRelease());
    clone.setRepository(artifact.getRepository());
    clone.setResolved(artifact.isResolved());
    clone.setVersionRange(artifact.getVersionRange());
    return clone;
  }

  public static Dependency newDependency(final String groupId, final String artifactId, final String version) {
    final Dependency dependency = new Dependency();
    dependency.setGroupId(groupId);
    dependency.setArtifactId(artifactId);
    dependency.setVersion(version);
    return dependency;
  }

  /**
   * Returns {@code true} if the specified {@link MojoExecution} is in a
   * lifecycle phase, and the name of the lifecycle phase contains "test".
   *
   * @param execution The {@link MojoExecution}.
   * @return {@code true} if the specified {@link MojoExecution} is in a
   *         lifecycle phase, and the name of the lifecycle phase contains
   *         "test".
   * @throws NullPointerException If {@code execution} is null.
   */
  public static boolean isInTestPhase(final MojoExecution execution) {
    return execution.getLifecyclePhase() != null && execution.getLifecyclePhase().contains("test");
  }

  /**
   * Returns the {@link PluginExecution} in the {@code mojoExecution}, if a
   * plugin is currently being executed.
   *
   * @param execution The {@link MojoExecution}.
   * @return The {@link PluginExecution} in the {@code mojoExecution}, if a
   *         plugin is currently being executed.
   * @throws NullPointerException If {@code execution} is null.
   */
  public static PluginExecution getPluginExecution(final MojoExecution execution) {
    final Plugin plugin = execution.getPlugin();
    plugin.flushExecutionMap();
    for (final PluginExecution pluginExecution : plugin.getExecutions())
      if (pluginExecution.getId().equals(execution.getExecutionId()))
        return pluginExecution;

    return null;
  }

  /**
   * Returns {@code true} if a calling MOJO should skip execution due to the
   * {@code -Dmaven.test.skip} property. If the {@code -Dmaven.test.skip}
   * property is present, this method will return {@code true} when the phase
   * name of MOJO or plugin {@code execution} contains the string "test".
   *
   * @param execution The {@link MojoExecution}.
   * @param mavenTestSkip The {@code -Dmaven.test.skip} property.
   * @return {@code true} if a calling MOJO should skip execution due to the
   *         {@code -Dmaven.test.skip} property.
   */
  public static boolean shouldSkip(final MojoExecution execution, final boolean mavenTestSkip) {
    if (!mavenTestSkip)
      return false;

    if (execution != null && isInTestPhase(execution))
      return true;

    final PluginExecution pluginExecution = getPluginExecution(execution);
    return pluginExecution != null && pluginExecution.getPhase() != null && pluginExecution.getPhase().contains("test");
  }

  /**
   * Returns the filesystem path of {@code artifact} located in
   * {@code localRepository}.
   *
   * @param localRepository The local repository reference.
   * @param artifact The artifact.
   * @return The filesystem path of {@code dependency} located in
   *         {@code localRepository}.
   * @throws NullPointerException If {@code localRepository} or {@code artifact}
   *           is null.
   */
  public static URL getPathOf(final ArtifactRepository localRepository, final Artifact artifact) {
    final StringBuilder builder = new StringBuilder();
    builder.append(localRepository.getBasedir());
    builder.append(File.separatorChar);
    builder.append(artifact.getGroupId().replace('.', File.separatorChar));
    builder.append(File.separatorChar);
    builder.append(artifact.getArtifactId());
    builder.append(File.separatorChar);
    builder.append(artifact.getVersion());
    builder.append(File.separatorChar);
    builder.append(artifact.getArtifactId());
    builder.append('-').append(artifact.getVersion());
    if (artifact.getClassifier() != null)
      builder.append('-').append(artifact.getClassifier());

    try {
      return new URL("file", "", builder.append(".jar").toString());
    }
    catch (final MalformedURLException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  private static String getArtifactFile(final File dir) {
    try {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      final Model model = reader.read(new FileReader(new File(dir, "pom.xml")));
      final String version = model.getVersion() != null ? model.getVersion() : model.getParent().getVersion();
      return model.getArtifactId() + "-" + version + ".jar";
    }
    catch (final IOException | XmlPullParserException e) {
      throw new IllegalStateException(e);
    }
  }
  /**
   * Filters the specified array of {@code File} objects by checking if the file
   * name is included in the specified set of files to match.
   *
   * @param files The array of {@code File} objects to filter.
   * @param matches The set of {@code File} objects whose names are to be
   *          matched by the specified array of URL objects.
   * @param index The index value for stack tracking (must be called with 0).
   * @param depth The depth value for stack tracking (must be called with 0).
   * @return An array of {@code File} objects that have file names that belong
   *         to the specified files to match.
   */
  private static File[] filterUrlFileNames(final File[] files, final Set<File> matches, final int index, final int depth) {
    for (int i = index; i < files.length; ++i) {
      final File file = files[i];
      final String artifact;
      if (file.isDirectory() && "target".equals(file.getParentFile().getName()) && "classes".equals(file.getName()))
        artifact = getArtifactFile(file.getParentFile().getParentFile());
      else if (file.isFile() && file.getName().endsWith(".jar"))
        artifact = file.getName();
      else
        continue;

      for (final File match : matches) {
        if (artifact.equals(match.getName())) {
          final File[] results = filterUrlFileNames(files, matches, i + 1, depth + 1);
          results[depth] = file;
          return results;
        }
      }
    }

    return depth == 0 ? null : new File[depth];
  }

  /**
   * Filter the specified array of {@code File} objects to return the
   * Instrumentation Rule files as specified by the Dependency TGF file at
   * {@code dependencyUrl}.
   *
   * @param files The array of {@code File} objects to filter.
   * @param dependenciesTgf The contents of the TGF file that specify the
   *          dependencies.
   * @param includeOptional Whether to include dependencies marked as
   *          {@code (optional)}.
   * @param scopes An array of Maven scopes to include in the returned set, or
   *          {@code null} to include all scopes.
   * @return An array of {@code File} objects representing Instrumentation Rule
   *         files.
   * @throws IOException If an I/O error has occurred.
   */
  public static File[] filterRuleURLs(final File[] files, final String dependenciesTgf, final boolean includeOptional, final String ... scopes) throws IOException {
    final Set<File> matches = AssembleUtil.selectFromTgf(dependenciesTgf, includeOptional, scopes);
    return filterUrlFileNames(files, matches, 0, 0);
  }

  private MavenUtil() {
  }
}
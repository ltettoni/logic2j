/*
 * logic2j - "Bring Logic to your Java" - Copyright (c) 2017 Laurent.Tettoni@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.logic2j.contrib.helper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.core.impl.theory.TheoryContent;
import org.logic2j.core.impl.theory.TheoryManager;
import org.logic2j.engine.exception.PrologNonSpecificException;

/**
 * A builder to instantiate a Prolog implementation ready to be used.
 * This object helps with the following tasks:
 * - selecting the implementation to use
 * - set parameters
 * - configure the libraries (none, default, or extensions)
 * - load theoryFiles
 * <p/>
 * This object is designed to be used either programmatically with its fluent API,
 * or via CDI configuration since it is also a Java Bean whose properties can be injected.
 * See http://stackoverflow.com/questions/2901166/how-to-make-spring-accept-fluent-non-void-setters
 * regarding how to have both a Spring compatible Java Bean and a fluent API...
 */
public class FluentPrologBuilder implements PrologBuilder {

  private boolean noLibraries = false;

  private boolean coreLibraries = false;

  private Collection<File> theoryFiles = new ArrayList<>();
  private Collection<String> theoryResources = new ArrayList<>();

  @Override
  public PrologImplementation build() {
    final PrologReferenceImplementation.InitLevel initLevel;
    if (isNoLibraries()) {
      initLevel = PrologReferenceImplementation.InitLevel.L0_BARE;
    } else if (isCoreLibraries()) {
      initLevel = PrologReferenceImplementation.InitLevel.L1_CORE_LIBRARY;
    } else {
      initLevel = PrologReferenceImplementation.InitLevel.L2_BASE_LIBRARIES;
    }
    final PrologReferenceImplementation prolog = new PrologReferenceImplementation(initLevel);

    // Theories from files
    final TheoryManager theoryManager = prolog.getTheoryManager();
    try {
      for (File theory : theoryFiles) {
        final TheoryContent content = theoryManager.load(theory);
        theoryManager.addTheory(content);
      }
    } catch (IOException e) {
      throw new PrologNonSpecificException("Builder could not load theory: " + e);
    }
    // Theories from resources
    for (String resource : theoryResources) {
      final TheoryContent content = theoryManager.load(resource);
      theoryManager.addTheory(content);
    }

    return prolog;
  }


  // ---------------------------------------------------------------------------
  // Fluent API
  // ---------------------------------------------------------------------------

  public FluentPrologBuilder withoutLibraries(boolean noLibraries) {
    this.noLibraries = noLibraries;
    return this;
  }

  public FluentPrologBuilder withCoreLibraries(boolean coreLibraries) {
    this.coreLibraries = coreLibraries;
    return this;
  }


  public FluentPrologBuilder withTheory(File... files) {
      Collections.addAll(theoryFiles, files);
    return this;
  }

  public FluentPrologBuilder withTheory(String... resources) {
      Collections.addAll(theoryResources, resources);
    return this;
  }
  // ---------------------------------------------------------------------------
  // Accessors
  // ---------------------------------------------------------------------------

  public boolean isNoLibraries() {
    return noLibraries;
  }

  public void setNoLibraries(boolean noLibraries) {
    this.noLibraries = noLibraries;
  }

  public boolean isCoreLibraries() {
    return coreLibraries;
  }

  public void setCoreLibraries(boolean coreLibraries) {
    this.coreLibraries = coreLibraries;
  }

  public Collection<File> getTheoryFiles() {
    return theoryFiles;
  }

  public void setTheoryFiles(Collection<File> theoryFiles) {
    this.theoryFiles = theoryFiles;
  }

  public Collection<String> getTheoryResources() {
    return theoryResources;
  }

  public void setTheoryResources(Collection<String> theoryResources) {
    this.theoryResources = theoryResources;
  }
}

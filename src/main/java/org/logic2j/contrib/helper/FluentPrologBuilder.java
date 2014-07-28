package org.logic2j.contrib.helper;

import org.logic2j.core.api.Prolog;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.impl.PrologReferenceImplementation;
import org.logic2j.core.impl.theory.TheoryContent;
import org.logic2j.core.impl.theory.TheoryManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A builder to instantiate a Prolog implementation ready to be used.
 * This object helps with the following tasks:
 * - selecting the implementation to use
 * - parameterize
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

    private Collection<File> theoryFiles = new ArrayList<File>();
    private Collection<String> theoryResources = new ArrayList<String>();

  @Override
    public Prolog createInstance() {
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
            throw new PrologNonSpecificError("Builder could not load theory: " + e);
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
        for (File file : files) {
            theoryFiles.add(file);
        }
        return this;
    }

    public FluentPrologBuilder withTheory(String... resources) {
        for (String resource : resources) {
            theoryResources.add(resource);
        }
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

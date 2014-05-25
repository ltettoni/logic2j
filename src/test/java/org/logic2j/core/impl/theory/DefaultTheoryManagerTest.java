package org.logic2j.core.impl.theory;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.impl.PrologReferenceImplementation;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class DefaultTheoryManagerTest extends PrologTestBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTheoryManagerTest.class);

    /**
     * Use a bare {@link org.logic2j.core.impl.PrologImplementation} for testing the {@link TheoryManager} and how theories are loaded.
     */
    @Override
    protected PrologReferenceImplementation.InitLevel initLevel() {
        return PrologReferenceImplementation.InitLevel.L0_BARE;
    }

    /**
     * This is making sure all test theories are loadable (no syntax issue, etc).
     */
    @Test
    public void loadAllTestTheories() throws IOException {
        final File[] allTheoryFilesFromTestResourceDir = allTheoryFilesFromTestResourceDir();
        for (final File theory : allTheoryFilesFromTestResourceDir) {
            logger.info("Attempting to load theory at {}", theory);
            final TheoryManager theoryManager = new DefaultTheoryManager(this.prolog);
            final TheoryContent content = theoryManager.load(theory);
            logger.info("Loaded library with content={}", content);
        }
    }
}
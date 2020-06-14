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

package org.logic2j.core.impl.theory;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.impl.PrologReferenceImplementation;

import java.io.File;
import java.io.IOException;

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
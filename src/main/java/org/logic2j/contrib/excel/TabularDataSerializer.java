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

package org.logic2j.contrib.excel;

import java.io.*;

/**
 * Read and write {@link TabularData} in binary format.
 *
 * @note It's critical to use a BufferedReader
 */
public class TabularDataSerializer {

  private final File file;

  /**
   * @param theFile
   */
  public TabularDataSerializer(File theFile) {
    super();
    this.file = theFile;
  }

  public TabularData read() throws IOException, ClassNotFoundException {
    final InputStream in = new FileInputStream(this.file);
    final BufferedInputStream buf = new BufferedInputStream(in, 102400); // Absolutely critical for perf!!!
    final ObjectInput input = new ObjectInputStream(buf);
    try {
      final TabularData td = (TabularData) input.readObject();
      return td;
    } finally {
      input.close();
      buf.close();
      in.close();
    }
  }

  public void write(TabularData data) throws IOException {
    final FileOutputStream out = new FileOutputStream(this.file);
    final BufferedOutputStream buf = new BufferedOutputStream(out, 102400);
    final ObjectOutputStream oos = new ObjectOutputStream(buf);
    try {
      oos.writeObject(data);
    } finally {
      oos.close();
      buf.close();
      out.close();
    }
  }
}

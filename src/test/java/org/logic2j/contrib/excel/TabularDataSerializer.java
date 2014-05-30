/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Read and write {@link TabularData} in binary format.
 * 
 * @note It's critical to use a BufferedReader
 */
public class TabularDataSerializer {

    private final File file;

    /**
     * @param file
     */
    public TabularDataSerializer(File file) {
        super();
        this.file = file;
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

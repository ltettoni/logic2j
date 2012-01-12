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
package org.logic2j.library.impl.config;

import java.io.File;
import java.io.IOException;

import org.logic2j.PrologImpl;
import org.logic2j.PrologWithDataSourcesTestBase;

public class ConfigLibraryTest extends PrologWithDataSourcesTestBase {

  // FIXME: pourquoi un main() dans un test case JUnit?
    public static void main(String[] args) throws IOException {
        PrologImpl prolog = new PrologImpl();
        prolog.getTheoryManager().addTheory(prolog.getTheoryManager().load(new File("src/test/resources/test-config.pl")));
        System.out.println(prolog.solve("zipcodesdb_ZIP_CODE(\"10001\", Latitude, Longitude)").all().bindings());
    }
    
}

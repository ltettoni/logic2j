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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.logic2j.contrib.rdb.RDBBase;
import org.logic2j.core.api.ClauseProvider;
import org.logic2j.core.api.TermAdapter;
import org.logic2j.core.api.TermAdapter.FactoryMode;
import org.logic2j.core.api.model.Clause;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.symbol.Term;
import org.logic2j.core.api.model.var.Bindings;
import org.logic2j.core.impl.PrologImplementation;

/**
 * List {@link Clause}s (facts, never rules) from Excel files.
 */
public class ExcelClauseProvider implements ClauseProvider {

    private final PrologImplementation prolog;
    private final TermAdapter termAdapter;
    private final String fileName;
    private final boolean firstRowIsHeaders;
    private Sheet sheet;

    public ExcelClauseProvider(PrologImplementation theProlog, String fileName, boolean firstRowIsHeaders) {
        this.prolog = theProlog;
        this.termAdapter = new RDBBase.AllStringsAsAtoms(this.prolog);
        this.fileName = fileName;
        this.firstRowIsHeaders = firstRowIsHeaders;
        this.sheet = null;
    }

    @Override
    public Iterable<Clause> listMatchingClauses(Struct theGoal, Bindings theGoalBindings) {
        try {
            final String predicateName = theGoal.getName();
            final List<Clause> clauses = new ArrayList<Clause>();
            if (this.fileName.endsWith(".xls")) {
                if (this.sheet == null) {
                    final InputStream myxls = new FileInputStream(this.fileName);
                    final HSSFWorkbook workBook = new HSSFWorkbook(myxls);
                    this.sheet = workBook.getSheetAt(0);
                }
                final int totalRows = this.sheet.getPhysicalNumberOfRows();
                for (int i = this.firstRowIsHeaders ? 0 : 1; i < totalRows; i++) {
                    final HSSFRow row = ((HSSFSheet) this.sheet).getRow(i);
                    if (row != null) {
                        final int cells = row.getPhysicalNumberOfCells();
                        final Term[] args = new Term[cells];
                        for (int c = 0; c < cells; c++) {
                            final HSSFCell cell = row.getCell(c);
                            String value = "";
                            switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_FORMULA:
                                value = cell.getCellFormula();
                                // If it is a formula, then it must be a numeric value.
                                args[c] = this.termAdapter.term(value, FactoryMode.ANY_TERM);
                                break;
                            case Cell.CELL_TYPE_NUMERIC:
                                value = Double.toString(cell.getNumericCellValue());
                                args[c] = this.termAdapter.term(value, FactoryMode.ANY_TERM);
                                break;
                            case Cell.CELL_TYPE_STRING:
                                value = cell.getStringCellValue();
                                args[c] = this.termAdapter.term("\"" + value.replace("\"", "").replaceAll("\\r|\\n", "") + "\"", FactoryMode.LITERAL);
                                break;
                            default:
                                args[c] = this.termAdapter.term("\"" + value + "\"", FactoryMode.LITERAL);
                            }
                            // FIXME There is a problem if the content of the cell is too long
                            // args[c] = prolog.getTermFactory().create("\""+value.replace("\"", "").replaceAll("\\r|\\n", "")+"\"",
                            // FactoryMode.ANY_TERM);
                        }
                        final Clause cl = new Clause(this.prolog, new Struct(predicateName, args));
                        clauses.add(cl);
                    }
                }
            } else if (this.fileName.endsWith(".xlsx")) {
                if (this.sheet == null) {
                    final XSSFWorkbook workBook = new XSSFWorkbook(this.fileName);
                    this.sheet = workBook.getSheetAt(0);
                }
                final int totalRows = this.sheet.getPhysicalNumberOfRows();
                for (int i = this.firstRowIsHeaders ? 0 : 1; i < totalRows; i++) {
                    final XSSFRow row = ((XSSFSheet) this.sheet).getRow(i);
                    if (row != null) {
                        final int cells = row.getPhysicalNumberOfCells();
                        final Term[] args = new Term[cells];
                        for (int c = 0; c < cells; c++) {
                            final XSSFCell cell = row.getCell(c);
                            String value = "";
                            switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_FORMULA:
                                value = cell.getCellFormula();
                                // If it is a formula, then it must be a numeric value.
                                args[c] = this.termAdapter.term(value, FactoryMode.ANY_TERM);
                                break;
                            case Cell.CELL_TYPE_NUMERIC:
                                value = Double.toString(cell.getNumericCellValue());
                                args[c] = this.termAdapter.term(value, FactoryMode.ANY_TERM);
                                break;
                            case Cell.CELL_TYPE_STRING:
                                value = cell.getStringCellValue();
                                args[c] = this.termAdapter.term("\"" + value.replace("\"", "").replaceAll("\\r|\\n", "") + "\"", FactoryMode.LITERAL);
                                break;
                            default:
                                args[c] = this.termAdapter.term("\"" + value + "\"", FactoryMode.LITERAL);
                            }
                            // FIXME There is a problem if the content of the cell is too long
                            // args[c] = prolog.getTermFactory().create("\""+value.replace("\"", "").replaceAll("\\r|\\n", "")+"\"",
                            // FactoryMode.ANY_TERM);
                        }
                        final Clause cl = new Clause(this.prolog, new Struct(predicateName, args));
                        clauses.add(cl);
                    }
                }
            }
            return clauses;
        } catch (Exception e) {
            throw new PrologNonSpecificError("Could not parse Excel doc: " + e);
        }
    }

    @Override
    public void registerIntoResolver() {
        // Work in progress
    }

}

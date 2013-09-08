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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.logic2j.core.api.model.exception.PrologNonSpecificError;
import org.logic2j.core.impl.util.ReflectUtils;

/**
 * Read Excel files and expose whole content as {@link TabularData}.
 */
public class ExcelReader {

    private File file;
    private boolean firstRowIsHeaders;
    private int primaryKeyColumn;

    /**
     * @param file
     * @param firstRowIsHeaders True when first row contains column headers.
     * @param primaryKeyColumn The column (0-based) which should be considered as a unique (primary) key, or -1 for none.
     */
    public ExcelReader(File file, boolean firstRowIsHeaders, int primaryKeyColumn) {
        super();
        this.file = file;
        this.firstRowIsHeaders = firstRowIsHeaders;
        this.primaryKeyColumn = primaryKeyColumn;
    }

    // From : http://stackoverflow.com/questions/8710719/generating-an-alphabetic-sequence-in-java
    public String createSequenceElement(int index) {
        String sequenceElement = "";
        int first = index / 26;
        int second = index % 26;
        if (first < 1) {
            sequenceElement += (char) ('A' + second);
        } else {
            sequenceElement += createSequenceElement(first) + (char) ('A' + second);
        }
        return sequenceElement;
    }

    public TabularData read() throws IOException {
        Sheet sheet = null;
        if (this.file.getName().endsWith(".xls")) {
            if (sheet == null) {
                final InputStream myxls = new FileInputStream(this.file);
                final HSSFWorkbook workBook = new HSSFWorkbook(myxls);
                sheet = workBook.getSheetAt(0);
            }
            final int excelPhysicalRows = sheet.getPhysicalNumberOfRows();
            List<String> columnNames;
            if (this.firstRowIsHeaders) {
                columnNames = readRow(sheet, 0, String.class);
            } else {
                int nbColunms = ((HSSFSheet) sheet).getRow(0).getPhysicalNumberOfCells();
                final List<String> colNames = new ArrayList<String>();
                for (int i = 0; i < nbColunms; i++) {
                    colNames.add(createSequenceElement(i));
                }
                columnNames = colNames;
            }
            List<List<Serializable>> listData = new ArrayList<List<Serializable>>();

            for (int r = this.firstRowIsHeaders ? 1 : 0; r < excelPhysicalRows; r++) {
                List<Serializable> listRow = readRow(sheet, r, Serializable.class);
                if (listRow != null) {
                    // Sometimes
                    listData.add(listRow);
                }
            }
            TabularData tbl = new TabularData(columnNames, listData);
            tbl.predicateName = this.file.getName();
            tbl.rowIdentifierColumn = 0;
            return tbl;
        } else {
            throw new IOException("According to extension file may not be of Excel format: " + this.file);
        }
        // else if (this.fileName.endsWith(".xlsx")) {
        // if (this.sheet == null) {
        // final XSSFWorkbook workBook = new XSSFWorkbook(this.fileName);
        // this.sheet = workBook.getSheetAt(0);
        // }
        // final int totalRows = this.sheet.getPhysicalNumberOfRows();
        // for (int i = this.firstRowIsHeaders ? 0 : 1; i < totalRows; i++) {
        // final XSSFRow row = ((XSSFSheet) this.sheet).getRow(i);
        // if (row != null) {
        // final int cells = row.getPhysicalNumberOfCells();
        // final Term[] args = new Term[cells];
        // for (int c = 0; c < cells; c++) {
        // final XSSFCell cell = row.getCell(c);
        // String value = "";
        // switch (cell.getCellType()) {
        // case Cell.CELL_TYPE_FORMULA:
        // value = cell.getCellFormula();
        // // If it is a formula, then it must be a numeric value.
        // args[c] = this.termAdapter.term(value, FactoryMode.ANY_TERM);
        // break;
        // case Cell.CELL_TYPE_NUMERIC:
        // value = Double.toString(cell.getNumericCellValue());
        // args[c] = this.termAdapter.term(value, FactoryMode.ANY_TERM);
        // break;
        // case Cell.CELL_TYPE_STRING:
        // value = cell.getStringCellValue();
        // args[c] = this.termAdapter.term("\"" + value.replace("\"", "").replaceAll("\\r|\\n", "") + "\"", FactoryMode.LITERAL);
        // break;
        // default:
        // args[c] = this.termAdapter.term("\"" + value + "\"", FactoryMode.LITERAL);
        // }
        // // FIXME There is a problem if the content of the cell is too long
        // // args[c] = prolog.getTermFactory().create("\""+value.replace("\"", "").replaceAll("\\r|\\n", "")+"\"",
        // // FactoryMode.ANY_TERM);
        // }
        // final Clause cl = new Clause(this.prolog, new Struct(predicateName, args));
        // clauses.add(cl);
        // }
        // }
        // }
    }

    /**
     * @param sheet
     * @param row Row index
     * @return Null if row is empty or only containing nulls.
     */
    private <T> List<T> readRow(Sheet sheet, final int rowNumber, Class<T> theTargetClass) {
        final HSSFRow row = ((HSSFSheet) sheet).getRow(rowNumber);
        if (row == null) {
            return null;
        }
        final int nbCols = row.getPhysicalNumberOfCells();
        ArrayList<T> values = new ArrayList<T>();
        boolean hasSomeData = false;
        for (int c = 0; c < nbCols; c++) {
            final HSSFCell cell = row.getCell(c);
            Object value = null;
            switch (cell.getCellType()) {
            case Cell.CELL_TYPE_FORMULA:
                value = cell.getCellFormula();
                break;
            case Cell.CELL_TYPE_NUMERIC:
                value = cell.getNumericCellValue();
                break;
            case Cell.CELL_TYPE_STRING:
                value = cell.getStringCellValue();
                break;
            case Cell.CELL_TYPE_BLANK:
                value = null;
                break;
            default:
                throw new PrologNonSpecificError("Excel cell at row=" + rowNumber + ", column=" + c + " of type " + cell.getCellType() + " not handled, value is " + value);
            }
            value = map(value);
            if (value != null) {
                hasSomeData = true;
            }
            T cast = ReflectUtils.safeCastOrNull("casting Excel cell", value, theTargetClass);
            values.add(cast);
        }
        if (!hasSomeData) {
            return null;
        }
        return values;
    }

    /**
     * @param value
     * @return
     */
    private Object map(Object value) {
        if (value instanceof CharSequence) {
            return value.toString().trim();
        }
        return value;
    }
}

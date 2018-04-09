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

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.logic2j.engine.exception.InvalidTermException;
import org.logic2j.engine.util.TypeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Read Excel files and expose whole content as {@link TabularData}.
 */
public class ExcelReader {

  private final File file;
  private final boolean firstRowIsHeaders;
  private final int primaryKeyColumn;

  public ExcelReader(File theFile, boolean theFirstRowIsHeaders) {
    this(theFile, theFirstRowIsHeaders, -1);
  }

  /**
   * @param theFile
   * @param theFirstRowIsHeaders True when first row contains column headers.
   * @param thePrimaryKeyColumn  The column (0-based) which should be considered as a unique (primary) key, or -1 for none.
   */
  public ExcelReader(File theFile, boolean theFirstRowIsHeaders, int thePrimaryKeyColumn) {
    this.file = theFile;
    this.firstRowIsHeaders = theFirstRowIsHeaders;
    this.primaryKeyColumn = thePrimaryKeyColumn;
  }

  /**
   * @return Data read from cache and cached.
   * @throws java.io.IOException
   */
  public TabularData readCached() throws IOException {
    final File cached = cachedFile();
    if (cached.exists() && cached.isFile() && cached.canRead() && cached.lastModified() > this.file.lastModified()) {
      // We can use the cached version
      try {
        return new TabularDataSerializer(cached).read();
      } catch (final ClassNotFoundException e) {
        throw new IOException("Recent cached version of " + this.file + " located at " + cached + " was not loadable: " + e);
      }
    }
    // Read the file
    final TabularData data = read();
    // Cache it
    cached.getParentFile().mkdirs();
    new TabularDataSerializer(cached).write(data);
    return data;
  }

  public TabularData read() throws IOException {
    if (this.file.getName().endsWith(".xls")) {
      final InputStream myxls = new FileInputStream(this.file);
      final HSSFWorkbook workBook = new HSSFWorkbook(myxls);
      final Sheet sheet = workBook.getSheetAt(0);
      final int excelPhysicalRows = sheet.getPhysicalNumberOfRows();
      final List<String> columnNames;
      if (this.firstRowIsHeaders) {
        columnNames = readRow(sheet, 0, String.class);
      } else {
        final int nbColunms = ((HSSFSheet) sheet).getRow(0).getPhysicalNumberOfCells();
        final List<String> colNames = new ArrayList<>();
        for (int i = 0; i < nbColunms; i++) {
          colNames.add(createSequenceElement(i));
        }
        columnNames = colNames;
      }
      final List<List<Serializable>> listData = new ArrayList<>();

      for (int r = this.firstRowIsHeaders ? 1 : 0; r < excelPhysicalRows; r++) {
        final List<Serializable> listRow = readRow(sheet, r, Serializable.class);
        if (listRow != null) {
          // Sometimes
          listData.add(listRow);
        }
      }
      String dataSetName = this.file.getName();
      if (dataSetName.lastIndexOf('.') >= 0) {
        dataSetName = dataSetName.substring(0, dataSetName.lastIndexOf('.'));
      }
      assert columnNames!=null;
      final TabularData tbl = new TabularData(dataSetName, columnNames, listData);
      tbl.setPrimaryKeyColumn(this.primaryKeyColumn);
      return tbl;
    }
    throw new IOException("According to extension file may not be of Excel format: " + this.file);
        /*
         OOXML
        else if (this.fileName.endsWith(".xlsx")) {
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
                        // Note: There is a problem if the content of the cell is too long
                        // args[c] = prolog.getTermFactory().create("\""+value.replace("\"", "").replaceAll("\\r|\\n", "")+"\"",
                        // FactoryMode.ANY_TERM);
                    }
                    final Clause cl = new Clause(this.prolog, new Struct(dataSetName, args));
                    clauses.add(cl);
                }
            }
        }
        */
  }

  private File cachedFile() {
    final File tempDir = new File(System.getProperty("java.io.tmpdir"));
    final String relativePath = this.file.getPath();
    final File pathWithinTempDir = new File(tempDir, relativePath);
    return pathWithinTempDir;
  }

  /**
   * @param sheet
   * @param rowNumber      Row index
   * @param theTargetClass
   * @return Null if row is empty or only containing nulls.
   */
  private <T> List<T> readRow(Sheet sheet, final int rowNumber, Class<T> theTargetClass) {
    final HSSFRow row = ((HSSFSheet) sheet).getRow(rowNumber);
    if (row == null) {
      return null;
    }
    final int nbCols = row.getPhysicalNumberOfCells();
    final ArrayList<T> values = new ArrayList<>();
    boolean hasSomeData = false;
    for (int c = 0; c < nbCols; c++) {
      final HSSFCell cell = row.getCell(c);
      Object value = null;
      if (cell != null) {
        switch (cell.getCellTypeEnum()) {
          case FORMULA:
            value = cell.getCellFormula();
            break;
          case NUMERIC:
            value = cell.getNumericCellValue();
            break;
          case STRING:
            value = cell.getStringCellValue();
            break;
          case BLANK:
            break;
          default:
            throw new InvalidTermException(
                "Excel cell at row=" + rowNumber + ", column=" + c + " of type " + cell.getCellTypeEnum() + " " + "not handled");
        }
      }
      value = mapCellValue(value);
      if (value != null) {
        hasSomeData = true;
      }
      final T cast = TypeUtils.safeCastOrNull("casting Excel cell", value, theTargetClass);
      values.add(cast);
    }
    if (!hasSomeData) {
      return null;
    }
    return values;
  }

  private Object mapCellValue(Object value) {
    if (value instanceof CharSequence) {
      return value.toString().trim();
    }
    return value;
  }

  // From : http://stackoverflow.com/questions/8710719/generating-an-alphabetic-sequence-in-java
  private String createSequenceElement(int index) {
    final int first = index / 26;
    final int second = index % 26;
    if (first < 1) {
      return String.valueOf((char) ('A' + second));
    }
    return createSequenceElement(first) + (char) ('A' + second);
  }
}

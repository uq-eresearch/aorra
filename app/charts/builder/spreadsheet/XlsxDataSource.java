package charts.builder.spreadsheet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xml.sax.InputSource;

import charts.builder.spreadsheet.external.UnresolvedRef;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class XlsxDataSource extends SpreadsheetDataSource {

  private static final Pattern EXTERNAL_REF_FILE_PATH = Pattern.compile(
      "^xl/externalLinks/_rels/externalLink(\\d+).xml.rels$");

  private static final Pattern EXTERNAL_REF_FORMULA = Pattern.compile(
      "^\\[(\\d+)\\](.*)!\\$(\\w+)\\$(\\d+)$");

  private static final String REL_NS_URI =
      "http://schemas.openxmlformats.org/package/2006/relationships";

  private static final String REL_PREFIX = "ref";

  private Map<Integer, String> rmap = Maps.newHashMap();

  public XlsxDataSource(InputStream in) throws IOException {
    initWorkbook(initExternalRefs(in));
  }

  private InputStream initExternalRefs(InputStream in) throws IOException {
    final File tmpDir = Files.createTempDir();
    File fSpreadsheet = new File(tmpDir, "spreadsheet.xlsx");
    FileOutputStream out = new FileOutputStream(fSpreadsheet);
    IOUtils.copyLarge(in, out);
    try (ZipInputStream zip = new ZipInputStream(new FileInputStream(fSpreadsheet))) {
      ZipEntry entry;
      while((entry = zip.getNextEntry())!=null) {
        Matcher m = EXTERNAL_REF_FILE_PATH.matcher(entry.getName());
        if(m.matches()) {
          ByteArrayOutputStream bufOut = new ByteArrayOutputStream();
          IOUtils.copy(zip, bufOut);
          ByteArrayInputStream bufIn = new ByteArrayInputStream(bufOut.toByteArray());
          setupExternalRef(new Integer(m.group(1)), bufIn);
        }
      }
    } catch(Exception e) {}
    return new FileInputStream(fSpreadsheet) {
      @Override
      public void close() throws IOException {
        super.close();
        FileUtils.deleteQuietly(tmpDir);
      }
    };
  }

  private void setupExternalRef(Integer refnr, InputStream in) throws Exception {
    XPath xp = XPathFactory.newInstance().newXPath();
    xp.setNamespaceContext(new NamespaceContext() {
      @Override
      public String getNamespaceURI(String prefix) {
        if(StringUtils.equalsIgnoreCase(REL_PREFIX, prefix)) {
          return REL_NS_URI;
        }
        return "";
      }
      @Override
      public String getPrefix(String namespaceURI) {
        if(StringUtils.equalsIgnoreCase(REL_NS_URI, namespaceURI)) {
          return REL_PREFIX;
        }
        return "";
      }
      @Override
      public Iterator<?> getPrefixes(String namespaceURI) {
        return Lists.newArrayList(getPrefix(namespaceURI)).iterator();
      }});
    String target = xp.evaluate(
        "/ref:Relationships/ref:Relationship/@Target", new InputSource(in));
    if(StringUtils.isNotBlank(target)) {
      rmap.put(refnr, target);
    }
  }

  private void initWorkbook(InputStream in) throws IOException {
    XSSFWorkbook workbook = new XSSFWorkbook(in);
    FormulaEvaluator evaluator = workbook.getCreationHelper()
        .createFormulaEvaluator();
    init(workbook, evaluator);
  }

  private XlsxDataSource(Workbook workbook, FormulaEvaluator evaluator, int defaultSheet,
      Map<Integer, String> rmap) {
    super(workbook, evaluator, defaultSheet);
    this.rmap = rmap;
  }

  @Override
  public SpreadsheetDataSource toSheet(int sheet) {
    return new XlsxDataSource(workbook(), evaluator(), sheet, rmap);
  }

  @Override
  UnresolvedRef externalReference(Cell cell, String sheetname) {
    UnresolvedRef uref = null;
    if((cell != null) && (cell.getCellType() == Cell.CELL_TYPE_FORMULA)) {
      Matcher m = EXTERNAL_REF_FORMULA.matcher(cell.getCellFormula());
      if(m.matches()) {
        try {
          Integer refnr = new Integer(m.group(1));
          //FIXME escape sheet name
          CellReference cr = new CellReference(String.format("%s!%s%s",
              m.group(2), m.group(3), m.group(4)));
          String nameOrId = rmap.get(refnr);
          if(nameOrId != null) {
          //FIXME escape sheet name?
            uref = uref(nameOrId,
                cr.formatAsString(), String.format("%s!%s", sheetname,
                new CellReference(cell).formatAsString()));
          }
        } catch(Exception e) {}
      }
    }
    return uref;
  }
}

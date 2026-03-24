import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.Color
import java.awt.FlowLayout
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell as PdfCell
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory

data class Student(
    val name: String,
    val subject: String,
    val score: Double,
    var grade: String = "",
    var remark: String = ""
)

class GradeManager {

    fun calculateGrade(score: Double): Pair<String, String> {
        return when {
            score >= 90 -> Pair("A+", "Excellent")
            score >= 80 -> Pair("A",  "Tres bien")
            score >= 75 -> Pair("B+", "Bien +")
            score >= 70 -> Pair("B",  "Bien")
            score >= 65 -> Pair("C+", "Assez bien +")
            score >= 60 -> Pair("C",  "Assez bien")
            score >= 55 -> Pair("D+", "Passable +")
            score >= 50 -> Pair("D",  "Passable")
            score >= 0  -> Pair("F",  "Echec")
            else        -> Pair("?",  "Invalide")
        }
    }

    fun getGradeColor(grade: String): Color {
        return when (grade) {
            "A+", "A"  -> Color(198, 239, 206)
            "B+", "B"  -> Color(255, 235, 156)
            "C+", "C"  -> Color(255, 220, 100)
            "D+", "D"  -> Color(255, 199, 206)
            else       -> Color(255, 150, 150)
        }
    }

    fun readExcel(file: File): List<Student> {
        val students = mutableListOf<Student>()
        try {
            val workbook = XSSFWorkbook(file.inputStream())
            val sheet = workbook.getSheetAt(0)
            for (row in sheet) {
                if (row.rowNum == 0) continue
                val name    = row.getCell(0)?.toString()?.trim() ?: ""
                val subject = row.getCell(1)?.toString()?.trim() ?: ""
                val score   = getCellNumber(row.getCell(2))
                if (name.isNotBlank() && score >= 0) {
                    val (grade, remark) = calculateGrade(score)
                    students.add(Student(name, subject, score, grade, remark))
                }
            }
            workbook.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return students
    }

    private fun getCellNumber(cell: Cell?): Double {
        if (cell == null) return -1.0
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING  -> cell.stringCellValue.trim().toDoubleOrNull() ?: -1.0
            else             -> -1.0
        }
    }

    fun exportToExcel(students: List<Student>, outputFile: File) {
        val workbook = XSSFWorkbook()
        val sheet    = workbook.createSheet("Resultats")

        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont()
            font.color = IndexedColors.WHITE.index
            font.bold  = true
            setFont(font)
            alignment = HorizontalAlignment.CENTER
        }

        val headerRow = sheet.createRow(0)
        listOf("Nom", "Matiere", "Note", "Grade", "Appreciation").forEachIndexed { i, h ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        students.forEachIndexed { idx, s ->
            val row      = sheet.createRow(idx + 1)
            val awtColor = getGradeColor(s.grade)
            val rowStyle = workbook.createCellStyle().apply {
                val xssfColor = XSSFColor(
                    byteArrayOf(awtColor.red.toByte(), awtColor.green.toByte(), awtColor.blue.toByte()),
                    null
                )
                setFillForegroundColor(xssfColor)
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment   = HorizontalAlignment.CENTER
            }
            listOf(s.name, s.subject, s.score.toString(), s.grade, s.remark).forEachIndexed { i, v ->
                val cell = row.createCell(i)
                cell.setCellValue(v)
                cell.cellStyle = rowStyle
            }
        }

        for (i in 0..4) sheet.autoSizeColumn(i)
        FileOutputStream(outputFile).use { workbook.write(it) }
        workbook.close()
    }

    fun exportToPdf(students: List<Student>, path: String) {
        val writer   = PdfWriter(path)
        val pdf      = PdfDocument(writer)
        val document = Document(pdf)
        val fontBold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
        val fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        document.add(
            Paragraph("Rapport des Grades - Android App Development")
                .setFont(fontBold).setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15f)
        )

        val table = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 15f, 10f, 25f)))
            .useAllAvailableWidth()

        listOf("Nom", "Matiere", "Note", "Grade", "Appreciation").forEach { h ->
            table.addHeaderCell(
                PdfCell().add(Paragraph(h).setFont(fontBold).setFontSize(10f))
                    .setBackgroundColor(DeviceRgb(52, 73, 94))
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(6f)
            )
        }

        students.forEachIndexed { i, s ->
            val bg         = if (i % 2 == 0) DeviceRgb(255, 255, 255) else DeviceRgb(245, 245, 245)
            val awtGrade   = getGradeColor(s.grade)
            val gradeColor = DeviceRgb(awtGrade.red, awtGrade.green, awtGrade.blue)
            listOf(s.name, s.subject, s.score.toString(), s.grade, s.remark).forEachIndexed { col, v ->
                val cellBg = if (col == 3) gradeColor else bg
                table.addCell(
                    PdfCell().add(Paragraph(v).setFont(fontNormal).setFontSize(10f))
                        .setBackgroundColor(cellBg)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(4f)
                )
            }
        }

        document.add(table)

        if (students.isNotEmpty()) {
            val scores = students.map { it.score }
            document.add(Paragraph("\nStatistiques").setFont(fontBold).setFontSize(12f).setMarginTop(10f))
            document.add(Paragraph(
                "Total : ${students.size}   |   " +
                        "Moyenne : ${"%.1f".format(scores.average())}   |   " +
                        "Max : ${scores.max()}   |   " +
                        "Min : ${scores.min()}   |   " +
                        "Admis : ${students.count { it.score >= 50 }}/${students.size}"
            ).setFont(fontNormal).setFontSize(11f))
        }

        document.close()
    }

    fun exportToJson(students: List<Student>, outputFile: File) {
        val json = StringBuilder("[\n")
        students.forEachIndexed { i, s ->
            json.append("  {\n")
            json.append("    \"nom\": \"${s.name}\",\n")
            json.append("    \"matiere\": \"${s.subject}\",\n")
            json.append("    \"note\": ${s.score},\n")
            json.append("    \"grade\": \"${s.grade}\",\n")
            json.append("    \"avis\": \"${s.remark}\"\n")
            json.append("  }${if (i < students.size - 1) "," else ""}\n")
        }
        json.append("]")
        FileWriter(outputFile).use { it.write(json.toString()) }
    }

    fun createTemplate(file: File) {
        val dest = if (file.name.endsWith(".xlsx")) file else File(file.path + ".xlsx")
        val workbook = XSSFWorkbook()
        val sheet    = workbook.createSheet("Etudiants")
        val header   = sheet.createRow(0)
        header.createCell(0).setCellValue("Nom")
        header.createCell(1).setCellValue("Matiere")
        header.createCell(2).setCellValue("Note")
        val ex = sheet.createRow(1)
        ex.createCell(0).setCellValue("Jean Dupont")
        ex.createCell(1).setCellValue("Android App Development")
        ex.createCell(2).setCellValue(85.0)
        for (i in 0..2) sheet.autoSizeColumn(i)
        FileOutputStream(dest).use { workbook.write(it) }
        workbook.close()
    }
}

// INTERFACE GRAPHIQUE
fun main() {
    try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) } catch (e: Exception) {}

    SwingUtilities.invokeLater {
        val manager = GradeManager()

        val frame = JFrame("Grade Calculator v1.0")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(500, 400)
        frame.setLocationRelativeTo(null)
        frame.layout = FlowLayout(FlowLayout.CENTER, 20, 20)

        val inputField  = JTextField(15)
        val btnCalc     = JButton("Calculer Note")
        val resultLabel = JLabel("Entrez une note ou chargez un fichier")
        val btnExcel    = JButton("Importer Excel")
        val btnTemplate = JButton("Creer Modele Excel")

        resultLabel.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 14)

        btnCalc.addActionListener {
            val score = inputField.text.trim().toDoubleOrNull()
            if (score == null || score < 0 || score > 100) {
                resultLabel.text = "Entrez un nombre valide (0-100)"
                resultLabel.foreground = Color(200, 0, 0)
            } else {
                val (grade, remark) = manager.calculateGrade(score)
                resultLabel.text = "Resultat : $grade ($remark)"
                resultLabel.foreground = manager.getGradeColor(grade).darker()
            }
        }
        inputField.addActionListener { btnCalc.doClick() }

        btnExcel.addActionListener {
            val chooser = JFileChooser()
            chooser.fileFilter = FileNameExtensionFilter("Fichiers Excel (*.xlsx)", "xlsx")
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                val students = manager.readExcel(chooser.selectedFile)
                if (students.isEmpty()) {
                    JOptionPane.showMessageDialog(frame,
                        "Aucune donnee trouvee.\nColonnes attendues : Nom | Matiere | Note",
                        "Fichier vide", JOptionPane.WARNING_MESSAGE)
                    return@addActionListener
                }
                val options = arrayOf("Excel", "PDF", "JSON", "Tout")
                val choice  = JOptionPane.showOptionDialog(frame,
                    "${students.size} etudiant(s) charge(s). Format d'export ?",
                    "Export", 0, JOptionPane.QUESTION_MESSAGE, null, options, options[0])
                val path = chooser.selectedFile.parent
                when (choice) {
                    0 -> manager.exportToExcel(students, File("$path/Resultats.xlsx"))
                    1 -> manager.exportToPdf(students, "$path/Resultats.pdf")
                    2 -> manager.exportToJson(students, File("$path/Resultats.json"))
                    3 -> {
                        manager.exportToExcel(students, File("$path/Resultats.xlsx"))
                        manager.exportToPdf(students, "$path/Resultats.pdf")
                        manager.exportToJson(students, File("$path/Resultats.json"))
                    }
                }
                if (choice in 0..3)
                    JOptionPane.showMessageDialog(frame, "Export reussi !\nDossier : $path")
            }
        }

        btnTemplate.addActionListener {
            val chooser = JFileChooser()
            chooser.selectedFile = File("modele_etudiants.xlsx")
            if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                manager.createTemplate(chooser.selectedFile)
                JOptionPane.showMessageDialog(frame, "Modele cree !\nRemplissez : Nom | Matiere | Note")
            }
        }

        frame.add(JLabel("Note (0-100) :"))
        frame.add(inputField)
        frame.add(btnCalc)
        frame.add(resultLabel)
        frame.add(JSeparator(SwingConstants.HORIZONTAL))
        frame.add(btnExcel)
        frame.add(btnTemplate)

        frame.isVisible = true
    }
}
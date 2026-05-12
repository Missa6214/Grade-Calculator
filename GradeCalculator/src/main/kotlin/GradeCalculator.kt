import javax.swing.*           // JFrame, JButton, JLabel, JTextField... (composants visuels)
import java.awt.FlowLayout     // Disposition des éléments dans la fenêtre
import java.awt.Color          // Couleurs
import java.io.File            // Représente un fichier sur le disque
import org.apache.poi.xssf.usermodel.XSSFWorkbook   // Lire/écrire des fichiers Excel .xlsx
import org.apache.poi.ss.usermodel.*                 // Styles Excel (couleurs, polices...)
import com.itextpdf.kernel.pdf.PdfDocument           // Document PDF bas niveau
import com.itextpdf.kernel.pdf.PdfWriter             // Écrit le PDF dans un fichier
import com.itextpdf.layout.Document                  // Document PDF haut niveau
import com.itextpdf.layout.element.Paragraph         // Texte dans le PDF
import com.itextpdf.layout.element.Table             // Tableau dans le PDF
import com.itextpdf.layout.element.Cell              // Cellule du tableau PDF
import com.itextpdf.layout.properties.UnitValue      // Largeurs en pourcentage
import com.itextpdf.layout.properties.TextAlignment  // Alignement texte PDF
import com.itextpdf.kernel.colors.DeviceRgb          // Couleurs RGB pour PDF
import com.itextpdf.io.font.constants.StandardFonts  // Polices standard PDF
import com.itextpdf.kernel.font.PdfFontFactory       // Crée des polices PDF
import javax.swing.filechooser.FileNameExtensionFilter // Filtre fichiers dans JFileChooser

data class Student(
    val name: String,       // Nom de l'étudiant
    val subject: String,    // Matière
    val score: Double,      // Note (nombre décimal)
    var grade: String = "", // Grade calculé, vide par défaut
    var remark: String = "" // Appréciation, vide par défaut
)

class GradeManager {

    fun calculateGrade(score: Double): Pair<String, String> {
        return when {
            score >= 90 -> Pair("A+", "Excellent")
            score >= 80 -> Pair("A",  "Très bien")
            score >= 75 -> Pair("B+", "Bien +")
            score >= 70 -> Pair("B",  "Bien")
            score >= 65 -> Pair("C+", "Assez bien +")
            score >= 60 -> Pair("C",  "Assez bien")
            score >= 55 -> Pair("D+", "Passable +")
            score >= 50 -> Pair("D",  "Passable")
            score >= 0  -> Pair("F",  "Échec")
            else        -> Pair("?",  "Note invalide")
        }
    }


    fun getGradeColor(grade: String): Color {
        return when (grade) {
            "A+", "A"  -> Color(198, 239, 206) // Vert clair
            "B+", "B"  -> Color(255, 235, 156) // Jaune
            "C+", "C"  -> Color(255, 220, 100) // Orange clair
            "D+", "D"  -> Color(255, 199, 206) // Rose
            else       -> Color(255, 100, 100) // Rouge (F)
        }
    }


    fun readExcel(file: File): List<Student> {
        val students = mutableListOf<Student>() // Liste vide au départ

        try {
            val workbook = XSSFWorkbook(file.inputStream()) // Ouvre le fichier Excel
            val sheet = workbook.getSheetAt(0)              // Prend le 1er onglet

            for (row in sheet) {
                if (row.rowNum == 0) continue // Saute la ligne d'en-tête (ligne 0)

                // Lit chaque cellule de la ligne
                val nameCell    = row.getCell(0) ?: continue // "?: continue" = saute si null
                val subjectCell = row.getCell(1) ?: continue
                val scoreCell   = row.getCell(2) ?: continue

                // Convertit les cellules en types Kotlin
                val name    = getCellString(nameCell)
                val subject = getCellString(subjectCell)
                val score   = getCellNumber(scoreCell)

                if (name.isBlank() || score < 0) continue // Ignore les lignes vides/invalides


                val (grade, remark) = calculateGrade(score)


                students.add(Student(name, subject, score, grade, remark))

            }
            workbook.close()

        } catch (e: Exception) {
            // Si une erreur survient, on l'affiche dans la console
            println("Erreur lecture Excel : ${e.message}")
        }

        return students
    }

    private fun getCellString(cell: org.apache.poi.ss.usermodel.Cell): String {
        return when (cell.cellType) {
            CellType.STRING  -> cell.stringCellValue.trim()
            CellType.NUMERIC -> cell.numericCellValue.toInt().toString()
            else             -> ""
        }
    }

    private fun getCellNumber(cell: org.apache.poi.ss.usermodel.Cell): Double {
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING  -> cell.stringCellValue.trim().toDoubleOrNull() ?: -1.0
            // "toDoubleOrNull()" → retourne null si conversion impossible
            // "?: -1.0" → si null, utilise -1.0
            else             -> -1.0
        }
    }


    fun exportToExcel(students: List<Student>, outputFile: File) {
        val workbook = XSSFWorkbook() // Nouveau classeur Excel vide
        val sheet = workbook.createSheet("Résultats")

        val headerStyle = workbook.createCellStyle().apply {
            // "apply { }" configure l'objet sans répéter son nom
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont()
            font.color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
            font.bold = true
            setFont(font)
            alignment = HorizontalAlignment.CENTER
        }

        val headerRow = sheet.createRow(0)
        val headers = listOf("Nom", "Matière", "Note (/100)", "Grade", "Appréciation")
        headers.forEachIndexed { i, title ->
            // "forEachIndexed" parcourt la liste avec (index, valeur)
            val cell = headerRow.createCell(i)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }


        students.forEachIndexed { rowIdx, student ->
            val row = sheet.createRow(rowIdx + 1) // +1 pour sauter l'en-tête


            val awtColor = getGradeColor(student.grade)
            val colorStyle = workbook.createCellStyle().apply {
                val xssfColor = org.apache.poi.xssf.usermodel.XSSFColor(
                    byteArrayOf(awtColor.red.toByte(), awtColor.green.toByte(), awtColor.blue.toByte()),
                    null
                )
                setFillForegroundColor(xssfColor)
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
            }

            listOf(student.name, student.subject, student.score.toString(), student.grade, student.remark)
                .forEachIndexed { i, value ->
                    val cell = row.createCell(i)
                    cell.setCellValue(value)
                    cell.cellStyle = colorStyle
                }
        }

        for (i in 0..4) sheet.autoSizeColumn(i) // Ajuste la largeur automatiquement

        val fos = java.io.FileOutputStream(outputFile)
        workbook.write(fos)
        fos.close()
        workbook.close()
    }

    fun exportToPdf(students: List<Student>, path: String) {
        val writer   = PdfWriter(path)
        val pdf      = PdfDocument(writer)
        val document = Document(pdf)

        val fontBold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
        val fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA)


        document.add(
            Paragraph("Résultats — Android App Development")
                .setFont(fontBold).setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15f)
        )

        val table = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 15f, 10f, 25f)))
            .useAllAvailableWidth()

        listOf("Nom", "Matière", "Note (/100)", "Grade", "Appréciation").forEach { h ->
            table.addHeaderCell(
                Cell().add(Paragraph(h).setFont(fontBold).setFontSize(10f))
                    .setBackgroundColor(DeviceRgb(52, 73, 94))
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER).setPadding(6f)
            )
        }

        students.forEachIndexed { i, s ->
            val bg = if (i % 2 == 0) DeviceRgb(255, 255, 255) else DeviceRgb(245, 245, 245)
            val awtGrade = getGradeColor(s.grade)
            val gradeColor = DeviceRgb(awtGrade.red, awtGrade.green, awtGrade.blue)

            listOf(s.name, s.subject, s.score.toString(), s.grade, s.remark)
                .forEachIndexed { col, value ->
                    val cellBg = if (col == 3) gradeColor else bg // Colorie la colonne Grade
                    table.addCell(
                        Cell().add(Paragraph(value).setFont(fontNormal).setFontSize(10f))
                            .setBackgroundColor(cellBg)
                            .setTextAlignment(TextAlignment.CENTER).setPadding(4f)
                    )
                }
        }

        document.add(table)

        if (students.isNotEmpty()) {
            val scores = students.map { it.score }
            document.add(Paragraph("\nStatistiques").setFont(fontBold).setFontSize(12f).setMarginTop(10f))
            document.add(Paragraph("""
                • Total étudiants : ${students.size}
                • Moyenne         : ${"%.2f".format(scores.average())} / 100
                • Maximum         : ${scores.max()} / 100
                • Minimum         : ${scores.min()} / 100
                • Admis (≥50)     : ${students.count { it.score >= 50 }} / ${students.size}
            """.trimIndent()).setFont(fontNormal).setFontSize(11f))
        }

        document.close()
    }

    fun createTemplate(outputFile: File) {
        val workbook = XSSFWorkbook()
        val sheet    = workbook.createSheet("Étudiants")

        val headerRow = sheet.createRow(0)
        listOf("Nom de l'étudiant", "Matière", "Note (/100)").forEachIndexed { i, title ->
            headerRow.createCell(i).setCellValue(title)
        }

        sheet.createRow(1).apply {
            createCell(0).setCellValue("Jean Dupont")
            createCell(1).setCellValue("Android App Development")
            createCell(2).setCellValue(85.0)
        }
        sheet.createRow(2).apply {
            createCell(0).setCellValue("Marie Martin")
            createCell(1).setCellValue("Android App Development")
            createCell(2).setCellValue(62.0)
        }

        for (i in 0..2) sheet.autoSizeColumn(i)
        val fos = java.io.FileOutputStream(outputFile)
        workbook.write(fos)
        fos.close()
        workbook.close()
    }
}

fun main() {
    // Configure l'apparence selon le système d'exploitation
    try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) } catch (e: Exception) {}

    // "invokeLater" = lance le code graphique sur le bon thread (obligatoire avec Swing)
    SwingUtilities.invokeLater {

        val manager = GradeManager() // Crée une instance de notre classe

        // --- Fenêtre principale ---
        val frame = JFrame("🎓 Grade Calculator — Android App Development")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE // Ferme l'app au X
        frame.setSize(480, 340)
        frame.setLocationRelativeTo(null) // Centre la fenêtre
        frame.layout = FlowLayout(FlowLayout.CENTER, 12, 12) // Disposition en flux

        // --- Composants ---
        val label      = JLabel("Entrez une note (0-100) ou choisissez un fichier Excel :")
        val inputField = JTextField(12)  // Champ de saisie, largeur 12 caractères
        val btnCalc    = JButton("✅ Calculer une note")
        val btnExcel   = JButton("📂 Charger un fichier Excel")
        val btnTemplate = JButton("📋 Créer un modèle Excel")
        val resultLabel = JLabel(" ")    // Affiche le résultat d'une note unique

        resultLabel.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 14)

        // --- ACTION : Calculer une note individuelle ---
        btnCalc.addActionListener {
            val score = inputField.text.trim().toDoubleOrNull()
            // "toDoubleOrNull()" = convertit le texte en Double, null si impossible

            if (score == null || score < 0 || score > 100) {
                resultLabel.text = "⚠ Entrez un nombre valide entre 0 et 100"
                resultLabel.foreground = Color.RED
            } else {
                val (grade, remark) = manager.calculateGrade(score)
                // Destructuration : extrait grade ET remark de la Pair en une ligne
                resultLabel.text = "Résultat : $grade — $remark"
                resultLabel.foreground = manager.getGradeColor(grade).darker()
            }
        }

        inputField.addActionListener { btnCalc.doClick() }


        btnExcel.addActionListener {
            val chooser = JFileChooser()
            chooser.fileFilter = FileNameExtensionFilter("Fichiers Excel (*.xlsx)", "xlsx")
            chooser.dialogTitle = "Sélectionner le fichier Excel des étudiants"

            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                val students = manager.readExcel(file)

                if (students.isEmpty()) {
                    JOptionPane.showMessageDialog(frame,
                        "Aucune donnée trouvée.\nVérifiez que le fichier a 3 colonnes : Nom | Matière | Note",
                        "Fichier vide", JOptionPane.WARNING_MESSAGE)
                    return@addActionListener
                    // "return@addActionListener" = sort du lambda sans quitter main()
                }


                val options = arrayOf("💾 Excel (.xlsx)", "📄 PDF (.pdf)", "Les deux !")
                val choice = JOptionPane.showOptionDialog(frame,
                    "${students.size} étudiant(s) chargé(s).\nComment voulez-vous exporter ?",
                    "Export des résultats",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0])

                val savePath = file.parent // Dossier du fichier source

                if (choice == 0 || choice == 2) { // Excel ou Les deux
                    val excelFile = File("$savePath/Resultats_Grades.xlsx")
                    manager.exportToExcel(students, excelFile)
                }
                if (choice == 1 || choice == 2) { // PDF ou Les deux
                    manager.exportToPdf(students, "$savePath/Resultats_Grades.pdf")
                }

                if (choice >= 0) {
                    JOptionPane.showMessageDialog(frame,
                        "✅ Export réussi !\nFichier(s) sauvegardé(s) dans :\n$savePath",
                        "Succès", JOptionPane.INFORMATION_MESSAGE)
                }
            }
        }

        // --- ACTION : Créer un modèle Excel ---
        btnTemplate.addActionListener {
            val chooser = JFileChooser()
            chooser.selectedFile = File("modele_etudiants.xlsx")
            chooser.dialogTitle = "Enregistrer le modèle"

            if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                var dest = chooser.selectedFile
                if (!dest.name.endsWith(".xlsx")) dest = File(dest.path + ".xlsx")
                manager.createTemplate(dest)
                JOptionPane.showMessageDialog(frame,
                    "Modèle créé !\n${dest.absolutePath}\n\nRemplissez : Nom | Matière | Note",
                    "Modèle créé ✓", JOptionPane.INFORMATION_MESSAGE)
            }
        }

        // --- Ajout des éléments à la fenêtre ---
        frame.add(label)
        frame.add(inputField)
        frame.add(btnCalc)
        frame.add(resultLabel)
        frame.add(JSeparator(SwingConstants.HORIZONTAL)) // Ligne de séparation visuelle
        frame.add(btnExcel)
        frame.add(btnTemplate)

        frame.isVisible = true // Rend la fenêtre visible — DOIT être en dernier
    }
}

// ============================================================
// FICHIER : util/ExcelManager.kt
// RÔLE    : Lit un fichier Excel importé par l'utilisateur
//           et exporte les résultats en Excel ou PDF.
//           Utilise Apache POI pour Excel, iText pour PDF.
// ============================================================

package com.gradecalculator.util

import android.content.Context
// Context = l'environnement Android (accès aux fichiers, ressources...)

import android.net.Uri
// Uri = adresse d'un fichier sur Android (remplace java.io.File pour les fichiers utilisateur)

import com.gradecalculator.model.GradeStats
import com.gradecalculator.model.GradeSystem
import com.gradecalculator.model.Student

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue

import java.io.OutputStream

// ============================================================
// OBJECT : ExcelManager (Singleton utilitaire)
// ============================================================
// Regroupe toutes les fonctions liées aux fichiers (lecture, export).
// "object" garantit qu'il n'y a qu'une seule instance dans toute l'app.
object ExcelManager {

    // ---- Fonction avancée : readFromUri ----
    // Lit un fichier Excel depuis un Uri Android et retourne une liste de Student.
    //
    // Pourquoi Uri et non File ?
    // Sur Android, l'utilisateur choisit un fichier via le "File Picker".
    // Android donne une Uri (adresse sécurisée) au lieu d'un chemin direct.
    // On doit passer par context.contentResolver pour ouvrir ce fichier.
    //
    // "Result<List<Student>>" = soit un succès avec la liste, soit une erreur.
    // C'est plus sûr que de lever une exception directement.
    fun readFromUri(context: Context, uri: Uri): Result<List<Student>> {
        // "runCatching" exécute le bloc et capture toute erreur automatiquement
        return runCatching {
            // Ouvre un flux de lecture depuis l'Uri Android
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: error("Impossible d'ouvrir le fichier")
            // "error()" lance une IllegalStateException si inputStream est null

            val workbook = XSSFWorkbook(inputStream)  // Interprète le fichier comme Excel
            val sheet    = workbook.getSheetAt(0)     // 1ère feuille (index 0)

            val students = mutableListOf<Student>()   // Liste modifiable

            sheet.forEachIndexed { index, row ->
                if (index == 0) return@forEachIndexed  // Ignore la ligne d'en-tête

                // Fonction locale pour lire une cellule en toute sécurité
                fun safeCell(col: Int): String =
                    row.getCell(col)?.toString()?.trim() ?: ""
                // "?." = appel sûr : si null, ne plante pas → retourne null
                // "?: """ = si null, retourner ""

                val name  = safeCell(0)  // Colonne A : Nom
                val score = safeCell(2)  // Colonne C : Note (on ignore la matière, elle est fixe)

                if (name.isBlank()) return@forEachIndexed  // Ignore les lignes vides

                val scoreDouble = score.toDoubleOrNull()
                    ?: return@forEachIndexed  // Ignore si la note n'est pas un nombre

                students.add(Student(name = name, score = scoreDouble))
            }

            workbook.close()
            inputStream.close()
            students  // Valeur de retour du bloc runCatching → le Result contiendra cette liste
        }
    }

    // ---- Fonction avancée : exportToExcel ----
    // Écrit les résultats dans un OutputStream (flux de sortie vers un fichier).
    // Android nous donne un OutputStream via le "File Picker" en mode écriture.
    fun exportToExcel(students: List<Student>, outputStream: OutputStream) {
        val workbook = XSSFWorkbook()
        val sheet    = workbook.createSheet("Résultats")

        // Style pour l'en-tête : fond sombre, texte blanc et gras
        val headerStyle = workbook.createCellStyle().apply {
            // "apply" = exécute un bloc sur l'objet et le retourne
            // C'est une façon concise de configurer un objet
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern         = FillPatternType.SOLID_FOREGROUND
            alignment           = HorizontalAlignment.CENTER
            val font = workbook.createFont().apply {
                bold       = true
                color      = IndexedColors.WHITE.index
                fontName   = "Arial"
                fontHeightInPoints = 11
            }
            setFont(font)
        }

        // Style pour lignes paires (alternance de couleur)
        val altStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_TURQUOISE.index
            fillPattern         = FillPatternType.SOLID_FOREGROUND
            alignment           = HorizontalAlignment.CENTER
        }

        val normalStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
        }

        // Fonction locale d'extension sur Row pour créer une cellule
        // "fun Row.addCell(...)" = ajoute une méthode à la classe Row (Extension Function)
        fun Row.addCell(col: Int, value: String, style: CellStyle) {
            createCell(col).apply {
                setCellValue(value)
                cellStyle = style
            }
        }

        // Ligne d'en-tête (index 0)
        sheet.createRow(0).apply {
            addCell(0, "Nom",     headerStyle)
            addCell(1, "Matière", headerStyle)
            addCell(2, "Note",    headerStyle)
            addCell(3, "Grade",   headerStyle)
            addCell(4, "Mention", headerStyle)
        }

        // Lignes de données
        students.forEachIndexed { i, s ->
            val style = if (i % 2 == 0) altStyle else normalStyle
            // Opérateur ternaire Kotlin : if/else est une expression
            sheet.createRow(i + 1).apply {
                addCell(0, s.name,             style)
                addCell(1, s.subject,          style)
                addCell(2, s.score.toString(), style)
                addCell(3, s.grade,            style)
                addCell(4, s.gradeLabel,       style)
            }
        }

        for (i in 0..4) sheet.autoSizeColumn(i)  // Largeur auto pour chaque colonne

        workbook.write(outputStream)  // Écrit dans le flux de sortie Android
        workbook.close()
        outputStream.close()
    }

    // ---- Fonction avancée : exportToPdf ----
    // Génère un PDF avec les résultats et l'écrit dans l'OutputStream.
    fun exportToPdf(students: List<Student>, stats: GradeStats, outputStream: OutputStream) {
        val writer   = PdfWriter(outputStream)
        val pdf      = PdfDocument(writer)
        val document = Document(pdf)

        // Titre principal
        document.add(
            Paragraph("Résultats — Android App Development")
                .setFontSize(20f).setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8f)
        )

        // Sous-titre avec statistiques
        document.add(
            Paragraph(
                "Total : ${stats.total} étudiant(s)   |   " +
                        "Moyenne : ${"%.1f".format(stats.average)}   |   " +
                        "Admis : ${stats.passing}   |   Échec : ${stats.failing}"
            )
                .setFontSize(11f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(20f)
        )

        // Tableau à 5 colonnes en pourcentages de la largeur
        val table = Table(UnitValue.createPercentArray(floatArrayOf(22f, 26f, 12f, 12f, 28f)))
            .useAllAvailableWidth()

        // Fonction locale pour les cellules d'en-tête
        fun Table.headerCell(text: String) = addHeaderCell(
            Cell().add(Paragraph(text).setBold().setFontSize(10f))
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(com.itextpdf.kernel.colors.DeviceRgb(31, 64, 121))
                .setFontColor(ColorConstants.WHITE)
                .setPadding(8f)
        )

        // Fonction locale pour les cellules normales
        fun Table.dataCell(text: String, bold: Boolean = false) = addCell(
            Cell().add(
                Paragraph(text).setFontSize(10f).also { if (bold) it.setBold() }
            )
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6f)
        )

        // En-têtes
        table.headerCell("Nom")
        table.headerCell("Matière")
        table.headerCell("Note")
        table.headerCell("Grade")
        table.headerCell("Mention")

        // Données
        students.forEach { s ->
            table.dataCell(s.name)
            table.dataCell(s.subject)
            table.dataCell(s.score.toInt().toString())
            table.dataCell(s.grade, bold = true)   // Grade en gras
            table.dataCell(s.gradeLabel)
        }

        document.add(table)
        document.close()
    }
}

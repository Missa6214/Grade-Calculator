// ============================================================
// FICHIER : ui/components/Components.kt
// RÔLE    : Composants Compose réutilisables dans toute l'app.
//           Chaque @Composable = un "bloc de construction" de l'UI.
// ============================================================

package com.gradecalculator.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gradecalculator.model.GradeStats
import com.gradecalculator.model.Student
import com.gradecalculator.ui.theme.*

// ============================================================
// COMPOSABLE : GradeBadge
// Affiche la lettre du grade dans un badge coloré.
// ============================================================
@Composable
fun GradeBadge(
    grade: String,          // La lettre du grade (A, B+, F...)
    size: Int = 1           // 1 = petit, 2 = grand
) {
    val color = gradeColor(grade)   // Couleur déduite du grade (depuis Theme.kt)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(if (size == 2) 16.dp else 8.dp))
            // RoundedCornerShape = coins arrondis. La valeur en dp contrôle le rayon.
            .background(color.copy(alpha = 0.15f))
            // copy(alpha = ...) = même couleur mais semi-transparente
            .border(
                width = if (size == 2) 2.dp else 1.dp,
                color = color.copy(alpha = 0.6f),
                shape = RoundedCornerShape(if (size == 2) 16.dp else 8.dp)
            )
            .padding(
                horizontal = if (size == 2) 20.dp else 10.dp,
                vertical   = if (size == 2) 10.dp else 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = grade,
            color      = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize   = if (size == 2) 28.sp else 13.sp
        )
    }
}

// ============================================================
// COMPOSABLE : SingleResultCard
// Carte affichant le résultat d'une note unique calculée.
// ============================================================
@Composable
fun SingleResultCard(student: Student) {
    val color = gradeColor(student.grade)

    // "AnimatedVisibility" fait apparaître la carte avec une animation fluide
    AnimatedVisibility(
        visible = true,
        enter   = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
        // fadeIn = apparition progressive
        // slideInVertically = glisse depuis le bas
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
            border   = BorderStroke(1.dp, color.copy(alpha = 0.4f))
            // BorderStroke = bordure colorée selon le grade
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Colonne gauche : note affichée
                Column {
                    Text(
                        "Note entrée",
                        color    = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${student.score.toInt()} / 100",
                        color      = OnSurface,
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Colonne droite : badge de grade + description
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GradeBadge(grade = student.grade, size = 2)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        student.gradeLabel,
                        color      = color,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ============================================================
// COMPOSABLE : GradeReferenceTable
// Affiche le tableau de référence des grades.
// ============================================================
@Composable
fun GradeReferenceTable() {
    // Liste de triples (plage, grade, description) — données fixes
    // "Triple" = groupe de 3 valeurs (comme Pair mais avec 3)
    val rows = listOf(
        Triple("90 - 100", "A+", "Excellent"),
        Triple("80 - 89",  "A",  "Très bien"),
        Triple("75 - 79",  "B+", "Bien +"),
        Triple("70 - 74",  "B",  "Bien"),
        Triple("65 - 69",  "C+", "Assez bien +"),
        Triple("60 - 64",  "C",  "Assez bien"),
        Triple("55 - 59",  "D+", "Passable +"),
        Triple("50 - 54",  "D",  "Passable"),
        Triple("0  - 49",  "F",  "Échec"),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Tableau des grades",
                color      = TextSecondary,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            // "forEach" parcourt chaque élément de la liste
            // "(range, grade, label)" = destructuring du Triple
            rows.forEach { (range, grade, label) ->
                val c = gradeColor(grade)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.copy(alpha = 0.07f))   // Fond légèrement coloré
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(range, color = TextSecondary, fontSize = 12.sp,
                        modifier = Modifier.width(64.dp))
                    Text(grade, color = c, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp))
                    Text(label, color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ============================================================
// COMPOSABLE : StatsRow
// Affiche les 4 cartes de statistiques.
// ============================================================
@Composable
fun StatsRow(stats: GradeStats) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Fonction locale @Composable : une carte statistique
        // "@Composable" peut être déclaré localement dans un autre @Composable
        @Composable
        fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
            Card(
                modifier = modifier,
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
                border   = BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(label, color = TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        StatCard("Total",   "${stats.total}",              PrimaryPurple, Modifier.weight(1f))
        StatCard("Moyenne", "${"%.1f".format(stats.average)}", GradeA,    Modifier.weight(1f))
        StatCard("Admis",   "${stats.passing}",            GradeB,        Modifier.weight(1f))
        StatCard("Échec",   "${stats.failing}",            GradeF,        Modifier.weight(1f))
    }
}

// ============================================================
// COMPOSABLE : StudentListItem
// Une ligne de la liste des étudiants importés.
// ============================================================
@Composable
fun StudentListItem(student: Student, index: Int) {
    val bg = if (index % 2 == 0) SurfaceDark else SurfaceMedium  // Alternance couleur

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Numéro de ligne
        Text(
            "${index + 1}",
            color    = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(24.dp)
        )
        // Nom
        Text(
            student.name,
            color    = OnSurface,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
            // maxLines = 1 → coupe le texte s'il est trop long (pas de retour à la ligne)
        )
        // Note
        Text(
            "${student.score.toInt()}",
            color     = OnSurface,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(40.dp)
        )
        // Badge de grade
        GradeBadge(grade = student.grade)
        Spacer(Modifier.width(8.dp))
        // Description du grade
        Text(
            student.gradeLabel,
            color    = gradeColor(student.grade),
            fontSize = 12.sp,
            modifier = Modifier.width(100.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================
// COMPOSABLE : StatusSnackbar
// Barre de notification en bas de l'écran.
// ============================================================
@Composable
fun StatusSnackbar(message: String, onDismiss: () -> Unit) {
    // "AnimatedVisibility" gère l'animation d'apparition/disparition
    AnimatedVisibility(
        visible = message.isNotEmpty(),
        enter   = slideInVertically { it } + fadeIn(),
        exit    = slideOutVertically { it } + fadeOut()
    ) {
        val isSuccess = message.startsWith("✓")
        val color     = if (isSuccess) GradeA else GradeF

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onDismiss() },   // Cliquer sur la barre la fait disparaître
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMedium),
            border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
        ) {
            Row(
                modifier              = Modifier.padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    message,
                    color    = color,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("✕", color = TextSecondary, fontSize = 14.sp)
            }
        }
    }
}

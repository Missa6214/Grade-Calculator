// ============================================================
// FICHIER : MainActivity.kt
// RÔLE    : Point d'entrée de l'application Android.
//           Lance l'interface Compose et gère les File Pickers.
// ============================================================

package com.gradecalculator

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
// ComponentActivity = classe de base moderne pour les activités Android

import androidx.activity.compose.rememberLauncherForActivityResult
// rememberLauncherForActivityResult = lance une autre activité (ex: file picker)
// et récupère son résultat

import androidx.activity.compose.setContent
// setContent = définit l'interface de l'activité avec du code Compose

import androidx.activity.result.contract.ActivityResultContracts
// ActivityResultContracts = contrats prédéfinis pour des actions courantes
// (ouvrir fichier, prendre photo, demander permission...)

import androidx.activity.viewModels
// viewModels() = récupère ou crée le ViewModel lié à cette activité

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
// LocalContext = accès au Context Android depuis un Composable

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// collectAsStateWithLifecycle = observe un StateFlow et recompose l'UI
// quand la valeur change, uniquement quand l'UI est visible (lifecycle-aware)

import com.gradecalculator.ui.components.*
import com.gradecalculator.ui.theme.*
import com.gradecalculator.viewmodel.MainViewModel

// ============================================================
// CLASSE : MainActivity
// ============================================================
// "ComponentActivity()" = classe de base Android pour les activités Compose.
// Une "Activity" = un écran de l'application Android.
class MainActivity : ComponentActivity() {

    // "by viewModels()" = délégation Kotlin : crée ou récupère le ViewModel
    // automatiquement. Le ViewModel survit aux rotations d'écran.
    private val viewModel: MainViewModel by viewModels()

    // "onCreate" est appelé quand l'activité est créée (démarrage de l'app)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)   // Toujours appeler la classe parente

        // "setContent" remplace le XML de layout traditionnel par du code Compose
        setContent {
            GradeCalculatorTheme {   // Applique notre thème (couleurs, typo)
                GradeCalculatorApp(viewModel = viewModel)
            }
        }
    }
}

// ============================================================
// COMPOSABLE RACINE : GradeCalculatorApp
// ============================================================
// Ce composable orchestre toute l'interface de l'application.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeCalculatorApp(viewModel: MainViewModel) {
    val context = LocalContext.current   // Accès au Context Android
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // "by" = délégation : uiState est automatiquement mis à jour
    // "collectAsStateWithLifecycle" transforme le StateFlow en State Compose

    // ---- File Pickers (lanceurs de sélection de fichiers) ----

    // Lanceur pour OUVRIR un fichier Excel
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
        // OpenDocument = contrat pour choisir un fichier existant
    ) { uri: Uri? ->
        // "uri" est nullable (null si l'utilisateur annule)
        uri?.let { viewModel.importFromExcel(context, it) }
        // "let" = exécute le bloc seulement si uri != null
    }

    // Lanceur pour ENREGISTRER un fichier Excel
    val exportExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        // CreateDocument = contrat pour créer un nouveau fichier
    ) { uri: Uri? ->
        uri?.let { viewModel.exportExcel(context, it) }
    }

    // Lanceur pour ENREGISTRER un PDF
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportPdf(context, it) }
    }

    // ---- Structure principale : Scaffold ----
    // Scaffold = structure Material3 avec TopBar, BottomBar, FAB, Snackbar...
    Scaffold(
        containerColor = BackgroundDark,   // Couleur de fond global

        // Barre du haut
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Grade Calculator",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = OnSurface
                        )
                        Text(
                            "Android App Development",
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        }
    ) { paddingValues ->
        // "paddingValues" = marges automatiques pour éviter la TopBar et la NavBar

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ---- Onglets (TabRow) ----
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor   = SurfaceDark,
                    contentColor     = PrimaryPurple,
                    indicator = { tabPositions ->
                        // Indicateur personnalisé sous l'onglet actif
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                            color    = PrimaryPurple
                        )
                    }
                ) {
                    // Onglet 0 : Note unique
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick  = { viewModel.selectTab(0) },
                        text     = { Text("Note unique", fontSize = 13.sp) },
                        icon     = { Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    // Onglet 1 : Fichier Excel
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick  = { viewModel.selectTab(1) },
                        text     = { Text("Fichier Excel", fontSize = 13.sp) },
                        icon     = { Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // ---- Contenu selon l'onglet actif ----
                // "AnimatedContent" anime la transition entre les deux onglets
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = {
                        // Glissement horizontal selon la direction
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    modifier = Modifier.weight(1f)  // Prend tout l'espace restant
                ) { tab ->
                    when (tab) {
                        0 -> SingleGradeScreen(viewModel = viewModel, uiState = uiState)
                        1 -> ExcelScreen(
                            viewModel         = viewModel,
                            uiState           = uiState,
                            onImport          = { importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
                            onExportExcel     = { exportExcelLauncher.launch("resultats_grades.xlsx") },
                            onExportPdf       = { exportPdfLauncher.launch("resultats_grades.pdf") }
                        )
                    }
                }
            }

            // ---- Snackbar de statut (superposée, en bas) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                StatusSnackbar(
                    message   = uiState.statusMsg,
                    onDismiss = { viewModel.clearStatus() }
                )
            }
        }
    }
}

// ============================================================
// COMPOSABLE : SingleGradeScreen (Onglet "Note unique")
// ============================================================
@Composable
fun SingleGradeScreen(viewModel: MainViewModel, uiState: com.gradecalculator.viewmodel.UiState) {
    LazyColumn(
        // LazyColumn = liste défilable performante (ne rend que les éléments visibles)
        modifier              = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp),
        contentPadding        = PaddingValues(bottom = 80.dp)
        // PaddingValues(bottom) = espace en bas pour ne pas masquer le contenu
    ) {
        item {
            // ---- Carte de saisie ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Entrez une note",
                        color      = OnSurface,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "La note doit être comprise entre 0 et 100.",
                        color    = TextSecondary,
                        fontSize = 13.sp
                    )

                    // ---- Champ de saisie Material 3 ----
                    OutlinedTextField(
                        value         = uiState.singleScore,
                        onValueChange = { viewModel.onScoreInputChange(it) },
                        // "it" = la nouvelle valeur saisie par l'utilisateur
                        label         = { Text("Note (0 - 100)") },
                        placeholder   = { Text("Ex: 75") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        // keyboardOptions = ouvre le clavier numérique sur mobile
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        isError = uiState.singleError.isNotEmpty(),
                        // isError = marque le champ en rouge si erreur
                        supportingText = {
                            if (uiState.singleError.isNotEmpty()) {
                                Text(uiState.singleError, color = GradeF)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = PrimaryPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor    = PrimaryPurple,
                            cursorColor          = PrimaryPurple
                        )
                    )

                    // ---- Bouton Calculer ----
                    Button(
                        onClick  = { viewModel.calculateSingleGrade() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(Icons.Filled.Calculate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Calculer le Grade",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Résultat (visible seulement si calculé)
        item {
            AnimatedVisibility(
                visible = uiState.singleResult != null,
                enter   = fadeIn(tween(400)) + expandVertically(tween(400))
            ) {
                uiState.singleResult?.let { SingleResultCard(student = it) }
                // "let" = exécute le bloc seulement si singleResult != null
            }
        }

        // Tableau de référence des grades
        item { GradeReferenceTable() }
    }
}

// ============================================================
// COMPOSABLE : ExcelScreen (Onglet "Fichier Excel")
// ============================================================
@Composable
fun ExcelScreen(
    viewModel     : MainViewModel,
    uiState       : com.gradecalculator.viewmodel.UiState,
    onImport      : () -> Unit,   // Lambda : action à faire au clic sur "Importer"
    onExportExcel : () -> Unit,
    onExportPdf   : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ---- Carte des boutons d'action ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Traitement par fichier Excel",
                    color      = OnSurface,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Format attendu : Colonne A = Nom  |  Colonne B = Matière  |  Colonne C = Note",
                    color    = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Bouton Importer
                Button(
                    onClick  = onImport,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Importer un fichier Excel", fontWeight = FontWeight.SemiBold)
                }

                // Boutons d'export (actifs seulement si des données sont chargées)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Bouton Export Excel
                    OutlinedButton(
                        onClick  = onExportExcel,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape    = RoundedCornerShape(12.dp),
                        enabled  = uiState.students.isNotEmpty(),
                        // enabled = false → bouton grisé si pas de données
                        border   = BorderStroke(1.dp, if (uiState.students.isNotEmpty()) Color(0xFF2E7D32) else BorderColor),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF66BB6A))
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Excel", fontSize = 13.sp)
                    }

                    // Bouton Export PDF
                    OutlinedButton(
                        onClick  = onExportPdf,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape    = RoundedCornerShape(12.dp),
                        enabled  = uiState.students.isNotEmpty(),
                        border   = BorderStroke(1.dp, if (uiState.students.isNotEmpty()) Color(0xFFC62828) else BorderColor),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF9A9A))
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF", fontSize = 13.sp)
                    }
                }
            }
        }

        // ---- Indicateur de chargement ----
        AnimatedVisibility(visible = uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color    = PrimaryPurple
            )
        }

        // ---- Statistiques ----
        AnimatedVisibility(visible = uiState.students.isNotEmpty()) {
            StatsRow(stats = uiState.stats)
        }

        // ---- Liste des étudiants ----
        if (uiState.students.isEmpty()) {
            // État vide
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.TableChart,
                        contentDescription = null,
                        tint     = BorderColor,
                        modifier = Modifier.size(56.dp)
                    )
                    Text("Aucun étudiant importé", color = TextSecondary, fontSize = 16.sp)
                    Text(
                        "Importez un fichier Excel pour afficher les résultats.",
                        color     = TextSecondary.copy(alpha = 0.6f),
                        fontSize  = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // En-tête du tableau
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(PrimaryPurple.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#",         color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                Text("Nom",       color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Note",      color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                Text("Grade",     color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                Text("Mention",   color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(100.dp))
            }

            // LazyColumn = liste virtualisée : très performante pour de nombreux éléments.
            // Contrairement à Column, elle ne rend que les éléments VISIBLES à l'écran.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            ) {
                // "itemsIndexed" = parcourt la liste avec l'index (numéro de ligne)
                itemsIndexed(uiState.students) { index, student ->
                    StudentListItem(student = student, index = index)
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

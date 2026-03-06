// ============================================================
// FICHIER : viewmodel/MainViewModel.kt
// RÔLE    : Gère l'état de l'application et la logique.
//           Le ViewModel survit aux rotations d'écran.
//           Il fait le lien entre les données (model) et l'UI (Compose).
// ============================================================

package com.gradecalculator.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
// ViewModel = classe Android qui survit aux changements de configuration
// (ex: rotation d'écran). Les données ne sont pas perdues.

import androidx.lifecycle.viewModelScope
// viewModelScope = contexte de coroutine lié au ViewModel

import com.gradecalculator.model.GradeStats
import com.gradecalculator.model.GradeSystem
import com.gradecalculator.model.Student
import com.gradecalculator.util.ExcelManager

import kotlinx.coroutines.Dispatchers
// Dispatchers = sur quel "thread" (fil d'exécution) faire le travail
// Dispatchers.IO = thread pour les entrées/sorties (fichiers, réseau)
// Dispatchers.Main = thread principal (interface graphique)

import kotlinx.coroutines.flow.MutableStateFlow
// MutableStateFlow = un conteneur réactif : Compose écoute ses changements
// et redessine l'UI automatiquement quand la valeur change

import kotlinx.coroutines.flow.StateFlow
// StateFlow = version en lecture seule de MutableStateFlow (pour l'UI)

import kotlinx.coroutines.flow.asStateFlow
// asStateFlow() = convertit un MutableStateFlow en StateFlow (lecture seule)

import kotlinx.coroutines.launch
// launch = démarre une coroutine (opération asynchrone non bloquante)

import kotlinx.coroutines.withContext
// withContext = change le thread dans une coroutine

// ============================================================
// DATA CLASS : UiState (État global de l'interface)
// ============================================================
// Toute l'interface est décrite par cet unique objet.
// Quand il change → Compose redessine automatiquement l'UI.
data class UiState(
    val singleScore  : String        = "",       // Texte dans le champ de note unique
    val singleResult : Student?      = null,     // Résultat calculé (null si rien encore)
    val singleError  : String        = "",       // Message d'erreur note unique

    val students     : List<Student> = emptyList(), // Liste des étudiants du fichier Excel
    val stats        : GradeStats    = GradeStats(), // Statistiques calculées

    val isLoading    : Boolean       = false,    // Vrai pendant le chargement d'un fichier
    val statusMsg    : String        = "",       // Message de statut (succès/erreur import/export)

    val selectedTab  : Int           = 0         // Onglet actif : 0 = Note unique, 1 = Fichier
)

// ============================================================
// CLASS : MainViewModel
// ============================================================
// Hérite de ViewModel (":") pour bénéficier de la gestion du cycle de vie Android.
class MainViewModel : ViewModel() {

    // _uiState = privé, modifiable seulement dans le ViewModel
    // MutableStateFlow avec valeur initiale = UiState() (état par défaut)
    private val _uiState = MutableStateFlow(UiState())

    // uiState = public, en lecture seule pour l'interface Compose
    // L'UI ne peut que LIRE l'état, jamais le modifier directement
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ---- Fonction : mettre à jour le champ de note unique ----
    // Appelée à chaque frappe de l'utilisateur dans le champ
    fun onScoreInputChange(value: String) {
        // "copy()" crée une copie de UiState avec seulement les champs modifiés
        // C'est un pattern courant avec les data class en Kotlin
        _uiState.value = _uiState.value.copy(
            singleScore  = value,
            singleError  = "",      // Efface l'erreur à chaque frappe
            singleResult = null     // Efface le résultat précédent
        )
    }

    // ---- Fonction : calculer le grade d'une note unique ----
    fun calculateSingleGrade() {
        val scoreText = _uiState.value.singleScore.trim()
        val score     = scoreText.toDoubleOrNull()

        if (score == null || score < 0 || score > 100) {
            // Mise à jour de l'état avec un message d'erreur
            _uiState.value = _uiState.value.copy(
                singleError  = "Entrez un nombre valide entre 0 et 100",
                singleResult = null
            )
            return   // Arrête la fonction ici
        }

        // Crée un objet Student et met à jour l'état avec le résultat
        val student = Student(name = "—", score = score)
        _uiState.value = _uiState.value.copy(
            singleResult = student,
            singleError  = ""
        )
    }

    // ---- Fonction : changer d'onglet ----
    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    // ---- Fonction avancée : importer depuis Excel ----
    // "suspend" = cette fonction peut être suspendue (attendre) sans bloquer l'UI.
    // Ici on utilise "launch" depuis le ViewModel pour éviter le "suspend" public.
    fun importFromExcel(context: Context, uri: Uri) {
        // "viewModelScope.launch" = démarre une coroutine dans le scope du ViewModel
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMsg = "")

            // "withContext(Dispatchers.IO)" = exécute ce bloc sur le thread IO
            // La lecture de fichier est une opération longue → ne pas bloquer l'UI
            val result = withContext(Dispatchers.IO) {
                ExcelManager.readFromUri(context, uri)
                // Retourne un Result<List<Student>>
            }

            // "fold" sur un Result : exécute onSuccess ou onFailure selon le résultat
            result.fold(
                onSuccess = { students ->
                    val stats = GradeSystem.statistics(students)
                    _uiState.value = _uiState.value.copy(
                        students  = students,
                        stats     = stats,
                        isLoading = false,
                        statusMsg = "✓ ${students.size} étudiant(s) importé(s) avec succès",
                        selectedTab = 1   // Basculer vers l'onglet "Fichier"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMsg = "Erreur : ${error.message}"
                    )
                }
            )
        }
    }

    // ---- Fonction avancée : exporter vers Excel ----
    fun exportExcel(context: Context, uri: Uri) {
        viewModelScope.launch {
            val students = _uiState.value.students
            if (students.isEmpty()) {
                _uiState.value = _uiState.value.copy(statusMsg = "Aucune donnée à exporter")
                return@launch   // "return@launch" = sortir de la coroutine
            }
            withContext(Dispatchers.IO) {
                // openOutputStream : Android ouvre un flux d'écriture vers l'Uri choisi
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: return@withContext
                ExcelManager.exportToExcel(students, stream)
            }
            _uiState.value = _uiState.value.copy(statusMsg = "✓ Fichier Excel exporté avec succès")
        }
    }

    // ---- Fonction avancée : exporter vers PDF ----
    fun exportPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            val students = _uiState.value.students
            val stats    = _uiState.value.stats
            if (students.isEmpty()) {
                _uiState.value = _uiState.value.copy(statusMsg = "Aucune donnée à exporter")
                return@launch
            }
            withContext(Dispatchers.IO) {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: return@withContext
                ExcelManager.exportToPdf(students, stats, stream)
            }
            _uiState.value = _uiState.value.copy(statusMsg = "✓ Fichier PDF exporté avec succès")
        }
    }

    // ---- Fonction utilitaire : effacer le message de statut ----
    fun clearStatus() {
        _uiState.value = _uiState.value.copy(statusMsg = "")
    }
}

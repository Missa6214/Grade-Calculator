// ============================================================
// FICHIER : model/Student.kt
// RÔLE    : Contient la classe Student et la logique de calcul
//           de grade. C'est le "cerveau" de l'application.
// ============================================================

package com.gradecalculator.model
// "package" = le dossier logique où vit ce fichier.
// Android l'utilise pour organiser le code.

// ============================================================
// DATA CLASS : Student (Notion de Classe et d'Objet)
// ============================================================
// Une "data class" est une classe spécialisée pour stocker des données.
// Kotlin génère automatiquement pour nous :
//   - equals()    → pour comparer deux étudiants
//   - toString()  → pour afficher un étudiant en texte
//   - copy()      → pour copier un étudiant en modifiant certains champs
//
// Exemple de création d'un OBJET depuis cette classe :
//   val etudiant = Student("Alice", 85.0)
//   → etudiant.name  == "Alice"
//   → etudiant.grade == "A"  (calculé automatiquement)
data class Student(
    val name: String,        // "val" = valeur immuable (ne peut pas changer)
    val score: Double        // La note (nombre à virgule)
) {
    // Ces propriétés sont calculées automatiquement dès la création de l'objet
    // "get()" = propriété calculée (comme une fonction sans parenthèses)
    val subject: String get() = "Android App Development"
    // La matière est fixe pour tous les étudiants

    val grade: String get() = GradeSystem.calculateGrade(score).first
    // ".first" récupère le 1er élément du Pair (la lettre du grade)

    val gradeLabel: String get() = GradeSystem.calculateGrade(score).second
    // ".second" récupère le 2ème élément du Pair (la description)
}

// ============================================================
// OBJECT : GradeSystem (Notion de Singleton / Objet Kotlin)
// ============================================================
// "object" en Kotlin = une classe avec UNE SEULE instance dans toute l'app.
// On ne peut pas faire "GradeSystem()" → il existe déjà automatiquement.
// Parfait pour regrouper des fonctions utilitaires.
object GradeSystem {

    // ---- Fonction basique : calculateGrade ----
    // Prend une note (Double) et retourne un Pair<String, String>
    // Pair = un couple de deux valeurs regroupées : (grade, description)
    fun calculateGrade(score: Double): Pair<String, String> {
        // "return when" = équivalent du switch/case en Kotlin
        // Teste les conditions dans l'ordre, s'arrête à la première vraie
        return when {
            score >= 90 -> Pair("A+", "Excellent")
            score >= 80 -> Pair("A",  "Très bien")
            score >= 75 -> Pair("B+", "Bien +")
            score >= 70 -> Pair("B",  "Bien")
            score >= 65 -> Pair("C+", "Assez bien +")
            score >= 60 -> Pair("C",  "Assez bien")
            score >= 55 -> Pair("D+", "Passable +")
            score >= 50 -> Pair("D",  "Passable")
            else        -> Pair("F",  "Échec")       // Tous les autres cas
        }
    }

    // ---- Fonction avancée : processStudents ----
    // Prend une liste de paires (nom, note) et retourne une liste de Student
    // List<Pair<String, Double>> = liste de couples (nom, note)
    // List<Student> = liste d'objets Student
    fun processStudents(data: List<Pair<String, Double>>): List<Student> {
        // "map" transforme chaque élément de la liste
        // Pour chaque couple (name, score), on crée un objet Student
        return data.map { (name, score) ->
            // "(name, score)" = destructuring : on extrait les valeurs du Pair
            Student(name = name, score = score)
        }
    }

    // ---- Fonction avancée : statistics ----
    // Calcule des statistiques sur une liste d'étudiants
    // Retourne un objet anonyme (on utilise une data class ici)
    fun statistics(students: List<Student>): GradeStats {
        if (students.isEmpty()) return GradeStats()   // Cas liste vide : stats vides

        val scores  = students.map { it.score }       // Extrait toutes les notes
        val passing = students.count { it.score >= 50 } // Compte ceux qui ont >= 50

        return GradeStats(
            total   = students.size,
            average = scores.average(),               // Moyenne automatique
            highest = scores.max(),                   // Note la plus haute
            lowest  = scores.min(),                   // Note la plus basse
            passing = passing,
            failing = students.size - passing
        )
    }
}

// ---- Data class pour les statistiques ----
// Valeurs par défaut (= 0, = 0.0) → permet de créer GradeStats() sans arguments
data class GradeStats(
    val total   : Int    = 0,
    val average : Double = 0.0,
    val highest : Double = 0.0,
    val lowest  : Double = 0.0,
    val passing : Int    = 0,
    val failing : Int    = 0
)

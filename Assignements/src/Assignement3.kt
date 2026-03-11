// 1. Définition de l'interface
interface Drawable {
    fun draw()
}

// 2. Implémentation pour le Cercle
class Circle(val radius: Int) : Drawable {
    override fun draw() {
        println("Dessin d'un Cercle (Rayon: $radius) :")
        println("   *** ")
        println(" * * ")
        println("* *")
        println(" * * ")
        println("   *** ")
    }
}

class Square(val sideLength: Int) : Drawable {
    override fun draw() {
        println("Dessin d'un Carré (Côté: $sideLength) :")
        println("*******")
        println("* *")
        println("* *")
        println("*******")
    }
}

fun main() {

    val shapes: List<Drawable> = listOf(
        Circle(5),
        Square(10),
        Circle(2)
    )

    shapes.forEach { it.draw() }
}
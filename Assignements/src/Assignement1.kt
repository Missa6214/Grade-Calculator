
abstract class Animal(val name: String) {

    abstract val legs: Int

    abstract fun makeSound(): String
}


class Dog(name: String) : Animal(name) {
    override val legs = 4

    override fun makeSound(): String {
        return "Woof!"
    }
}

class Cat(name: String) : Animal(name) {
    override val legs = 4

    override fun makeSound(): String {
        return "Meow!"
    }
}

fun main() {
    val myZoo: List<Animal> = listOf(
        Dog("Buddy"),
        Cat("Whiskers")
    )

    for (animal in myZoo) {
        println("${animal.name} says ${animal.makeSound()}")
    }
}
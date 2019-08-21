import com.gumbocoin.command
import java.io.File
import java.util.*

fun main() {
    val fileA = File("tmp/a")
    val fileB = File("tmp/b")

    val a = """
fun helloWorld(){
    delete this line
    println("Hello World")
    and this line
}
    """.trimIndent()

    val b = """
fun helloWorld(){
    println("Hello, World!")
}
    """.trimIndent()

    fileA.writeText(a)
    fileB.writeText(b)


    val str = /*kit*/"-v diff tmp/a tmp/b"

    command.parse(str).execute()
}

/*

// a
fun helloWorld(){
    if(true) {
        println("Hello World")
    }
}
// b
fun helloWorld(){
    println("Hello World")
}



 */
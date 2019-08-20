package com.gumbocoin.arguments



class BooleanValue(val name :String,val value :Boolean)
class StringValue(val name: String,val value: String)

sealed class Flag(val short: Char?,val long: String, val help: String)
class BooleanFlag(short: Char?,long: String,help: String):Flag(short,long, help)
class ValueFlag(short: Char?,long: String,help: String, val argumentName: String):Flag(short,long, help)

class FloatingValue(val name :String, val help :String)

fun command(block: CommandBuilder.() -> Unit):Command{
    return CommandBuilder().apply(block).build()
}



class CommandBuilder{

    private val names = mutableListOf<String>()
    var name :String
        get() = throw IllegalAccessError()
        set(value){ names.add(value) }

    private val commands = mutableListOf<Command>()

    fun subCommand(block : CommandBuilder.() -> Unit){
        commands += CommandBuilder().apply(block).build()
    }

    var subCommand : Command
        get() = error("no")
        set(value) {
            commands.add(value)
        }


    private val booleanFlags = mutableListOf<BooleanFlag>()
    fun flag(short: Char? = null,long: String,help: String){
        booleanFlags.add(BooleanFlag(short, long, help))
    }

    private val values = mutableListOf<ValueFlag>()
    fun value(short: Char? = null,long: String,help: String,argumentName: String){
        values.add(ValueFlag(short, long, help, argumentName))
    }

    private var runner :(ParsedValues.() -> Unit)? = null
    fun runner(block: ParsedValues.() -> Unit){
        runner = block
    }

    private var subrunner: (ParsedValues.() -> Unit)? = null
    fun subrunner(block: ParsedValues.() -> Unit){
        subrunner = block
    }

    private val floatings = mutableListOf<FloatingValue>()
    fun floating(name: String,help :String){
        floatings.add(FloatingValue(name, help))
    }

    var intro: String = ""
    var outro: String = ""
    var desc: String = ""
    internal fun build(): Command {

        if (commands.size > 0 && floatings.size > 0){
            error("can't have both floating values and subcommands")
        }

        val c = Command(
            names = names,
            intro = intro,
            outro = outro,
            desc = desc,
            subcommands = commands,
            flags = booleanFlags,
            values = values,
            floatingNames = floatings,
            runner = runner!!,
            subrunner = subrunner
        )
        for(sub in commands){
            sub.parent = c
        }
        return c
    }
}
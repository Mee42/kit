package com.gumbocoin.arguments

import com.gumbocoin.logging.Logger
import java.lang.RuntimeException
import java.lang.StringBuilder
import kotlin.system.exitProcess


val logger = Logger("command tree")

class Command(val names :List<String>,
              val intro :String,
              val outro :String,
              val desc: String,
              private val subcommands :List<Command>,
              val flags :List<BooleanFlag>,
              val values :List<ValueFlag>,
              val floatingNames: List<FloatingValue>,
              val runner :ParsedValues.() -> Unit,
              val subrunner :(ParsedValues.() -> Unit)?// this runs before subcommands and this run
){
    var parent :Command? = null


    fun parse(str :String) = parse(str.split(" ").toTypedArray())

    fun parse(args :Array<String>): ParsedValues{
        try {
            return parseInternal(args.asList())
        } catch (e :ShowHelpException){
            //show help for that command
            println(e.helpMessage)
            exitProcess(0)
        }
    }




    val logger :Logger by lazy {
        val str = StringBuilder()
        var me :Command? = this
        while (me != null){
            str.insert(0,me.names.first() + " ")
            me = me.parent
        }
        Logger(str.toString().trim())
    }
    private fun parseInternal(args :List<String>) :ParsedValues {
        if (args.filter { it.isNotBlank() }.isEmpty()){
            return ParsedValues(emptyList(), emptyList(),listOf(this), emptyMap())
        }
        val booleanValues = mutableListOf<BooleanValue>()
        val stringValues = mutableListOf<StringValue>()
        val floating = mutableListOf<String>()



        var index = 0

        while (index < args.size){
            val str = args[index]
            if(str == "--help"){
                throw ShowHelpException()
            }


            //see if it starts with a '-'
            if (str.startsWith('-')){
                // it's an argument
                val substring = str.substring(0,str.indexOf(' ').takeUnless { it == -1 } ?: str.length)
                // erase substring from str in advanced
                val flag = if (substring.startsWith("--")){
                    val name = substring.substring(2)
                    flags.firstOrNull { it.long == name } ?: values.firstOrNull { it.long == name } ?: parseError("can't find value '$name'")
                } else {
                    var chars = substring.substring(1)
                    while (chars.length != 1){//every char that isn't the last one is a boolean
                        val char = chars[0]
                        val theBooleanFlag = flags.firstOrNull() { it.short == char } ?: parseError("can't find boolean flag '$char'")
                        booleanValues.add(BooleanValue(theBooleanFlag.long,true))
                        chars = chars.substring(1)//remove the boolean flag from it
                    }
                    val lastChar = chars[0]
                    flags.firstOrNull { it.short == lastChar } ?: values.firstOrNull { it.short == lastChar } ?: parseError("can't find flag '$lastChar'")
                }
                when (flag) {
                    is BooleanFlag -> //yay easy parse
                        booleanValues.add(BooleanValue(name = flag.long,value = true))
                    is ValueFlag -> {
                        // we need to take the next argument and use it as a string value
                        // if it starts with quotes, we need to take characters until it isn't. quotes can't be escaped.
                        // oh fuck. uhh. fuck. bash arguments time
                        // just take the next arg. if it exists.
                        if (index + 1 == args.size) parseError("can't find value for argument ${flag.long}")
                        val value = args[index + 1]
                        stringValues.add(StringValue(flag.long,value))
                        index++// it's going to need to go one more so that it can not jump to the next thing
                    }
                }
                index++
            } else {
                val nextCommand = subcommands.firstOrNull { it.names.contains(str) }
                if (nextCommand == null && floatingNames.size == floating.size){
                    parseError("can't find command \"$nextCommand\"")
                } else if (nextCommand != null) {
                    val subArray = args.subList(index + 1,args.size)
                    val parsed = try {
                        nextCommand.parseInternal(subArray)
                    }catch (e :ShowHelpException){
                        e.registerCommand(this)
                        throw e
                    }
                    // we need to be first because this is the end of the chain
                    return ParsedValues(booleanValues,stringValues, listOf(this),floating.mapIndexed { i, s -> floatingNames[i].name to s }.toMap()).concat(parsed)
                } else {
                    //it's a floating value
                    floating.add(str)
                    index ++
                }

            }
        }
        return ParsedValues(booleanValues,stringValues, listOf(this),floating.mapIndexed { i, s -> floatingNames[i].name to s }.toMap())


    }

    inner class ShowHelpException() : RuntimeException() {
        private val commands = mutableListOf(this@Command)
        fun registerCommand(command: Command) {
            commands.add(command)
        }

        val helpMessage: String
            get(){
                var s = ""
                s += "Usage: "
                commands.reverse()
                for (command in commands){
                    val options = command.values.isNotEmpty() || command.flags.isNotEmpty()
                    s += command.names.first() + if (options) """ [${command.names.first().toUpperCase()} OPTIONS] """ else " "
                }
                for (floating in floatingNames){
                    s += """[${floating.name}] """
                }
                s += "\n"
                s += commands.first().intro
                s += "\n"
                for (command in commands){
                    if (command.values.isNotEmpty() || command.flags.isNotEmpty() || (command == this@Command && command.floatingNames.isNotEmpty())) {
                        s += "\n[${command.names.first().toUpperCase()} OPTIONS]\n"
                        for (valueFlag in command.values) {
                            s +=
                                (if (valueFlag.short != null) {
                                    "    -" + valueFlag.short + " (${valueFlag.argumentName}) "
                                } else {
                                    ""
                                }).padEnd(15) +
                                        ("  --" + valueFlag.long + " (${valueFlag.argumentName}) ").padEnd(20) +
                                        valueFlag.help + "\n"
                        }
                        for (bool in command.flags) {
                            s += (if (bool.short != null) {
                                "    -" + bool.short
                            } else {
                                ""
                            }).padEnd(15) +
                                    ("  --" + bool.long).padEnd(20) +
                                    bool.help + "\n"
                        }
                    }
                    if(this@Command == command){
                        for(float in floatingNames){
                            s += "    [${float.name}]".padEnd(15) + "".padEnd(20) + float.help + "\n"
                        }
                    }
                }
                if (subcommands.isNotEmpty()) {
                    s += "\nSubcommands:\n"
                    for (sub in subcommands) {
                        s += "    " + (sub.names.first() + "  ").padEnd(15) + sub.desc + '\n'
                    }
                }
                s += commands.last().outro
                s += "\n"
                return s
            }
    }

    inner class ParseException(s: String):RuntimeException(s)

    private fun parseError(s: String): Nothing {
        @Suppress("UNREACHABLE_CODE")
        return throw ParseException(s)
    }
}

class ParsedValues(
    private val booleanFields: List<BooleanValue>,
    private val stringFields: List<StringValue>,
    private val commandTree :List<Command>,
    val floatingValues: Map<String,String>){

    val logger :Logger
        get() = commandTree.last().logger

    private val internalLogger = Logger.forObject(this)


    fun concat(other: ParsedValues): ParsedValues{
        return ParsedValues(
            booleanFields = booleanFields.plus(other.booleanFields),
            stringFields = stringFields.plus(other.stringFields),
            commandTree = commandTree.plus(other.commandTree),
            floatingValues = floatingValues.plus(other.floatingValues))
    }

    fun execute(){
        for (command in commandTree){
            command.subrunner?.let {
                internalLogger.verbose("running subrunner for command ${command.names.first()}")
                it(this)
            }
        }
        internalLogger.verbose("running runner for command ${commandTree.last().names.first()}")
        commandTree.last().runner.invoke(this)
    }

    fun getBoolean(name :String):Boolean{
        return booleanFields.firstOrNull { it.name == name }?.value ?: false
    }

    fun getString(name :String):String {
        return getStringOrNull(name)!!
    }
    fun getStringOrNull(name: String):String? {
        return stringFields.firstOrNull { it.name == name }?.value
    }
}

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

    fun subCommand(block :CommandBuilder.() -> Unit){
        commands += CommandBuilder().apply(block).build()
    }

    var subCommand :Command
        get() = error("no")
        set(value) {
            commands.add(value)
        }


    private val booleanFlags = mutableListOf<BooleanFlag>()
    fun flag(short: Char? = null,long: String,help: String){
        booleanFlags.add(BooleanFlag(short,long,help))
    }

    private val values = mutableListOf<ValueFlag>()
    fun value(short: Char? = null,long: String,help: String,argumentName: String){
        values.add(ValueFlag(short,long,help,argumentName))
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
        floatings.add(FloatingValue(name,help))
    }

    var intro: String = ""
    var outro: String = ""
    var desc: String = ""
    internal fun build():Command{

        if (commands.size > 0 && floatings.size > 0){
            error("can't have both floating values and subcommands")
        }

        val c =  Command(
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

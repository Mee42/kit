package com.gumbocoin.arguments

import com.gumbocoin.logging.Logger

class ParsedValues(
    private val booleanFields: List<BooleanValue>,
    private val stringFields: List<StringValue>,
    private val commandTree :List<Command>,
    val floatingValues: Map<String,String>){

    val logger : Logger
        get() = commandTree.last().logger

    private val internalLogger = Logger.forObject(this)


    fun concat(other: ParsedValues): ParsedValues {
        return ParsedValues(
            booleanFields = booleanFields.plus(other.booleanFields),
            stringFields = stringFields.plus(other.stringFields),
            commandTree = commandTree.plus(other.commandTree),
            floatingValues = floatingValues.plus(other.floatingValues)
        )
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
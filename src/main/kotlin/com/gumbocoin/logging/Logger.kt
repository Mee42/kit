package com.gumbocoin.logging

import kotlin.system.exitProcess

class Logger (private val name: String) {
    companion object {
        fun forObject(obj :Any):Logger{
            return Logger(obj.javaClass.canonicalName)
        }
        var verbose :Boolean = false
        var debug: Boolean = false
    }
    fun out(str :String){
        println(formatStr("out",str))
    }

    private fun formatStr(level: String, str: String): String {
        return if (level == "out")
            str
        else
            "$name:$level -> $str"
    }

    fun verbose(str: String){
        if (verbose){
            println(formatStr("VERBOSE",str))
        }
    }

    fun debug(str :String){
        if (verbose){
            println(formatStr("VERBOSE",str))
        }
        // do nothing for now
    }

    fun warning(str: String){
        System.err.println(str)
    }

    fun error(str: String,throwable: Throwable? = null){
        System.err.println(formatStr("ERROR",str))
        if (verbose) {
            throwable?.printStackTrace()
        }
    }

    fun crashWith(str: String,throwable: Throwable? = null, exitCode: Int = 1) :Nothing{
        error(str,throwable)
        exitProcess(exitCode)
    }
}
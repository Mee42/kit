package com.gumbocoin

import com.gumbocoin.arguments.command
import java.io.File
import java.io.FileNotFoundException

val diff = command {
    name = "diff"
    name = "dif"
    floating("FILE 1","this is the first file")
    floating("FILE 2","this is the second file")
    runner {
        val file1 = getRequiredFloating("FILE 1")
        val file2 = getRequiredFloating("FILE 2")
        //load them into memory
        val text1 :List<String>
        val text2: List<String>
        try {
            text1 = File(file1).readLines(Charsets.UTF_8)
            text2 = File(file2).readLines(Charsets.UTF_8)
        }catch (e :FileNotFoundException){
            logger.crashWith(e.message ?: "error reading files",e)
        }
        if (text1.isEmpty() && text2.isEmpty()){
            return@runner//there should be no output as they are identical
        }
        //traverse A, while testing lines. If there is identical lines, advance both pointers forward

        var pointerA = 0
        var pointerB = 0
        while (pointerA < text1.size && pointerB < text2.size){
            val lineA = text1[pointerA]
            val lineB = text2[pointerB]
            if (lineA == lineB){
                pointerA++
                pointerB++
            } else {
                //figure out how many lines it is till its the same again. find the next identical line in A and B
                var offsetA = -1
                var offsetB = -1
                var set = false
                offsetLoop@ for (offsetAx in pointerA until text1.size){
                    offsetA = offsetAx
                    for (offsetBx in (pointerB until text2.size).reversed()){
                        offsetB = offsetBx
                        val lineOffsetA = text1[offsetA]
                        val lineOffsetB = text2[offsetB]
                        if (lineOffsetA == lineOffsetB){
                            //score! we need to merge the lists here. break out of both loops
                            set = true
                            break@offsetLoop
                        }
                    }
                }
                if (!set){
                    offsetA = text1.size
                    offsetB = text2.size
                }
                println("LINE $pointerA")
                //okay so we have the two offsets. everything from pointerA to offsetA (inclus,exclus) should be...added?
                for(line in text1.subList(pointerA,offsetA)){
                    println("- $line")
                }
                for(line in text2.subList(pointerB,offsetB)){
                    println("+ $line")
                }
                println()
                pointerA = offsetA
                pointerB = offsetB
            }
        }
        when {
            text1.size > pointerA -> {
                //take the remaining lines and subtract them
                println("LINE $pointerA")
                val remainingTextA = text1.subList(pointerA,text1.size)
                for (line in remainingTextA){
                    println("- $line")
                }
            }
            text2.size > pointerB -> {
                println("LINE $pointerB")
                val remainingTextB = text2.subList(pointerB,text2.size)
                for (line in remainingTextB){
                    println("+ $line")
                }
            }
            else -> logger.verbose("end")
        }
    }
}

/*
A:

a <- pointerA
b
c
d

B:

a <- pointerB
c
d





 */
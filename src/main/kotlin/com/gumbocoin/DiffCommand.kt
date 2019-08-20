package com.gumbocoin

import com.gumbocoin.arguments.command

val diff = command {
    name = "diff"
    name = "dif"
    floating("FILE 1","this is the first file")
    floating("FILE 2","this is the second file")
    runner {
        println(this.floatingValues)
    }
}
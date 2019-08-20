package com.gumbocoin

import com.gumbocoin.arguments.Command
import com.gumbocoin.arguments.ParsedValues
import com.gumbocoin.arguments.command
import com.gumbocoin.logging.Logger

const val VERSION = "v0.0.1"

val command = command {
    name = "kit"
    intro = "kit - a git clone written in kotlin"
    flag('v',"verbose","enable verbose output")
    flag('d',"debug","enable debug output")
    subrunner {
        if (getBoolean("verbose")){
            Logger.verbose = true
        }
    }
    subCommand {
        name = "version"
        runner {
            logger.out("Version: $VERSION")
        }
    }
    runner {
        logger.out("Kit - a source control tool, written in kotlin.")
        logger.out("for more information, run kit --help")
    }
    subCommand = diff

}



// TODO
//   config file

fun main() {
    command.parse("diff --help").execute()
}
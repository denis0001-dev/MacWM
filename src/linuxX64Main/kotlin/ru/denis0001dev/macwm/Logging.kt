@file:OptIn(ExperimentalForeignApi::class)
package ru.denis0001dev.macwm;

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Clock.System
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.denis0001dev.util.File
import ru.denis0001dev.util.IOException
import ru.denis0001dev.util.Mode
import ru.denis0001dev.util.plusAssign

object Logging {
    private const val LOGFILE = "/var/log/macwm.log"
    private const val LOGFILE_NOROOT = "./macwm.log"

    enum class Level {
        INFO,
        WARNING,
        ERROR,
        FATAL,
        DEBUG
    }

    private lateinit var file: File;

    fun init(): Boolean {
        file = File(LOGFILE)
        try {
            file.open(Mode.Write)
            file.write("")
            file.close()
        } catch (e: IOException) {
            println(
                buildLogMessage(
                    Level.ERROR,
                    "Error opening log file ${LOGFILE}, do you have root?"
                )
            );
            println(buildLogMessage(Level.INFO, "Using fallback log file."));
            try {
                file = File(LOGFILE_NOROOT)
                file.open(Mode.Write)
                file.write("")
                file.close()
            } catch (e: IOException) {
                println(
                    buildLogMessage(
                        Level.FATAL,
                        "Error opening fallback log file ${LOGFILE_NOROOT}, exiting"
                    )
                );
                return false;
            }
        }
        file.open(Mode.Append)
        return true;
    }

    fun buildLogMessage(level: Level, message: String): String {
        val logmsg = StringBuilder();
        // Get timestamp
        val t = System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        logmsg += "[${t.dayOfMonth}.${t.month}.${t.year} ${t.hour}:${t.minute}:${t.second}]"

        // Add color and message
        when (level) {
            Level.INFO -> logmsg.append(" INFO: ").append(message);
            Level.DEBUG -> logmsg.insert(0, "\u0033[37m").append(" DEBUG: ").append(message);
            Level.WARNING -> logmsg.insert(0, "\u0033[1;93m").append(" WARN: ").append(message);
            Level.ERROR -> logmsg.insert(0, "\u0033[0;91m").append(" ERROR: ").append(message);
            Level.FATAL -> logmsg.insert(0, "\u0033[1;31m").append(" FATAL: ").append(message);
        }
        logmsg.append("\u0033[0m\n");
        return logmsg.toString();
    }

    fun log(level: Level, message: String) {
        if (!file.isOpen) {
            println("Error: log file is not open, did you forget to call init()?");
        }
        val logmsg = buildLogMessage(level, message);
        // The message is ready, write it
        println(logmsg)
        file.write("$logmsg\n")
    }

    fun close() = file.close()
}
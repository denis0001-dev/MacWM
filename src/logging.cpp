//
// Created by denis0001-dev on 05.10.2024.
//

#include "logging.hpp"
#include <iostream>
#include "util.hpp"

namespace logging {
    bool init() {
        out.open(LOGFILE, DISCARD_CONTENT);
        if (!out.is_open()) {
            println(buildLogMessage(ERROR,
                std::string("Error opening log file ") +
                LOGFILE +
                ", do you have root?")
                );
            println(buildLogMessage(INFO, "Using fallback log file."));
            out.open(LOGFILE_NOROOT, DISCARD_CONTENT);
            if (!out.is_open()) {
                println(buildLogMessage(FATAL,
                    std::string("Error opening fallback log file") +
                    LOGFILE_NOROOT +
                    ", exiting")
                    );
                return false;
            }
        }
        return true;
    }

    std::string buildLogMessage(const level level, const std::string &message) {
        std::string logmsg;
        // Get timestamp
        const time_t now = time(nullptr);
        tm tstruct; // NOLINT(*-pro-type-member-init)
        char buf[80];
        tstruct = *localtime(&now);
        strftime(buf, sizeof(buf), "[%d.%m.%Y %X]", &tstruct);
        logmsg.append(buf);
        // Add color and message
        switch (level) {
            case INFO:
                logmsg.append(" INFO: ").append(message);
            break;
            case DEBUG:
                logmsg.insert(0, "\033[37m");
            logmsg.append(" DEBUG: ").append(message);
            break;
            case WARNING:
                logmsg.insert(0, "\033[1;93m");
            logmsg.append(" WARN: ").append(message);
            break;
            case ERROR:
                logmsg.insert(0, "\033[0;91m");
            logmsg.append(" ERROR: ").append(message);
            break;
            case FATAL:
                logmsg.insert(0, "\033[1;31m");
            logmsg.append(" FATAL: ").append(message);
            break;
        }
        logmsg.append("\033[0m\n");
        return logmsg;
    }

    void log(const level level, const std::string &message) {
        if (!out.is_open()) {
            errln("Error: log file is not open, did you forget to call init()?");
        }
        const std::string logmsg = buildLogMessage(level, message);
        // The message is ready, write it
        std::cout << logmsg;
        out << logmsg;
    }


    void close() {
        out.close();
    }
}
//
// Created by denis0001-dev on 05.10.2024.
//

#ifndef LOGGING_HPP
#define LOGGING_HPP


#define LOGFILE "/var/log/macwm.log"
#define LOGFILE_NOROOT "./macwm.log"
#define DISCARD_CONTENT (std::ofstream::out | std::ofstream::trunc)

#include <fstream>

namespace logging {
    enum level {
        INFO,
        WARNING,
        ERROR,
        FATAL,
        DEBUG,
    };

    inline std::ofstream out;

    bool init();

    std::string buildLogMessage(level level, const std::string &message);
    void log(level level, const std::string &message);

    void close();
}

#endif //LOGGING_HPP

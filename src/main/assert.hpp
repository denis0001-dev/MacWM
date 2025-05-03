#ifndef ASSERT_HPP
#define ASSERT_HPP

#include "logging.hpp"
#include <cstdlib>
#include <string>

namespace assert {

inline void check(bool condition, const std::string& message) {
    if (!condition) {
        logging::log(logging::FATAL, "Check failed: " + message);
        std::exit(1);
    }
}

template<typename T, typename U>
inline void check_eq(const T& a, const U& b, const std::string& message) {
    if (a != b) {
        logging::log(logging::FATAL, "Check failed: " + message);
        std::exit(1);
    }
}

template<typename T>
inline T* check_not_null(T* ptr, const std::string& message) {
    if (ptr == nullptr) {
        logging::log(logging::FATAL, "Check failed: " + message);
        std::exit(1);
    }
    return ptr;
}

} // namespace assert

#endif // ASSERT_HPP 
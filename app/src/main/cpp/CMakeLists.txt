cmake_minimum_required(VERSION 3.22.1)
project("fuzzysearch")

add_library(
        fuzzysearch
        SHARED
        fuzzy_search.cpp
)

find_library(log-lib log)

target_link_libraries(
        fuzzysearch
        ${log-lib}
)

target_compile_options(fuzzysearch PRIVATE -O3 -Wall -Wextra)

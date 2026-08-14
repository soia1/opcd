cmake_minimum_required(VERSION 3.18.1)
project(opcd_exec_shim C)

add_library(opcd_exec_shim SHARED opcd_exec_shim.c)

target_link_libraries(opcd_exec_shim dl)

# Strip unnecessary symbols to keep the shim small (~6 KB).
set_target_properties(opcd_exec_shim PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections -Wl,--strip-all")
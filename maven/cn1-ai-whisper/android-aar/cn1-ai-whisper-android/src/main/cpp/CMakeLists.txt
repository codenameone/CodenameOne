cmake_minimum_required(VERSION 3.22.1)
project(cn1_ai_whisper_android LANGUAGES C CXX)

add_library(cn1aiwhisper SHARED native_whisper_jni.cpp)
target_compile_features(cn1aiwhisper PRIVATE cxx_std_17)

if (DEFINED WHISPER_CPP_DIR AND NOT "${WHISPER_CPP_DIR}" STREQUAL "")
    set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
    add_subdirectory("${WHISPER_CPP_DIR}" "${CMAKE_BINARY_DIR}/whisper.cpp" EXCLUDE_FROM_ALL)
    target_link_libraries(cn1aiwhisper PRIVATE whisper)
elseif (DEFINED WHISPER_PREBUILT_DIR AND NOT "${WHISPER_PREBUILT_DIR}" STREQUAL "")
    if (NOT DEFINED WHISPER_INCLUDE_DIR OR "${WHISPER_INCLUDE_DIR}" STREQUAL "")
        message(FATAL_ERROR "WHISPER_INCLUDE_DIR is required when WHISPER_PREBUILT_DIR is used")
    endif ()
    add_library(whisper SHARED IMPORTED)
    set_target_properties(whisper PROPERTIES
            IMPORTED_LOCATION "${WHISPER_PREBUILT_DIR}/${ANDROID_ABI}/libwhisper.so"
            INTERFACE_INCLUDE_DIRECTORIES "${WHISPER_INCLUDE_DIR}")
    target_link_libraries(cn1aiwhisper PRIVATE whisper)
else ()
    message(FATAL_ERROR "Set WHISPER_CPP_DIR to a whisper.cpp checkout or WHISPER_PREBUILT_DIR plus WHISPER_INCLUDE_DIR to prebuilt Android libraries")
endif ()

find_library(log-lib log)
target_link_libraries(cn1aiwhisper PRIVATE ${log-lib})

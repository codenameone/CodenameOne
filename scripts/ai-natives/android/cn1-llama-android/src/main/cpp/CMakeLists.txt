cmake_minimum_required(VERSION 3.22.1)
project(cn1_llama_android LANGUAGES C CXX)

# Google Play requires every 64-bit shared library in an upload to have
# PT_LOAD segments aligned to 16 KB -- see
# https://developer.android.com/guide/practices/page-sizes. NDK r28, which
# build.gradle pins, does this by default; the flags keep it true for any
# other NDK. Set before any target so the upstream subproject gets them too.
add_link_options(-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)

# One shared library exporting only its JNI functions. ggml is linked in
# statically with its symbols hidden, so the whisper and llama libraries can
# share a process without one's ggml resolving to the other's.
set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
# OpenMP would add libomp.so to the AAR, and a second AAR carrying it fails
# the app's merge; ggml's own thread pool is used instead.
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
# llamafile's sgemm needs the fp16 NEON loads armv7 does not have.
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
endif()
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)

add_subdirectory("${UPSTREAM_DIR}" "${CMAKE_BINARY_DIR}/upstream" EXCLUDE_FROM_ALL)

set(CN1_SRC "${CMAKE_CURRENT_SOURCE_DIR}/../../../../..")
add_library(cn1llama SHARED "${CN1_SRC}/llama/src/cn1_llama.cpp" "${CN1_SRC}/llama/jni/cn1_llama_jni.cpp")
target_compile_features(cn1llama PRIVATE cxx_std_17)
target_include_directories(cn1llama PRIVATE "${CN1_SRC}/llama/src")
target_link_libraries(cn1llama PRIVATE llama)
target_link_options(cn1llama PRIVATE -Wl,--exclude-libs,ALL -Wl,--gc-sections)

find_library(log-lib log)
target_link_libraries(cn1llama PRIVATE ${log-lib})

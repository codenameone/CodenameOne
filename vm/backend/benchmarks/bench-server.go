// The Go half of the comparison: the same two routes as demo/bench, on net/http.
//
// net/http rather than fasthttp, because "Go performance" to most people means
// the standard library, and because it is the honest comparison: fasthttp is
// faster than net/http, so a claim measured against net/http must not be
// restated as a claim about Go in general.
//
// Deliberately plain: no router, no middleware, no logging. Anything added here
// that demo/bench does not do is a difference in the measurement rather than in
// the runtimes.
package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"runtime"
	"strconv"
)

type message struct {
	Message string `json:"message"`
}

func main() {
	port := envInt("PORT", 8080)

	mux := http.NewServeMux()
	mux.HandleFunc("/plaintext", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte("Hello, World!"))
	})
	mux.HandleFunc("/json", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(message{Message: "Hello, World!"})
	})

	fmt.Printf("bench listening on port %d with GOMAXPROCS=%d\n", port, runtime.GOMAXPROCS(0))
	server := &http.Server{Addr: fmt.Sprintf(":%d", port), Handler: mux}
	if err := server.ListenAndServe(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func envInt(name string, fallback int) int {
	value := os.Getenv(name)
	if value == "" {
		return fallback
	}
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return fallback
	}
	return parsed
}

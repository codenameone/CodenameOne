// The Go half again, on fasthttp instead of net/http.
//
// net/http is what "Go" means to most people, but it is NOT what anyone who
// cares about throughput deploys, and a claim measured only against the
// standard library is the kind that gets taken apart the first time a reader
// runs valyala/fasthttp themselves. So both are measured and both are reported.
//
// Deliberately the same shape as bench-server.go: two routes, no router, no
// middleware, no logging, and the JSON body SERIALISED per request rather than
// returned as a constant -- anything else would be a difference in the
// measurement rather than in the runtimes.
package main

import (
	"encoding/json"
	"fmt"
	"os"
	"runtime"
	"strconv"

	"github.com/valyala/fasthttp"
)

type message struct {
	Message string `json:"message"`
}

func main() {
	port := envInt("PORT", 8080)

	handler := func(ctx *fasthttp.RequestCtx) {
		switch string(ctx.Path()) {
		case "/plaintext":
			ctx.SetContentType("text/plain")
			ctx.Write([]byte("Hello, World!"))
		case "/json":
			ctx.SetContentType("application/json")
			// json.Encoder like the net/http arm, so the body is built the same
			// way and the trailing newline matches too.
			json.NewEncoder(ctx).Encode(message{Message: "Hello, World!"})
		default:
			ctx.SetStatusCode(fasthttp.StatusNotFound)
		}
	}

	fmt.Printf("fasthttp listening on port %d with GOMAXPROCS=%d\n", port, runtime.GOMAXPROCS(0))
	if err := fasthttp.ListenAndServe(fmt.Sprintf(":%d", port), handler); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func envInt(name string, def int) int {
	if v := os.Getenv(name); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
	}
	return def
}

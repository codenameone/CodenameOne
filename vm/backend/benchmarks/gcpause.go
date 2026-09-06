// The Go twin of demo/gcpause/com/demo/GcPause.java: the same loop, the same
// live-set size, the same iteration count, the same log2 histogram.
//
// Deliberately identical rather than idiomatic. Anything done here that the Java
// side does not do is a difference in the measurement rather than in the
// collectors.
package main

import (
	"fmt"
	"os"
	"strconv"
	"time"
)

type node struct {
	v    int32
	next *node
}

func envInt(name string, def int) int {
	if v := os.Getenv(name); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
	}
	return def
}

func main() {
	iterations := envInt("ITERS", 20000000)
	liveSize := envInt("LIVE", 4096)
	live := make([]*node, liveSize)
	var buckets [48]int64
	var worst int64
	var checksum int64

	for i := 0; i < 1000000; i++ {
		live[i&(liveSize-1)] = &node{v: int32(i)}
	}

	prev := time.Now()
	for i := 0; i < iterations; i++ {
		n := &node{v: int32(i), next: live[(i*7)&(liveSize-1)]}
		live[i&(liveSize-1)] = n
		checksum += int64(n.v)
		now := time.Now()
		d := now.Sub(prev).Nanoseconds()
		prev = now
		b := 0
		for x := d; x > 0 && b < 47; x >>= 1 {
			b++
		}
		buckets[b]++
		if d > worst {
			worst = d
		}
	}
	report(&buckets, worst, checksum, int64(iterations))
}

func report(buckets *[48]int64, worst, checksum, iterations int64) {
	fmt.Printf("GCPAUSE iterations=%d checksum=%d\n", iterations, checksum)
	fmt.Printf("GCPAUSE maxNs=%d\n", worst)
	var total int64
	for _, c := range buckets {
		total += c
	}
	pct("p50", buckets, total, 0.50)
	pct("p99", buckets, total, 0.99)
	pct("p999", buckets, total, 0.999)
	pct("p9999", buckets, total, 0.9999)
	var stalls int64
	for b := 17; b < len(buckets); b++ {
		stalls += buckets[b]
	}
	fmt.Printf("GCPAUSE stallsOver64us=%d\n", stalls)
	for b := 17; b < len(buckets); b++ {
		if buckets[b] != 0 {
			fmt.Printf("GCPAUSE bucket=%dns count=%d\n", int64(1)<<(b-1), buckets[b])
		}
	}
}

func pct(name string, buckets *[48]int64, total int64, q float64) {
	want := int64(q * float64(total))
	var seen int64
	for b := 0; b < len(buckets); b++ {
		seen += buckets[b]
		if seen > want {
			v := int64(0)
			if b > 0 {
				v = int64(1) << (b - 1)
			}
			fmt.Printf("GCPAUSE %sNs=%d\n", name, v)
			return
		}
	}
}

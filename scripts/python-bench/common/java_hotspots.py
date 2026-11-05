# The same workloads with the hot spot written in Java: each entry calls
# com.bench.CommonWorkloads (vm/benchmarks/common), the class every Codename
# One port benchmarks, straight from Python. This is how an application
# tunes a measured hot spot -- the Python around it stays as it is.
from java.com.bench import CommonWorkloads

WORKLOADS = {
    "intArithmetic": CommonWorkloads.intArithmetic,
    "longArithmetic": CommonWorkloads.longArithmetic,
    "mathTranscendental": CommonWorkloads.mathTranscendental,
    "arraySequential": CommonWorkloads.arraySequential,
    "arrayRandom": CommonWorkloads.arrayRandom,
    "objectAllocation": CommonWorkloads.objectAllocation,
    "valueEscape": CommonWorkloads.valueEscape,
    "hashMapChurn": CommonWorkloads.hashMapChurn,
    "stringBuilding": CommonWorkloads.stringBuilding,
    "recursion": CommonWorkloads.recursion,
    "quicksort": CommonWorkloads.quicksortBench,
}

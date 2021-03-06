// This is a Flix implementation of a Floyd-Warshall-like algorithm
// for finding shortest paths between all pairs of nodes in a graph.

// Inputs: Directed distances d between nodes a and b
rel Edge(a: Str, b: Str, d: Int)

// Outputs: Shortest distance d from a to b
lat ShortestDist(a: Str, b: Str, d: Dist)

// Elements of the distance lattice
enum Dist {
  case Top,      // Reachable from a negative-cost cycle
  case Dst(Int), // Reachable with some minimum distance
  case Bot       // Not reachable
}

// Lattice operations

///
/// Returns `true` iff `e1` is equal to `e2`.
///
def equ(e1: Dist, e2: Dist): Bool = e1 == e2

///
/// Returns `true` if the distance `e1` is greater than or equal to the distance `e2`.
///
def leq(e1: Dist, e2: Dist): Bool = match (e1, e2) with {
  case (_, Top) => true
  case (Bot, _) => true
  case (Dst(n1), Dst(n2)) => n1 >= n2
  case _ => false
}

///
/// Returns the shorter distance of `e1` and `e2`.
///
def lub(e1: Dist, e2: Dist): Dist = match (e1, e2) with {
  case (Bot, x) => x
  case (x, Bot) => x
  case (Dst(n1), Dst(n2)) => Dst(Int32.min(n1, n2))
  case _ => Top
}

///
/// Returns the greater distance of `e1` and `e2`.
///
def glb(e1: Dist, e2: Dist): Dist = match (e1, e2) with {
  case (Top, x) => x
  case (x, Top) => x
  case (Dst(n1), Dst(n2)) => Dst(Int32.max(n1, n2))
  case _ => Bot
}

let Dist<> = (Bot, Top, equ, leq, lub, glb)

// Copy input relation into the ShortestDist lattice
ShortestDist(a, b, Dst(d)) :- Edge(a, b, d).

// If a->b->c, try using that to get a->c
ShortestDist(a, c, sum(d1, d2)) :- ShortestDist(a, b, d1), ShortestDist(b, c, d2).

// If you can get from a->a with negative distance, then it is part of a negative cycle
ShortestDist(a, a, Top) :- ShortestDist(a, a, d), negativeDist(d).

///
/// Transfer function.
/// Returns the sum of two given distances.
///
def sum(e1: Dist, e2: Dist): Dist = match (e1, e2) with {
  case (Top, _) => Top
  case (_, Top) => Top
  case (Dst(n1), Dst(n2)) => Dst(n1 + n2)
  case _ => Bot
}

///
/// Filter function.
/// Returns `true` if the given distance `d` is negative.
///
def negativeDist(d: Dist): Bool = match d with {
  case Top => true
  case Dst(x) => x < 0
  case _ => false
}

// Example input data
Edge("a", "b", 1).
Edge("b", "c", 2).
Edge("c", "a", 3).
Edge("c", "d", 4).
Edge("d", "e", 7).
Edge("d", "f", 11).
Edge("f", "e", 23).

// Negative cycle
Edge("1", "2", -3).
Edge("2", "3", 1).
Edge("3", "1", 1).
Edge("3", "4", 30).

// Expected output
// ShortestDist
// +-----+-----+---------+
// | a   | b   | d       |
// +-----+-----+---------+
// | "1" | "1" | Top     |
// | "1" | "4" | Top     |
// | "1" | "2" | Top     |
// | "1" | "3" | Top     |
// | "2" | "4" | Top     |
// | "2" | "2" | Top     |
// | "2" | "3" | Top     |
// | "2" | "1" | Top     |
// | "3" | "2" | Top     |
// | "3" | "3" | Top     |
// | "3" | "1" | Top     |
// | "3" | "4" | Top     |
// | "a" | "b" | Dst(1)  |
// | "a" | "e" | Dst(14) |
// | "a" | "c" | Dst(3)  |
// | "a" | "f" | Dst(18) |
// | "a" | "d" | Dst(7)  |
// | "a" | "a" | Dst(6)  |
// | "b" | "b" | Dst(6)  |
// | "b" | "e" | Dst(13) |
// | "b" | "f" | Dst(17) |
// | "b" | "c" | Dst(2)  |
// | "b" | "a" | Dst(5)  |
// | "b" | "d" | Dst(6)  |
// | "c" | "c" | Dst(6)  |
// | "c" | "f" | Dst(15) |
// | "c" | "a" | Dst(3)  |
// | "c" | "d" | Dst(4)  |
// | "c" | "e" | Dst(11) |
// | "c" | "b" | Dst(4)  |
// | "d" | "e" | Dst(7)  |
// | "d" | "f" | Dst(11) |
// | "f" | "e" | Dst(23) |
// +-----+-----+---------+

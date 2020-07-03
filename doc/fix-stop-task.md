# Unbalancing PerfMark Calls

Tl;Dr: `PerfMark.stopTask()` will no longer use task name or tags.

# Background

PerfMark was designed as a replacement for an existing tracing library called
JEndoscope.  JEndoscope used annotations to indicate which methods to trace.
When the classes were loaded, a Java agent would rewrite the methods to include
the tracing calls if JEndoscope was enabled.   One of the benefits of this API
is that it isn't possible to forget to add the closing trace call, which
indicates a span is complete.  Thus, the danger of mismatched start and stop
trace calls was not a problem.

PerfMark was designed to not rely on an agent for tracing, and thus could not
use annotations to indicate which calls to trace.   To avoid the risk for API
users, PerfMark offered matching stop trace calls that included the relevant
task name and tag.  An example usage:

```java
PerfMark.startTask("makeRPC");
invokeSomeRPC();
PerfMark.stopTask("makeRPC");
```

When PerfMark returned the traces collected, it was expected that the
start task name matched the stop task name.  It could verify these to ensure
the trace calls were properly balanced.  It was expected that a warning could
be emitted to aid the user in fixing the mismatch.

There is an additional benefit to this API that was not needed in the previous
JEndoscope model.   PerfMark allows tracing to be dynamically enabled and
disable at runtime.   This means that tracing may be enabled in the middle of a
start-stop pair.   This would mean that only the stop would be recorded,
without knowing the starting task name.   Even if the calls were properly
balanced, if only the stop edge is present, there isn't a way to reconstruct the
name of the task.   Having the task name in both the start and the stop solves
this problem.   (The same problem exists on the opposite side too, if the
traces are read before the task completes.)

Finally, having the task name in both the start and stop has a symmetry to it
that made it look correct.  It was clear which task was being stopped, even if
the start trace was many lines above.  

# Problems with stopTask().

At the time of the design, the aforementioned choices made sense.   However,
several problems arose from this pattern that can't easily be worked around.

## Stuttering Arguments Makes Code Verbose

The PerfMark API is very fast, but it accomplishes this by trusting the
programmer to be careful.   Because of this, a safe invocation of the trace
involves Wrapping the code in a try-finally block:

```java
PerfMark.startTask("makeRPC");
try {
  invokeSomeRPC();
} finally {
  PerfMark.stopTask("makeRPC");
}
```

This costs an indent block, as well as 3 extra lines per trace call.   This
puts us into verbosity debt.  Having multiple such calls makes the code pyramid
off the page, decreasing the readability of the code.

In order to repay this debt, dropping the redundant task name (and tag) makes
the code less unpleasant to look at.

While the duplicated names do have technical reasons (mentioned above), users
feedback indicated the verbosity was more of a problem.  Given the relatively
rare problems with mismatched tags and split traces, addressing verbosity is
more important.

## try-with-resources Imposes an Allocation cost

One of the ways Java helps programmers clean up resources is the
try-with-resources idiom.  PerfMark explored using this as an alternative to
bare start-stop calls with the following usage:

```java
try (PerfMarkTask task = PerfMark.task("makeRPC")) {
  invokeSomeRPC();
}
```

In an ideal world, `PerfMarkTask` would be a no-op object when PerfMark is
disabled, and be a singleton object when enabled.   This would in turn call the
appropriate start-stop methods.  However, because the preferred start-stop calls
*both* require the task name, a new object must be allocated to capture the name
(and tag).   This forces the user to choose between a runtime cost and safe
programming practice.  

## Lazy Task Names complicate API.

Another way PerfMark explored making trace calls cheaper was to use a lambda
base task naming call.  Sometimes task names are not compile time constant, and
in fact may have a significant runtime cost.  Eagerly calculating these imposes
a runtime cost, even when PerfMark is disabled.   The following API shows the
proposed usage:

```java
PerfMark.startTask(rpc, RPC::nameAndId);
// Same as PerfMark.startTask(rpc.nameAndId());
```

The use of a method handle avoids the unwanted calculation of the `nameAndId`
string, which the JVM may not be able to eliminate otherwise.   

The problem becomes more awkward when encouraging users to use matching names
for start and stop:

```java
PerfMark.startTask(rpc, RPC::nameAndId);
invokeSomeRPC();
PerfMark.stopTask(rpc, RPC::nameAndId);
```

Will the expensive function be calculated twice?  What happens if the function
is not idempotent?  The user has used the API to the best of their ability, but
the skew problems are still present.  Trying to solve this is difficult without
imposing other costs, and being hard to reason about.

### Permutations in API size.

PerfMark has several overloads of the start - stop calls:

* `startTask(String name);`
* `startTask(String name, Tag tag);`
* `startTask(String name, String subName);`


While exploring lambdas to make lazy variants of these methods, the API grew
substantially.  As the number of start methods grows, so too must the stop
methods.   This makes usage more error prone since there may be 10s of possible
overloads to invoke.  This is an unnecessary burden on the user, since the
library already must deal with the possibility of mismatched names.


## Agent Byte Code is more difficult

While JEndoscope required an agent to work, PerfMark can be optionally enhanced
by using one.  It can add line and file information about the calls
when instrumenting classes.  However, it's difficult to rewrite this byte code
when it depends on the possibly changing task name.  JEndoscope worked around
this problem by forcing all tracing calls to be compile time constants, and
forcing rewritten calls to use the `ldc` JVM instruction.  

# Proposed Change: one stopTask()

To avoid the problems above, a singular `stopTask()` method overload is
proposed.  The existing stopTask overloads will no longer be recommended, and
behave as the single `stopTask()` method with extra info.  The tracing library
will be updated to not expect this info.

This solves each of the problems above.   The usage of PerfMark becomes less
verbose.  There is no longer a runtime cost to using the
try-with-resources idiom.  There is no possibility of skew between start and
stop names and tags.  The API will grow slightly to accommodate this new method,
but will prevent the doubling of methods.

The benefits brought by having the name twice are seldom seen, and are paid
at runtime.  The names passed to stopTask() are stored, but are never used
when visualizing trace diagrams.

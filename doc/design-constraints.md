# PerfMark Design Constraints

PerfMark is tailored to work in as many environments as possible.   
The implementation and API surface have been carefully crafted to 
work in lots of different places while still maintaining low
overhead.  The following considerations are those that other library
authors should consider when publishing their code.

## Binary Size
### Mobile Devices and Constrained Platforms
In environments like Android, the size of the code being distributed
is a critical constraint.  Each function and class added to the 
library must be transmitted to phones and tablets, especially in places
where Internet data is neither fast nor cheap.  Mobile applications
have many challenges that server applications do not:

__Getting the application on the mobile device costs network bandwidth.__
This means users are less likely to download applications that are 
large, as they may cost a lot.  Library authors contribute to this cost
and thus put pressure on app writers to trim their binaries down.

__Mobile devices are often storage constrained.__ While talking to an
Android optimization engineer at Google, I found out that a substantial
number of devices were on very old versions of apps.   The reason was
that the users had taken so many photos, there was no room left on the
phone to update applications.  Reducing library size helps app authors
distribute their code more easily.

__Android Applications are Method Constrained.__ In a similar vain,
Android devices have something called the Dex Limit, which limits the 
maximum number of methods to be 65K.   Speaking with the same Android
optimization engineer mentioned above, I had heard that the scarcity
of methods was so bad, that the Mobile app's Engineers had to get 
VP-level approval to add new Java methods to their code.  These 
problems have work-arounds like MultiDex, but the cost is still 
present.

### Cloud and Server Environments
In a cloud environment, the binary size is still somewhat relevant,
as the container for Java code still has to be transmitted to each 
machine.  While storage and network are cheap and fast, it's still
nice to have small JARs and Docker containers.  It eases the startup
cost when turning on new instances.

### PerfMark's Approach.
To minimize binary size cost, PerfMark is split into two packages:

* `perfmark-api` - The trace API methods.
* `perfmark-impl` - The implementation for recording trace data.

The `perfmark-api` package includes No-op implementations of each of 
the tracing methods that are used in the case that the implementation
is not present.  When static initialization happens in the API, it 
attempts to load the implementation dynamically (i.e. using 
`Class.forName`).  If absent, the API disables itself safely.  If 
present, the API links to the correct end points for storage.  

The benefit of this approach is that the API is only 7KB in size.
Application and library authors can include `perfmark-api` easily in
their applications with minimal worry about binary size.  The number
of methods used in the API package is minimal, and can easily be
reduced based on static analyzers (like 
[Proguard](https://www.guardsquare.com/proguard) and 
[R8](https://developer.android.com/studio/build/shrink-code)).  If
an application wants to include the tracing implementation, all it
takes is adding it to the classpath.

## Tooling Integration
One of benefits of Java is the stability of the class 
[ABI](https://en.wikipedia.org/wiki/Application_binary_interface).
The Java tooling ecosystem has flourished due to the ability to 
automatically parse and modify _compiled_ Java code.

To this end, making it easy for these tools to consume library code
should be a goal.  Some specific examples:

__Using `Class.forName(name)`__.  Tools like Proguard work by 
removing methods and types that are unused.  Because of the dynamic
nature of the JVM, this can be more difficult for static analyzers.
Tools can know at compile time if a class will be used if they see
a reflective reference (like `Class.forName(name)`).  If the class
is known not to be on the classpath, the code can be safely removed.

Note the delicacy of this: Proguard cannot make the same transform
with `Class.forName(name, init, classloader)`.  Only the first 
overload will work.  Secondly, note that this works when passing a
_constant_ string to the function.  Putting the class names in a
list or array and iterating over them would defeat the static 
analysis.

__Using Static Methods__.  Hotspot and other JVM JITs have amazed
the world with the amount of optimization they can do.  There is 
almost no difference between calling virtual methods on classes and
interfaces.  While the cost of virtual methods is practically
identical, static methods are much easier to inject from tooling.

For example, a Java Agent (a small program that can modify class files
at program startup) is able to inject tracing methods into other 
classes.  To do this, it needs to understand the Java byte code
enough to call methods inside the body of other methods.  Static
method are the easiest to call, since they have no receiver object.
There is no need to make sure the type is correct on the stack or in
local variables, since they are not used.

Tooling authors can work much more easily around static method 
invocations and thus can instrument their code with yours.

### PerfMark's Approach
All Methods on the `PerfMark` class are static methods to make 
calling easy.  Both Java code and JVM classfile readers can understand
where tracing is happening.  

An early design mistake is still visible in the API, but has been
disabled.  PerfMark includes [Link](https://javadoc.io/doc/io.perfmark/perfmark-api/latest/io/perfmark/Link.html) 
objects for keeping track of which tasks are related to each other 
across thread boundaries.  The original usage looked something like:

```java
class Foo {
  volatile Link link;
  
  void start() {
    PerfMark.startTask("Foo.start");
    this.link = PerfMark.link();
    new Thread(Foo::work).start();
    PerfMark.stopTask("Foo.start");
  }
  
  void work() {
    PerfMark.startTask("Foo.work");
    link.link();
    // ...
    PerfMark.stopTask("Foo.work");
  }
}
```

This approach works for Human writing code, but complicates _Tooling_
writing code.  When I started implementing the PerfMark Java Agent,
It became more difficult to keep track of the `Link` object.  Finding 
the method name on the object, and calling it from outside the 
`PerfMark` class took more work.  As mentioned in the 
[commit](https://github.com/perfmark/perfmark/commit/c4340b24):

> It's hard to annotate the links with the classfile transformer.
Keeping the linkId hidden and being able to call the public, Marker
overload needs some amount of public access.  [...]

(A Marker is a filename and line annotation that the Agent injects 
into the tracing call.)

The new version added later treats the `Link` as opaque and only works
with it from the `PerfMark` level:

```java
class Foo {
  volatile Link link;
  
  void start() {
    PerfMark.startTask("Foo.start");
    this.link = PerfMark.linkOut();
    new Thread(Foo::work).start();
    PerfMark.stopTask("Foo.start");
  }
  
  void work() {
    PerfMark.startTask("Foo.work");
    PerfMark.linkIn(link);
    // ...
    PerfMark.stopTask("Foo.work");
  }
}
```

This method has less stuttering, and keeps API consistency with
all interactions starting with the `PerfMark` class as an entrypoint.


## Zero Cost (Only Pay For What You Use)

Library developers are in a unique position compared to application
authors.  A library takes on the intersection of performance 
constraints of all users, not just a few.  If  library has poor or 
unfixable performance problems, other people will reinvent the 
functionality of your code.  Thus, having good performance (high 
efficiency, low variance) increases adoption.

### PerfMark's Approach

PerfMark guards each tracing call with a check to see if the 
implementation has been enabled.  This check potentially happens  
millions of times a second, so the cost of doing nothing could 
be surprisingly high.  To avoid this cost, PerfMark leans on the JIT
to remove the check altogether.   When PerfMark is disabled, it informs
the JVM that the class will never do anything, and asks the JVM to
recompile the code accordingly.  When PerfMark is re-enabled, it 
again asks the JVM to recompile the code to be unconditionally on.

There are a few tradeoffs with this approach that need to be mentioned,
however.  First, turning PerfMark on or off has a high cost, since
the JIT needs to recompile the calling code. It may need to de-optimize
the caller and recompute, adding latency to all tracing calls in the 
VM.  This is mentioned in the JavaDoc, but the precise cost is hard
to measure without knowing the use case.

Second, the JVM optimizes the "is PerfMark enabled?" check only once 
fully warmed up (i.e. in C2).  In the interpreter and lower compiler
levels, the cost of checking is more expensive compared to the 
equivalent `volatile boolean` or `AtomicBoolean` approaches.  If 
PerfMark is used in a short-lived program, but many times, the cost
will be higher.

Third, platforms like Android do not have the advanced compiler 
controls that PerfMark uses.  This is not a problem in general, since
The implementation library is usually present in the APK.

To address this tradeoff, PerfMark gives users a choice of 3 different
"enabled" checkers.  (How to pick is outside the scope of this 
document).



## Immutability
TODO

## SecurityManager
TODO

## ThreadReferences
TODO

## ClassLoader Leaks
TODO

## Logging
TODO

## Minimizing ClassLoad
TODO

## Ease of Use
Doing the right thing should be easy, and doing the advanced thing
should be possible.  PerfMark is library for advanced users, who
are okay with the extra verbosity.  In exchange, PerfMark offers
predictable, high-accuracy tracing, that authors can trust.  It is a
tradeoff that _most_ APIs should not make.

That said, the actual Java text that uses the library is only one way
the programmer interacts with a program.  Packaging, distribution, and
execution end up being more important as code needs to be maintained.
PerfMark has a steeper learning curve at the beginning, and a shallower
curve near the end.  The design considerations discussed in this 
document call attention to these challenges (e.g. "How do I fit a 100KB
library into 10KB?)




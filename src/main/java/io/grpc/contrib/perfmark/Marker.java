package io.grpc.contrib.perfmark;

final class Marker {

    static final Marker NONE = new Marker(null, -1, null, null);

    private final String fileName;
    private final int lineNumber;
    private final String className;
    private final String functionName;

    private Marker(String fileName, int lineNumber, String className, String functionName) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.className = className;
        this.functionName = functionName;
    }

    static Marker create() {
        // TODO(carl-mastrangelo): implement
        StackTraceElement[] st = new RuntimeException().fillInStackTrace().getStackTrace();
        if (st.length > 1) {
            return new Marker(st[1].getFileName(), st[1].getLineNumber(), st[1].getClassName(), st[1].getMethodName());
        } else {
            return NONE;
        }
    }
}

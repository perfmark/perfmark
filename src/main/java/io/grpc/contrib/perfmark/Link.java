package io.grpc.contrib.perfmark;

import java.util.concurrent.atomic.AtomicLong;

public final class Link {
    static final Link NONE = new Link(0);

    static final AtomicLong linkIdAlloc = new AtomicLong();

    private final long id;

    Link(long linkId) {
        this.id = linkId;
    }

    public void link() {
        PerfMark.link(id);
    }
}

package com.hitchhikerprod.dragonjars.exec;

public class ExecutionContext {
    // A lambda that must be run when this context completes.
    private final Runnable after;
    private Address nextIP;

    public ExecutionContext(Runnable after) {
        this.after = after;
    }

    public ExecutionContext(Runnable after, Address nextIP) {
        this.after = after;
        this.nextIP = nextIP;
    }

    public ExecutionContext(Runnable after, int segmentId, int address) {
        this.after = after;
        this.nextIP = new Address(segmentId, address);
    }

    public void nextIP(Address ip) {
        this.nextIP = ip;
    }

    public Address nextIP() {
        return this.nextIP;
    }

    public void after() {
        after.run();
    }
}

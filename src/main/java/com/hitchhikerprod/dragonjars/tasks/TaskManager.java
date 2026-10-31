package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class TaskManager {
    private final Interpreter i;

    private final AtomicInteger eyePhase = new AtomicInteger(-1);
    private final AtomicInteger torchPhase = new AtomicInteger(-1);

    private final ReentrantLock monsterAnimationLock = new ReentrantLock();
    private boolean animationEnabled_4d4e = false;
    private int animationMonsterId_4d32 = 0xff;

    private MonsterAnimationTask monsterAnimationTask;
    private EyeAnimationTask eyeAnimationTask;
    private TorchAnimationTask torchAnimationTask;
    private SpellDecayTask spellDecayTask;

    public TaskManager(Interpreter i) {
        this.i = i;
    }

    public int getEyePhase() {
        return eyePhase.get();
    }

    public void setEyePhase(int phase) {
        this.eyePhase.set(phase);
    }

    public int getTorchPhase() {
        return torchPhase.get();
    }

    public void setTorchPhase(int phase) {
        this.torchPhase.set(phase);
    }

    public void checkEye() {
        if (Heap.get(Heap.DETECT_TRAPS_RANGE).read() > 0) {
            if (Objects.isNull(eyeAnimationTask)) {
                eyeAnimationTask = new EyeAnimationTask(i);
                eyeAnimationTask.setOnSucceeded(ev -> eyeAnimationTask = null);
                eyeAnimationTask.setOnFailed(ev -> eyeAnimationTask = null);
                eyeAnimationTask.setOnCancelled(ev -> eyeAnimationTask = null);
                Thread.ofVirtual().start(eyeAnimationTask);
            }
        } else {
            setEyePhase(-1);
        }
    }

    public void checkTorch() {
        if (Heap.get(Heap.LIGHT_RANGE).read() > 0) {
            if (Objects.isNull(torchAnimationTask)) {
                torchAnimationTask = new TorchAnimationTask(i);
                torchAnimationTask.setOnSucceeded(ev -> torchAnimationTask = null);
                torchAnimationTask.setOnFailed(ev -> torchAnimationTask = null);
                torchAnimationTask.setOnCancelled(ev -> torchAnimationTask = null);
                Thread.ofVirtual().start(torchAnimationTask);
            }
        } else {
            setTorchPhase(-1);
        }
    }

    public void stopAllThreads() {
        stopMonsterAnimation();
        spellDecayTask.cancel();
        torchAnimationTask.cancel();
        eyeAnimationTask.cancel();
        while (Objects.nonNull(monsterAnimationTask) ||
                Objects.nonNull(spellDecayTask) ||
                Objects.nonNull(torchAnimationTask) ||
                Objects.nonNull(eyeAnimationTask)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {}
        }
    }

    public void startSpellDecayTask() {
        spellDecayTask = new SpellDecayTask(i);
        spellDecayTask.setOnSucceeded(ev -> spellDecayTask = null);
        spellDecayTask.setOnFailed(ev -> spellDecayTask = null);
        spellDecayTask.setOnCancelled(ev -> spellDecayTask = null);
        Thread.ofVirtual().start(spellDecayTask);
    }

    public boolean isMonsterAnimationEnabled() {
        monsterAnimationLock.lock();
        try {
            return animationEnabled_4d4e;
        } finally {
            monsterAnimationLock.unlock();
        }
    }

    public int activeMonster() {
        monsterAnimationLock.lock();
        try {
            return animationMonsterId_4d32;
        } finally {
            monsterAnimationLock.unlock();
        }
    }

    public void enableMonsterAnimation(int monsterId) {
        monsterAnimationLock.lock();
        try {
            animationEnabled_4d4e = true;
            animationMonsterId_4d32 = monsterId;
        } finally {
            monsterAnimationLock.unlock();
        }
    }

    public void disableMonsterAnimation() {
        monsterAnimationLock.lock();
        try {
            stopMonsterAnimation();
            animationEnabled_4d4e = false;
            animationMonsterId_4d32 = 0xff;
        } finally {
            monsterAnimationLock.unlock();
        }
    }

    public void startMonsterAnimation(MonsterAnimationTask task) {
        stopMonsterAnimation();
        monsterAnimationTask = task;
        Thread.ofVirtual().start(monsterAnimationTask);
    }

    public void stopMonsterAnimation() {
        if (Objects.nonNull(monsterAnimationTask)) {
            monsterAnimationTask.cancel();
            monsterAnimationTask = null;
        }
    }
}

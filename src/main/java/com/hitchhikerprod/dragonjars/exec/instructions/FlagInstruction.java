package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.function.Function;

public record FlagInstruction (
        GetOperand getOp1,
        GetOperand getOp2,
        FlagOperation operation,
        Function<Address, Address> getNextIP
) implements Instruction {
    @FunctionalInterface
    interface GetOperand {
        int apply(Interpreter i, Address ip);
    }

    @FunctionalInterface
    interface FlagOperation {
        void apply(Interpreter i, int op1, int op2);
    }

    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int op1 = getOp1.apply(i, ip);
        final int op2 = getOp2.apply(i, ip);
        operation.apply(i, op1, op2);
        return getNextIP.apply(ip);
    }

    private static final GetOperand GET_AL = (i, ip) -> i.getAL();
    private static final GetOperand GET_ARG_1 = (i, ip) -> i.memory().read(ip.incr(1), 1);
    private static final GetOperand GET_ARG_2 = (i, ip) -> i.memory().read(ip.incr(2), 1);

    private static final FlagOperation SET_FLAG = (i, op1, op2) -> Heap.setFlag(op1, op2);
    private static final FlagOperation CLEAR_FLAG = (i, op1, op2) -> Heap.clearFlag(op1, op2);
    private static final FlagOperation TEST_FLAG = (i, op1, op2) -> {
        final int value = Heap.readFlag(op1, op2);
        i.setCarryFlag(false);
        i.setZeroFlag(value == 0x00);
        i.setSignFlag(value == 0x80);
    };

    private static final Function<Address, Address> IP_ONE_IMMEDIATE = (ip) -> ip.incr(OPCODE + IMMEDIATE);
    private static final Function<Address, Address> IP_TWO_IMMEDIATES = (ip) -> ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);

    public static final FlagInstruction TEST_AL_IMM =
            new FlagInstruction(GET_AL, GET_ARG_1, TEST_FLAG, IP_ONE_IMMEDIATE);
    public static final FlagInstruction SET_AL_IMM =
            new FlagInstruction(GET_AL, GET_ARG_1, SET_FLAG, IP_ONE_IMMEDIATE);
    public static final FlagInstruction CLEAR_AL_IMM =
            new FlagInstruction(GET_AL, GET_ARG_1, CLEAR_FLAG, IP_ONE_IMMEDIATE);

    public static final FlagInstruction TEST_IMM_IMM =
            new FlagInstruction(GET_ARG_1, GET_ARG_2, TEST_FLAG, IP_TWO_IMMEDIATES);
    public static final FlagInstruction SET_IMM_IMM =
            new FlagInstruction(GET_ARG_1, GET_ARG_2, SET_FLAG, IP_TWO_IMMEDIATES);
    public static final FlagInstruction CLEAR_IMM_IMM =
            new FlagInstruction(GET_ARG_1, GET_ARG_2, CLEAR_FLAG, IP_TWO_IMMEDIATES);
}

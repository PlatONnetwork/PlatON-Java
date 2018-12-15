package org.platon.core.vm.program;

import org.apache.commons.lang3.tuple.Pair;
import org.platon.common.utils.ByteComparator;
import org.platon.common.utils.ByteUtil;
import org.platon.common.wrapper.DataWord;
import org.platon.core.Account;
import org.platon.core.Repository;
import org.platon.core.config.BlockchainConfig;
import org.platon.core.config.CommonConfig;
import org.platon.core.config.CoreConfig;
import org.platon.core.config.SystemConfig;
import org.platon.core.db.ContractDetails;
import org.platon.core.transaction.Transaction;
import org.platon.core.utils.Utils;
import org.platon.core.vm.MessageCall;
import org.platon.core.vm.OpCode;
import org.platon.core.vm.PrecompiledContracts;
import org.platon.core.vm.VM;
import org.platon.core.vm.program.invoke.ProgramInvoke;
import org.platon.core.vm.program.invoke.ProgramInvokeFactory;
import org.platon.core.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.platon.core.vm.program.listener.CompositeProgramListener;
import org.platon.core.vm.program.listener.ProgramListenerAware;
import org.platon.core.vm.program.listener.ProgramStorageChangeListener;
import org.platon.core.vm.trace.ProgramTrace;
import org.platon.core.vm.trace.ProgramTraceListener;
import org.platon.crypto.HashUtil;
import org.platon.common.utils.ByteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.*;

import static java.lang.StrictMath.min;
import static java.lang.String.format;
import static java.math.BigInteger.ZERO;
import static org.apache.commons.lang3.ArrayUtils.*;
import static org.bouncycastle.util.encoders.Hex.toHexString;
import static org.platon.common.utils.BIUtil.*;
import static org.platon.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * @author Roman Mandeleil
 * @since 01.06.2014
 */
public class Program {

    private static final Logger logger = LoggerFactory.getLogger("VM");

    /**
     * This attribute defines the number of recursive calls allowed in the EVM
     * Note: For the JVM to reach this level without a StackOverflow exception,
     * ethereumj may need to be started with a JVM argument to increase
     * the stack size. For example: -Xss10m
     */
    private static final int MAX_DEPTH = 1024;

    //Max size for stack checks
    private static final int MAX_STACKSIZE = 1024;

    private Transaction transaction;

    private ProgramInvoke invoke;
    private ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();

    private ProgramOutListener listener;
    private ProgramTraceListener traceListener;
    private ProgramStorageChangeListener storageDiffListener = new ProgramStorageChangeListener();
    private CompositeProgramListener programListener = new CompositeProgramListener();

    private Stack stack;
    private Memory memory;
    private Storage storage;
    private byte[] returnDataBuffer;

    private ProgramResult result = new ProgramResult();
    private ProgramTrace trace = new ProgramTrace();

    private byte[] codeHash;
    private byte[] ops;
    private int pc;
    private byte lastOp;
    private byte previouslyExecutedOp;
    private boolean stopped;
    private ByteArraySet touchedAccounts = new ByteArraySet();

    private ProgramPrecompile programPrecompile;

    CommonConfig commonConfig = CommonConfig.getInstance();

    private CoreConfig coreConfig = CoreConfig.getInstance();

    private final SystemConfig config;

    private final BlockchainConfig blockchainConfig;

    public Program(byte[] ops, ProgramInvoke programInvoke) {
        this(ops, programInvoke, (Transaction) null);
    }

    public Program(byte[] ops, ProgramInvoke programInvoke, SystemConfig config) {
        this(ops, programInvoke, null, config);
    }

    public Program(byte[] ops, ProgramInvoke programInvoke, Transaction transaction) {
        this(ops, programInvoke, transaction, SystemConfig.getDefault());
    }

    public Program(byte[] ops, ProgramInvoke programInvoke, Transaction transaction, SystemConfig config) {
        this(null, ops, programInvoke, transaction, config);
    }

    public Program(byte[] codeHash, byte[] ops, ProgramInvoke programInvoke, Transaction transaction, SystemConfig config) {
        this.config = config;
        this.invoke = programInvoke;
        this.transaction = transaction;

        this.codeHash = codeHash == null || ByteComparator.equals(HashUtil.EMPTY_HASH, codeHash) ? null : codeHash;
        this.ops = nullToEmpty(ops);

        traceListener = new ProgramTraceListener(coreConfig.vmTrace());
        this.memory = setupProgramListener(new Memory());
        this.stack = setupProgramListener(new Stack());
        this.storage = setupProgramListener(new Storage(programInvoke));
        this.trace = new ProgramTrace(coreConfig, programInvoke);
        this.blockchainConfig = config.getBlockchainConfig().getConfigForBlock(programInvoke.getNumber().longValue());
    }

    public ProgramPrecompile getProgramPrecompile() {
        if (programPrecompile == null) {
            if (codeHash != null && commonConfig.precompileSource() != null) {
                programPrecompile = commonConfig.precompileSource().get(codeHash);
            }
            if (programPrecompile == null) {
                programPrecompile = ProgramPrecompile.compile(ops);

                if (codeHash != null && commonConfig.precompileSource() != null) {
                    commonConfig.precompileSource().put(codeHash, programPrecompile);
                }
            }
        }
        return programPrecompile;
    }

    public Program withCommonConfig(CommonConfig commonConfig) {
        this.commonConfig = commonConfig;
        return this;
    }

    public int getCallDeep() {
        return invoke.getCallDeep();
    }

    private InternalTransaction addInternalTx(DataWord energonLimit, byte[] senderAddress, byte[] receiveAddress,
                                              BigInteger value, byte[] data, String note) {

        InternalTransaction result = null;
        if (transaction != null) {
            data = coreConfig.recordInternalTransactionsData() ? data : null;
            result = getResult().addInternalTransaction(transaction.getHash(),getCallDeep(),senderAddress,note,transaction.getTransactionType(),value.toByteArray(),receiveAddress,
                    transaction.getReferenceBlockNum(),transaction.getReferenceBlockHash(),getEnergonPrice(),
                    energonLimit,data);
        }

        return result;
    }

    private <T extends ProgramListenerAware> T setupProgramListener(T programListenerAware) {
        if (programListener.isEmpty()) {
            programListener.addListener(traceListener);
            programListener.addListener(storageDiffListener);
        }

        programListenerAware.setProgramListener(programListener);

        return programListenerAware;
    }

    public Map<DataWord, DataWord> getStorageDiff() {
        return storageDiffListener.getDiff();
    }

    public byte getOp(int pc) {
        return (getLength(ops) <= pc) ? 0 : ops[pc];
    }

    public byte getCurrentOp() {
        return isEmpty(ops) ? 0 : ops[pc];
    }

    /**
     * Last Op can only be set publicly (no getLastOp method), is used for logging.
     */
    public void setLastOp(byte op) {
        this.lastOp = op;
    }

    /**
     * Should be set only after the OP is fully executed.
     */
    public void setPreviouslyExecutedOp(byte op) {
        this.previouslyExecutedOp = op;
    }

    /**
     * Returns the last fully executed OP.
     */
    public byte getPreviouslyExecutedOp() {
        return this.previouslyExecutedOp;
    }

    public void stackPush(byte[] data) {
        stackPush(DataWord.of(data));
    }

    public void stackPushZero() {
        stackPush(DataWord.ZERO);
    }

    public void stackPushOne() {
        DataWord stackWord = DataWord.ONE;
        stackPush(stackWord);
    }

    public void stackPush(DataWord stackWord) {
        verifyStackOverflow(0, 1); //Sanity Check
        stack.push(stackWord);
    }

    public Stack getStack() {
        return this.stack;
    }

    public int getPC() {
        return pc;
    }

    public void setPC(DataWord pc) {
        this.setPC(pc.intValue());
    }

    public void setPC(int pc) {
        this.pc = pc;

        if (this.pc >= ops.length) {
            stop();
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        stopped = true;
    }

    public void setHReturn(byte[] buff) {
        getResult().setHReturn(buff);
    }

    public void step() {
        setPC(pc + 1);
    }

    public byte[] sweep(int n) {

        if (pc + n > ops.length)
            stop();

        byte[] data = Arrays.copyOfRange(ops, pc, pc + n);
        pc += n;
        if (pc >= ops.length) stop();

        return data;
    }

    public DataWord stackPop() {
        return stack.pop();
    }

    /**
     * Verifies that the stack is at least <code>stackSize</code>
     *
     * @param stackSize int
     * @throws StackTooSmallException If the stack is
     *                                smaller than <code>stackSize</code>
     */
    public void verifyStackSize(int stackSize) {
        if (stack.size() < stackSize) {
            throw Program.Exception.tooSmallStack(stackSize, stack.size());
        }
    }

    public void verifyStackOverflow(int argsReqs, int returnReqs) {
        if ((stack.size() - argsReqs + returnReqs) > MAX_STACKSIZE) {
            throw new StackTooLargeException("Expected: overflow " + MAX_STACKSIZE + " elements stack limit");
        }
    }

    public int getMemSize() {
        return memory.size();
    }

    public void memorySave(DataWord addrB, DataWord value) {
        memory.write(addrB.intValue(), value.getData(), value.getData().length, false);
    }

    public void memorySaveLimited(int addr, byte[] data, int dataSize) {
        memory.write(addr, data, dataSize, true);
    }

    public void memorySave(int addr, byte[] value) {
        memory.write(addr, value, value.length, false);
    }

    public void memoryExpand(DataWord outDataOffs, DataWord outDataSize) {
        if (!outDataSize.isZero()) {
            memory.extend(outDataOffs.intValue(), outDataSize.intValue());
        }
    }

    /**
     * Allocates a piece of memory and stores value at given offset address
     *
     * @param addr      is the offset address
     * @param allocSize size of memory needed to write
     * @param value     the data to write to memory
     */
    public void memorySave(int addr, int allocSize, byte[] value) {
        memory.extendAndWrite(addr, allocSize, value);
    }


    public DataWord memoryLoad(DataWord addr) {
        return memory.readWord(addr.intValue());
    }

    public DataWord memoryLoad(int address) {
        return memory.readWord(address);
    }

    public byte[] memoryChunk(int offset, int size) {
        return memory.read(offset, size);
    }

    /**
     * Allocates extra memory in the program for
     * a specified size, calculated from a given offset
     *
     * @param offset the memory address offset
     * @param size   the number of bytes to allocate
     */
    public void allocateMemory(int offset, int size) {
        memory.extend(offset, size);
    }


    public void suicide(DataWord obtainerAddress) {

        byte[] owner = getOwnerAddress().getLast20Bytes();
        byte[] obtainer = obtainerAddress.getLast20Bytes();
        BigInteger balance = getStorage().getBalance(owner);

        if (logger.isInfoEnabled())
            logger.info("Transfer to: [{}] heritage: [{}]",
                    toHexString(obtainer),
                    balance);

        addInternalTx( null, owner, obtainer, balance, null, "suicide");

        if (ByteComparator.compareTo(owner, 0, 20, obtainer, 0, 20) == 0) {
            // if owner == obtainer just zeroing account according to Yellow Paper
            getStorage().addBalance(owner, balance.negate());
        } else {
            Utils.transfer(getStorage(), owner, obtainer, balance);
        }

        getResult().addDeleteAccount(this.getOwnerAddress());
    }

    public Repository getStorage() {
        return this.storage;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void createContract(DataWord value, DataWord memStart, DataWord memSize) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            return;
        }

        byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
        BigInteger endowment = value.value();
        if (isNotCovers(getStorage().getBalance(senderAddress), endowment)) {
            stackPushZero();
            return;
        }

        // [1] FETCH THE CODE FROM THE MEMORY
        byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());

        if (logger.isInfoEnabled())
            logger.info("creating a new contract inside contract run: [{}]", toHexString(senderAddress));

        BlockchainConfig blockchainConfig = config.getBlockchainConfig().getConfigForBlock(getNumber().longValue());
        //  actual energon subtract
        DataWord energonLimit = blockchainConfig.getCreateEnergon(getEnergon());
        spendEnergon(energonLimit.longValue(), "internal call");

        // [2] CREATE THE CONTRACT ADDRESS
        //TODO 需要一个新的地址生成方式
        byte[] newAddress = null;//HashUtil.calcNewAddr(getOwnerAddress().getLast20Bytes(), nonce);

        Account existingAddr = getStorage().getAccount(newAddress);
        boolean contractAlreadyExists = existingAddr != null && existingAddr.isContractExist();

        if (byTestingSuite()) {
            // This keeps track of the contracts created for a test
            getResult().addCallCreate(programCode, EMPTY_BYTE_ARRAY,
                    energonLimit.getNoLeadZeroesData(),
                    value.getNoLeadZeroesData());
        }

        // [3] UPDATE THE NONCE
        // (THIS STAGE IS NOT REVERTED BY ANY EXCEPTION)
        Repository track = getStorage().startTracking();

        //In case of hashing collisions, check for any balance before createAccount()
        BigInteger oldBalance = track.getBalance(newAddress);
        track.createAccount(newAddress);
//        if (blockchainConfig.eip161()) {
//            track.increaseNonce(newAddress);
//        }
        track.addBalance(newAddress, oldBalance);

        // [4] TRANSFER THE BALANCE
        BigInteger newBalance = ZERO;
        if (!byTestingSuite()) {
            track.addBalance(senderAddress, endowment.negate());
            newBalance = track.addBalance(newAddress, endowment);
        }


        // [5] COOK THE INVOKE AND EXECUTE
        InternalTransaction internalTx = addInternalTx(getEnergonLimit(), senderAddress, null, endowment, programCode, "create");
        ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
                this, DataWord.of(newAddress), getOwnerAddress(), value, energonLimit,
                newBalance, null, track, this.invoke.getBlockStore(), false, byTestingSuite());

        ProgramResult result = ProgramResult.createEmpty();

        if (contractAlreadyExists) {
            result.setException(new BytecodeExecutionException("Trying to create a contract with existing contract address: 0x" + toHexString(newAddress)));
        } else if (isNotEmpty(programCode)) {
            VM vm = new VM(coreConfig);
            Program program = new Program(programCode, programInvoke, internalTx, config).withCommonConfig(commonConfig);
            vm.play(program);
            result = program.getResult();
        }

        // 4. CREATE THE CONTRACT OUT OF RETURN
        byte[] code = result.getHReturn();

        long storageCost = getLength(code) * getBlockchainConfig().getEnergonCost().getCREATE_DATA();
        long afterSpend = programInvoke.getEnergon().longValue() - storageCost - result.getEnergonUsed();
        if (afterSpend < 0) {
            if (!blockchainConfig.getConstants().createEmptyContractOnOOG()) {
                result.setException(Program.Exception.notEnoughSpendingEnergon("No Energon to return just created contract",
                        storageCost, this));
            } else {
                track.saveCode(newAddress, EMPTY_BYTE_ARRAY);
            }
        } else if (getLength(code) > blockchainConfig.getConstants().getMAX_CONTRACT_SZIE()) {
            result.setException(Program.Exception.notEnoughSpendingEnergon("Contract size too large: " + getLength(result.getHReturn()),
                    storageCost, this));
        } else if (!result.isRevert()){
            result.spendEnergon(storageCost);
            track.saveCode(newAddress, code);
        }

        getResult().merge(result);

        if (result.getException() != null || result.isRevert()) {
            logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
                    toHexString(newAddress),
                    result.getException());

            internalTx.reject();
            result.rejectInternalTransactions();

            track.rollback();
            stackPushZero();

            if (result.getException() != null) {
                return;
            } else {
                returnDataBuffer = result.getHReturn();
            }
        } else {
            if (!byTestingSuite())
                track.commit();

            // IN SUCCESS PUSH THE ADDRESS INTO THE STACK
            stackPush(DataWord.of(newAddress));
        }

        // 5. REFUND THE REMAIN Energon
        long refundEnergon = energonLimit.longValue() - result.getEnergonUsed();
        if (refundEnergon > 0) {
            refundEnergon(refundEnergon, "remain energon from the internal call");
            if (logger.isInfoEnabled()) {
                logger.info("The remaining energon is refunded, account: [{}], energon: [{}] ",
                        toHexString(getOwnerAddress().getLast20Bytes()),
                        refundEnergon);
            }
        }
    }

    /**
     * That method is for internal code invocations
     * <p/>
     * - Normal calls invoke a specified contract which updates itself
     * - Stateless calls invoke code from another contract, within the context of the caller
     *
     * @param msg is the message call object
     */
    public void callToAddress(MessageCall msg) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            refundEnergon(msg.getEnergon().longValue(), " call deep limit reach");
            return;
        }

        byte[] data = memoryChunk(msg.getInDataOffs().intValue(), msg.getInDataSize().intValue());

        // FETCH THE SAVED STORAGE
        byte[] codeAddress = msg.getCodeAddress().getLast20Bytes();
        byte[] senderAddress = getOwnerAddress().getLast20Bytes();
        byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;

        if (logger.isInfoEnabled())
            logger.info(msg.getType().name() + " for existing contract: address: [{}], outDataOffs: [{}], outDataSize: [{}]  ",
                    toHexString(contextAddress), msg.getOutDataOffs().longValue(), msg.getOutDataSize().longValue());

        Repository track = getStorage().startTracking();

        // 2.1 PERFORM THE VALUE (endowment) PART
        BigInteger endowment = msg.getEndowment().value();
        BigInteger senderBalance = track.getBalance(senderAddress);
        if (isNotCovers(senderBalance, endowment)) {
            stackPushZero();
            refundEnergon(msg.getEnergon().longValue(), "refund energon from message call");
            return;
        }


        // FETCH THE CODE
        byte[] programCode = getStorage().isExist(codeAddress) ? getStorage().getCode(codeAddress) : EMPTY_BYTE_ARRAY;


        BigInteger contextBalance = ZERO;
        if (byTestingSuite()) {
            // This keeps track of the calls created for a test
            getResult().addCallCreate(data, contextAddress,
                    msg.getEnergon().getNoLeadZeroesData(),
                    msg.getEndowment().getNoLeadZeroesData());
        } else {
            track.addBalance(senderAddress, endowment.negate());
            contextBalance = track.addBalance(contextAddress, endowment);
        }

        // CREATE CALL INTERNAL TRANSACTION
        InternalTransaction internalTx = addInternalTx(getEnergonLimit(), senderAddress, contextAddress, endowment, data, "call");

        ProgramResult result = null;
        if (isNotEmpty(programCode)) {
            ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
                    this, DataWord.of(contextAddress),
                    msg.getType().callIsDelegate() ? getCallerAddress() : getOwnerAddress(),
                    msg.getType().callIsDelegate() ? getCallValue() : msg.getEndowment(),
                    msg.getEnergon(), contextBalance, data, track, this.invoke.getBlockStore(),
                    msg.getType().callIsStatic() || isStaticCall(), byTestingSuite());

            VM vm = new VM(coreConfig);
            Program program = new Program(getStorage().getCodeHash(codeAddress), programCode, programInvoke, internalTx, config).withCommonConfig(commonConfig);
            vm.play(program);
            result = program.getResult();

            getTrace().merge(program.getTrace());
            getResult().merge(result);

            if (result.getException() != null || result.isRevert()) {
                logger.debug("contract run halted by Exception: contract: [{}], exception: [{}]",
                        toHexString(contextAddress),
                        result.getException());

                internalTx.reject();
                result.rejectInternalTransactions();

                track.rollback();
                stackPushZero();

                if (result.getException() != null) {
                    return;
                }
            } else {
                // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
                track.commit();
                stackPushOne();
            }

            if (byTestingSuite()) {
                logger.info("Testing run, skipping storage diff listener");
            } else if (Arrays.equals(transaction.getReceiveAddress(), internalTx.getReceiveAddress())) {
                storageDiffListener.merge(program.getStorageDiff());
            }
        } else {
            // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
            track.commit();
            stackPushOne();
        }

        // 3. APPLY RESULTS: result.getHReturn() into out_memory allocated
        if (result != null) {
            byte[] buffer = result.getHReturn();
            int offset = msg.getOutDataOffs().intValue();
            int size = msg.getOutDataSize().intValue();

            memorySaveLimited(offset, buffer, size);

            returnDataBuffer = buffer;
        }

        // 5. REFUND THE REMAIN Energon
        if (result != null) {
            BigInteger refundEnergon = msg.getEnergon().value().subtract(toBI(result.getEnergonUsed()));
            if (isPositive(refundEnergon)) {
                refundEnergon(refundEnergon.longValue(), "remaining Energon from the internal call");
                if (logger.isInfoEnabled())
                    logger.info("The remaining Energon refunded, account: [{}], Energon: [{}] ",
                            toHexString(senderAddress),
                            refundEnergon.toString());
            }
        } else {
            refundEnergon(msg.getEnergon().longValue(), "remaining Energon from the internal call");
        }
    }

    public void spendEnergon(long energonValue, String cause) {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] Spent for cause: [{}], Energon: [{}]", invoke.hashCode(), cause, energonValue);
        }

        if (getEnergonLong() < energonValue) {
            throw Program.Exception.notEnoughSpendingEnergon(cause, energonValue, this);
        }
        getResult().spendEnergon(energonValue);
    }

    public void spendAllEnergon() {
        spendEnergon(getEnergon().longValue(), "Spending all remaining");
    }

    public void refundEnergon(long energonValue, String cause) {
        logger.info("[{}] Refund for cause: [{}], Energon: [{}]", invoke.hashCode(), cause, energonValue);
        getResult().refundEnergon(energonValue);
    }

    public void futureRefundEnergon(long energonValue) {
        logger.info("Future refund added: [{}]", energonValue);
        getResult().addFutureRefund(energonValue);
    }

    public void resetFutureRefund() {
        getResult().resetFutureRefund();
    }

    public void storageSave(DataWord word1, DataWord word2) {
        storageSave(word1.getData(), word2.getData());
    }

    public void storageSave(byte[] key, byte[] val) {
        DataWord keyWord = DataWord.of(key);
        DataWord valWord = DataWord.of(val);
        getStorage().addStorageRow(getOwnerAddress().getLast20Bytes(), keyWord, valWord);
    }

    public byte[] getCode() {
        return ops;
    }

    public byte[] getCodeAt(DataWord address) {
        byte[] code = invoke.getRepository().getCode(address.getLast20Bytes());
        return nullToEmpty(code);
    }

    public byte[] getCodeHashAt(DataWord address) {
        byte[] code = invoke.getRepository().getCodeHash(address.getLast20Bytes());
        return nullToEmpty(code);
    }

    public DataWord getOwnerAddress() {
        return invoke.getOwnerAddress();
    }

    public DataWord getBlockHash(int index) {
        return index < this.getNumber().longValue() && index >= Math.max(256, this.getNumber().intValue()) - 256 ?
                DataWord.of(this.invoke.getBlockStore().getBlockHashByNumber(index, getPrevHash().getData())) :
                DataWord.ZERO;
    }

    public DataWord getBalance(DataWord address) {
        BigInteger balance = getStorage().getBalance(address.getLast20Bytes());
        return DataWord.of(balance.toByteArray());
    }

    public DataWord getOriginAddress() {
        return invoke.getOriginAddress();
    }

    public DataWord getCallerAddress() {
        return invoke.getCallerAddress();
    }

    public DataWord getEnergonPrice() {
        return invoke.getMinEnergonPrice();
    }

    public long getEnergonLong() {
        return invoke.getEnergonLong() - getResult().getEnergonUsed();
    }

    public DataWord getEnergon() {
        return DataWord.of(invoke.getEnergonLong() - getResult().getEnergonUsed());
    }

    public DataWord getCallValue() {
        return invoke.getCallValue();
    }

    public DataWord getDataSize() {
        return invoke.getDataSize();
    }

    public DataWord getDataValue(DataWord index) {
        return invoke.getDataValue(index);
    }

    public byte[] getDataCopy(DataWord offset, DataWord length) {
        return invoke.getDataCopy(offset, length);
    }

    public DataWord getReturnDataBufferSize() {
        return DataWord.of(getReturnDataBufferSizeI());
    }

    private int getReturnDataBufferSizeI() {
        return returnDataBuffer == null ? 0 : returnDataBuffer.length;
    }

    public byte[] getReturnDataBufferData(DataWord off, DataWord size) {
        if ((long) off.intValueSafe() + size.intValueSafe() > getReturnDataBufferSizeI()) return null;
        return returnDataBuffer == null ? new byte[0] :
                Arrays.copyOfRange(returnDataBuffer, off.intValueSafe(), off.intValueSafe() + size.intValueSafe());
    }

    public DataWord storageLoad(DataWord key) {
        return getStorage().getStorageValue(getOwnerAddress().getLast20Bytes(), key);
    }

    public DataWord getPrevHash() {
        return invoke.getPrevHash();
    }

    public DataWord getCoinbase() {
        return invoke.getCoinbase();
    }

    public DataWord getTimestamp() {
        return invoke.getTimestamp();
    }

    public DataWord getNumber() {
        return invoke.getNumber();
    }

    public BlockchainConfig getBlockchainConfig() {
        return blockchainConfig;
    }

    public DataWord getDifficulty() {
        return invoke.getDifficulty();
    }

    public DataWord getEnergonLimit() {
            return invoke.getEnergonLimit();
    }

    public boolean isStaticCall() {
        return invoke.isStaticCall();
    }

    public ProgramResult getResult() {
        return result;
    }

    public void setRuntimeFailure(RuntimeException e) {
        getResult().setException(e);
    }

    public String memoryToString() {
        return memory.toString();
    }

    public void fullTrace() {

        if (logger.isTraceEnabled() || listener != null) {

            StringBuilder stackData = new StringBuilder();
            for (int i = 0; i < stack.size(); ++i) {
                stackData.append(" ").append(stack.get(i));
                if (i < stack.size() - 1) stackData.append("\n");
            }

            if (stackData.length() > 0) stackData.insert(0, "\n");

            ContractDetails contractDetails = getStorage().
                    getContractDetails(getOwnerAddress().getLast20Bytes());
            StringBuilder storageData = new StringBuilder();
            if (contractDetails != null) {
                try {
                    List<DataWord> storageKeys = new ArrayList<>(contractDetails.getStorage().keySet());
                    Collections.sort(storageKeys);
                    for (DataWord key : storageKeys) {
                        storageData.append(" ").append(key).append(" -> ").
                                append(contractDetails.getStorage().get(key)).append("\n");
                    }
                    if (storageData.length() > 0) storageData.insert(0, "\n");
                } catch (java.lang.Exception e) {
                    storageData.append("Failed to print storage: ").append(e.getMessage());
                }
            }

            StringBuilder memoryData = new StringBuilder();
            StringBuilder oneLine = new StringBuilder();
            if (memory.size() > 320)
                memoryData.append("... Memory Folded.... ")
                        .append("(")
                        .append(memory.size())
                        .append(") bytes");
            else
                for (int i = 0; i < memory.size(); ++i) {

                    byte value = memory.readByte(i);
                    oneLine.append(ByteUtil.oneByteToHexString(value)).append(" ");

                    if ((i + 1) % 16 == 0) {
                        String tmp = format("[%4s]-[%4s]", Integer.toString(i - 15, 16),
                                Integer.toString(i, 16)).replace(" ", "0");
                        memoryData.append("").append(tmp).append(" ");
                        memoryData.append(oneLine);
                        if (i < memory.size()) memoryData.append("\n");
                        oneLine.setLength(0);
                    }
                }
            if (memoryData.length() > 0) memoryData.insert(0, "\n");

            StringBuilder opsString = new StringBuilder();
            for (int i = 0; i < ops.length; ++i) {

                String tmpString = Integer.toString(ops[i] & 0xFF, 16);
                tmpString = tmpString.length() == 1 ? "0" + tmpString : tmpString;

                if (i != pc)
                    opsString.append(tmpString);
                else
                    opsString.append(" >>").append(tmpString).append("");

            }
            if (pc >= ops.length) opsString.append(" >>");
            if (opsString.length() > 0) opsString.insert(0, "\n ");

            logger.trace(" -- OPS --     {}", opsString);
            logger.trace(" -- STACK --   {}", stackData);
            logger.trace(" -- MEMORY --  {}", memoryData);
            logger.trace(" -- STORAGE -- {}\n", storageData);
            logger.trace("\n  Spent Energon: [{}]/[{}]\n  Left Energon:  [{}]\n",
                    getResult().getEnergonUsed(),
                    invoke.getEnergon().longValue(),
                    getEnergon().longValue());

            StringBuilder globalOutput = new StringBuilder("\n");
            if (stackData.length() > 0) stackData.append("\n");

            if (pc != 0)
                globalOutput.append("[Op: ").append(OpCode.code(lastOp).name()).append("]\n");

            globalOutput.append(" -- OPS --     ").append(opsString).append("\n");
            globalOutput.append(" -- STACK --   ").append(stackData).append("\n");
            globalOutput.append(" -- MEMORY --  ").append(memoryData).append("\n");
            globalOutput.append(" -- STORAGE -- ").append(storageData).append("\n");

            if (getResult().getHReturn() != null)
                globalOutput.append("\n  HReturn: ").append(
                        toHexString(getResult().getHReturn()));

            // sophisticated assumption that msg.data != codedata
            // means we are calling the contract not creating it
            byte[] txData = invoke.getDataCopy(DataWord.ZERO, getDataSize());
            if (!Arrays.equals(txData, ops))
                globalOutput.append("\n  msg.data: ").append(toHexString(txData));
            globalOutput.append("\n\n  Spent Energon: ").append(getResult().getEnergonUsed());

            if (listener != null)
                listener.output(globalOutput.toString());
        }
    }

    public void saveOpTrace() {
        if (this.pc < ops.length) {
            trace.addOp(ops[pc], pc, getCallDeep(), getEnergon(), traceListener.resetActions());
        }
    }

    public ProgramTrace getTrace() {
        return trace;
    }

    static String formatBinData(byte[] binData, int startPC) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < binData.length; i += 16) {
            ret.append(Utils.align("" + Integer.toHexString(startPC + (i)) + ":", ' ', 8, false));
            ret.append(toHexString(binData, i, min(16, binData.length - i))).append('\n');
        }
        return ret.toString();
    }

    public static String stringifyMultiline(byte[] code) {
        int index = 0;
        StringBuilder sb = new StringBuilder();
        BitSet mask = buildReachableBytecodesMask(code);
        ByteArrayOutputStream binData = new ByteArrayOutputStream();
        int binDataStartPC = -1;

        while (index < code.length) {
            final byte opCode = code[index];
            OpCode op = OpCode.code(opCode);

            if (!mask.get(index)) {
                if (binDataStartPC == -1) {
                    binDataStartPC = index;
                }
                binData.write(code[index]);
                index++;
                if (index < code.length) continue;
            }

            if (binDataStartPC != -1) {
                sb.append(formatBinData(binData.toByteArray(), binDataStartPC));
                binDataStartPC = -1;
                binData = new ByteArrayOutputStream();
                if (index == code.length) continue;
            }

            sb.append(Utils.align("" + Integer.toHexString(index) + ":", ' ', 8, false));

            if (op == null) {
                sb.append("<UNKNOWN>: ").append(0xFF & opCode).append("\n");
                index++;
                continue;
            }

            if (op.name().startsWith("PUSH")) {
                sb.append(' ').append(op.name()).append(' ');

                int nPush = op.val() - OpCode.PUSH1.val() + 1;
                byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
                BigInteger bi = new BigInteger(1, data);
                sb.append("0x").append(bi.toString(16));
                if (bi.bitLength() <= 32) {
                    sb.append(" (").append(new BigInteger(1, data).toString()).append(") ");
                }

                index += nPush + 1;
            } else {
                sb.append(' ').append(op.name());
                index++;
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    static class ByteCodeIterator {
        byte[] code;
        int pc;

        public ByteCodeIterator(byte[] code) {
            this.code = code;
        }

        public void setPC(int pc) {
            this.pc = pc;
        }

        public int getPC() {
            return pc;
        }

        public OpCode getCurOpcode() {
            return pc < code.length ? OpCode.code(code[pc]) : null;
        }

        public boolean isPush() {
            return getCurOpcode() != null ? getCurOpcode().name().startsWith("PUSH") : false;
        }

        public byte[] getCurOpcodeArg() {
            if (isPush()) {
                int nPush = getCurOpcode().val() - OpCode.PUSH1.val() + 1;
                byte[] data = Arrays.copyOfRange(code, pc + 1, pc + nPush + 1);
                return data;
            } else {
                return new byte[0];
            }
        }

        public boolean next() {
            pc += 1 + getCurOpcodeArg().length;
            return pc < code.length;
        }
    }

    static BitSet buildReachableBytecodesMask(byte[] code) {
        NavigableSet<Integer> gotos = new TreeSet<>();
        ByteCodeIterator it = new ByteCodeIterator(code);
        BitSet ret = new BitSet(code.length);
        int lastPush = 0;
        int lastPushPC = 0;
        do {
            ret.set(it.getPC()); // reachable bytecode
            if (it.isPush()) {
                lastPush = new BigInteger(1, it.getCurOpcodeArg()).intValue();
                lastPushPC = it.getPC();
            }
            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.JUMPI) {
                if (it.getPC() != lastPushPC + 1) {
                    // some PC arithmetic we totally can't deal with
                    // assuming all bytecodes are reachable as a fallback
                    ret.set(0, code.length);
                    return ret;
                }
                int jumpPC = lastPush;
                if (!ret.get(jumpPC)) {
                    // code was not explored yet
                    gotos.add(jumpPC);
                }
            }
            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.RETURN ||
                    it.getCurOpcode() == OpCode.STOP) {
                if (gotos.isEmpty()) break;
                it.setPC(gotos.pollFirst());
            }
        } while (it.next());
        return ret;
    }

    public static String stringify(byte[] code) {
        int index = 0;
        StringBuilder sb = new StringBuilder();
        BitSet mask = buildReachableBytecodesMask(code);
        String binData = "";

        while (index < code.length) {
            final byte opCode = code[index];
            OpCode op = OpCode.code(opCode);

            if (op == null) {
                sb.append(" <UNKNOWN>: ").append(0xFF & opCode).append(" ");
                index++;
                continue;
            }

            if (op.name().startsWith("PUSH")) {
                sb.append(' ').append(op.name()).append(' ');

                int nPush = op.val() - OpCode.PUSH1.val() + 1;
                byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
                BigInteger bi = new BigInteger(1, data);
                sb.append("0x").append(bi.toString(16)).append(" ");

                index += nPush + 1;
            } else {
                sb.append(' ').append(op.name());
                index++;
            }
        }

        return sb.toString();
    }


    public void addListener(ProgramOutListener listener) {
        this.listener = listener;
    }

    public int verifyJumpDest(DataWord nextPC) {
        if (nextPC.bytesOccupied() > 4) {
            throw Program.Exception.badJumpDestination(-1);
        }
        int ret = nextPC.intValue();
        if (!getProgramPrecompile().hasJumpDest(ret)) {
            throw Program.Exception.badJumpDestination(ret);
        }
        return ret;
    }

    public void callToPrecompiledAddress(MessageCall msg, PrecompiledContracts.PrecompiledContract contract) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            this.refundEnergon(msg.getEnergon().longValue(), " call deep limit reach");
            return;
        }

        Repository track = getStorage().startTracking();

        byte[] senderAddress = this.getOwnerAddress().getLast20Bytes();
        byte[] codeAddress = msg.getCodeAddress().getLast20Bytes();
        byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;


        BigInteger endowment = msg.getEndowment().value();
        BigInteger senderBalance = track.getBalance(senderAddress);
        if (senderBalance.compareTo(endowment) < 0) {
            stackPushZero();
            this.refundEnergon(msg.getEnergon().longValue(), "refund Energon from message call");
            return;
        }

        byte[] data = this.memoryChunk(msg.getInDataOffs().intValue(),
                msg.getInDataSize().intValue());

        // Charge for endowment - is not reversible by rollback
        Utils.transfer(track, senderAddress, contextAddress, msg.getEndowment().value());

        if (byTestingSuite()) {
            // This keeps track of the calls created for a test
            this.getResult().addCallCreate(data,
                    msg.getCodeAddress().getLast20Bytes(),
                    msg.getEnergon().getNoLeadZeroesData(),
                    msg.getEndowment().getNoLeadZeroesData());

            stackPushOne();
            return;
        }


        long requiredEnergon = contract.getEnergonForData(data);
        if (requiredEnergon > msg.getEnergon().longValue()) {

            this.refundEnergon(0, "call pre-compiled"); //matches cpp logic
            this.stackPushZero();
            track.rollback();
        } else {

            if (logger.isDebugEnabled())
                logger.debug("Call {}(data = {})", contract.getClass().getSimpleName(), toHexString(data));

            Pair<Boolean, byte[]> out = contract.execute(data);

            if (out.getLeft()) { // success
                this.refundEnergon(msg.getEnergon().longValue() - requiredEnergon, "call pre-compiled");
                this.stackPushOne();
                returnDataBuffer = out.getRight();
                track.commit();
            } else {
                // spend all Energon on failure, push zero and revert state changes
                this.refundEnergon(0, "call pre-compiled");
                this.stackPushZero();
                track.rollback();
            }

            this.memorySave(msg.getOutDataOffs().intValue(), msg.getOutDataSize().intValueSafe(), out.getRight());
        }
    }

    public boolean byTestingSuite() {
        return invoke.byTestingSuite();
    }

    public interface ProgramOutListener {
        void output(String out);
    }

    /**
     * Denotes problem when executing Ethereum bytecode.
     * From blockchain and peer perspective this is quite normal situation
     * and doesn't mean exceptional situation in terms of the program execution
     */
    @SuppressWarnings("serial")
    public static class BytecodeExecutionException extends RuntimeException {
        public BytecodeExecutionException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    public static class OutOfEnergonException extends BytecodeExecutionException {

        public OutOfEnergonException(String message, Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class IllegalOperationException extends BytecodeExecutionException {

        public IllegalOperationException(String message, Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class BadJumpDestinationException extends BytecodeExecutionException {

        public BadJumpDestinationException(String message, Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class StackTooSmallException extends BytecodeExecutionException {

        public StackTooSmallException(String message, Object... args) {
            super(format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class ReturnDataCopyIllegalBoundsException extends BytecodeExecutionException {
        public ReturnDataCopyIllegalBoundsException(DataWord off, DataWord size, long returnDataSize) {
            super(String.format("Illegal RETURNDATACOPY arguments: offset (%s) + size (%s) > RETURNDATASIZE (%d)", off, size, returnDataSize));
        }
    }

    @SuppressWarnings("serial")
    public static class StaticCallModificationException extends BytecodeExecutionException {
        public StaticCallModificationException() {
            super("Attempt to call a state modifying opcode inside STATICCALL");
        }
    }


    public static class Exception {

        public static OutOfEnergonException notEnoughOpEnergon(OpCode op, long opEnergon, long programEnergon) {
            return new OutOfEnergonException("Not enough Energon for '%s' operation executing: opEnergon[%d], programEnergon[%d];", op, opEnergon, programEnergon);
        }

        public static OutOfEnergonException notEnoughOpEnergon(OpCode op, DataWord opEnergon, DataWord programEnergon) {
            return notEnoughOpEnergon(op, opEnergon.longValue(), programEnergon.longValue());
        }

        public static OutOfEnergonException notEnoughOpEnergon(OpCode op, BigInteger opEnergon, BigInteger programEnergon) {
            return notEnoughOpEnergon(op, opEnergon.longValue(), programEnergon.longValue());
        }

        public static OutOfEnergonException notEnoughSpendingEnergon(String cause, long energonValue, Program program) {
            return new OutOfEnergonException("Not enough Energon for '%s' cause spending: invokeEnergon[%d], Energon[%d], usedEnergon[%d];",
                    cause, program.invoke.getEnergon().longValue(), energonValue, program.getResult().getEnergonUsed());
        }

        public static OutOfEnergonException EnergonOverflow(BigInteger actualEnergon, BigInteger energonLimit) {
            return new OutOfEnergonException("Energon value overflow: actualEnergon[%d], EnergonLimit[%d];", actualEnergon.longValue(), energonLimit.longValue());
        }

        public static IllegalOperationException invalidOpCode(byte... opCode) {
            return new IllegalOperationException("Invalid operation code: opCode[%s];", toHexString(opCode, 0, 1));
        }

        public static BadJumpDestinationException badJumpDestination(int pc) {
            return new BadJumpDestinationException("Operation with pc isn't 'JUMPDEST': PC[%d];", pc);
        }

        public static StackTooSmallException tooSmallStack(int expectedSize, int actualSize) {
            return new StackTooSmallException("Expected stack size %d but actual %d;", expectedSize, actualSize);
        }
    }

    @SuppressWarnings("serial")
    public class StackTooLargeException extends BytecodeExecutionException {
        public StackTooLargeException(String message) {
            super(message);
        }
    }

    /**
     * used mostly for testing reasons
     */
    public byte[] getMemory() {
        return memory.read(0, memory.size());
    }

    /**
     * used mostly for testing reasons
     */
    public void initMem(byte[] data) {
        this.memory.write(0, data, data.length, false);
    }

}

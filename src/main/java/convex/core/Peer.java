package convex.core;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import convex.core.crypto.AKeyPair;
import convex.core.data.ACell;
import convex.core.data.AMap;
import convex.core.data.AVector;
import convex.core.data.AccountKey;
import convex.core.data.Address;
import convex.core.data.BlobMap;
import convex.core.data.Hash;
import convex.core.data.Keyword;
import convex.core.data.Keywords;
import convex.core.data.Maps;
import convex.core.data.Ref;
import convex.core.data.SignedData;
import convex.core.data.Vectors;
import convex.core.data.prim.CVMLong;
import convex.core.exceptions.BadSignatureException;
import convex.core.exceptions.InvalidDataException;
import convex.core.exceptions.TODOException;
import convex.core.init.Init;
import convex.core.lang.AOp;
import convex.core.lang.Context;
import convex.core.store.AStore;
import convex.core.store.Stores;
import convex.core.transactions.ATransaction;
import convex.core.util.Utils;

/**
 * <p>
 * Immutable class representing the encapsulated state of a Peer
 * </p>
 *
 * SECURITY:
 * <ul>
 * <li>Needs to contain the Peer's unlocked private key for online signing.</li>
 * <li>Manages Peer state transitions given external events. Must do so
 * correctly.</li>
 * </ul>
 *
 * <p>
 * Must have at least one state, the initial state. New states will be added as
 * consensus updates happen.
 * </p>
 *
 *
 * "Don't worry about what anybody else is going to do. The best way to predict
 * the future is to invent it." - Alan Kay
 */
public class Peer {
	/** This Peer's key */
	private final AccountKey peerKey;

	/** This Peer's key pair */
	private final AKeyPair keyPair;

	/** The latest merged belief */
	private final SignedData<Belief> belief;

	/**
	 * The latest observed timestamp. This is increased by the Server polling the
	 * local clock.
	 */
	private final long timestamp;

	/**
	 * Vector of states
	 */
	private final AVector<State> states;

	/**
	 * Vector of results
	 */
	private final AVector<BlockResult> blockResults;

	private Peer(AKeyPair kp, SignedData<Belief> belief, AVector<State> states, AVector<BlockResult> results,
			long timeStamp) {
		this.keyPair = kp;
		this.peerKey = kp.getAccountKey();
		this.belief = belief;
		this.states = states;
		this.blockResults = results;
		this.timestamp = timeStamp;
	}

	@SuppressWarnings("unchecked")
	public static Peer fromData(AKeyPair keyPair,AMap<Keyword, ACell> peerData)  {
		SignedData<Belief> belief=(SignedData<Belief>) peerData.get(Keywords.BELIEF);
		AVector<BlockResult> results=(AVector<BlockResult>) peerData.get(Keywords.RESULTS);
		AVector<State> states=(AVector<State>) peerData.get(Keywords.STATES);
		long timestamp=belief.getValue().getTimestamp();
		return new Peer(keyPair,belief,states,results,timestamp);
	}

	public AMap<Keyword, ACell> toData() {
		return Maps.of(
			Keywords.BELIEF,belief,
			Keywords.RESULTS,blockResults,
			Keywords.STATES,states
		);
	}

	public static Peer create(AKeyPair peerKP, State initialState) {
		Belief belief = Belief.createSingleOrder(peerKP);
		SignedData<Belief> sb = peerKP.signData(belief);
		AVector<State> states=Vectors.of(initialState);

		// Ensure initial belief and states are persisted in current store
		ACell.createPersisted(sb);
		ACell.createPersisted(states);

		// Check belief persistence
		Ref<SignedData<Belief>> sbr=Ref.forHash(sb.getHash());
		if (sbr==null) {
			throw new Error("Belief not correctly persisted! "+sb.getHash());
		}

		return new Peer(peerKP, sb, states, Vectors.empty(), initialState.getTimeStamp().longValue());
	}
	
	/**
	 * Create a Peer instance from a remotely acquired Belief
	 * @param peerKP
	 * @param initialState Initial genesis State of the Network
	 * @param remoteBelief
	 * @return New Peer instance
	 */
	public static Peer create(AKeyPair peerKP, State initialState, Belief remoteBelief) {
		throw new TODOException();
	}

	/**
	 * Restores a Peer from the Etch database specified in Config
	 * @param store Store to restore from
	 * @param root Root Hash of Peer data
	 * @param keyPair Key Pair to use for restored Peer
	 * @return Peer instance, or null if root hash was not found
	 * @throws IOException If store reading failed
	 */
	public static Peer restorePeer(AStore store,Hash root, AKeyPair keyPair) throws IOException {
		// temporarily set current store
		AStore tempStore=Stores.current();
		try {
			Stores.setCurrent(store);
			Ref<ACell> ref=store.refForHash(root);
			if (ref==null) return null; // not found case
			@SuppressWarnings("unchecked")
			AMap<Keyword,ACell> peerData=(AMap<Keyword, ACell>) ref.getValue();

			Peer peer=Peer.fromData(keyPair,peerData);
			return peer;
		} finally {
			Stores.setCurrent(tempStore);
		}
	}

	/**
	 * Creates a new Peer instance at server startup using the provided
	 * configuration. Current store must be set to store for server.
	 *
	 * @param config
	 * @return A new Peer instance
	 */
	public static Peer createStartupPeer(Map<Keyword, Object> config) {
		State initialState = (State) config.get(Keywords.STATE);
		if (initialState == null) throw new IllegalArgumentException("Peer initialisation requires an initial state");
		AKeyPair keyPair = (AKeyPair) config.get(Keywords.KEYPAIR);
		if (keyPair == null) throw new IllegalArgumentException("Peer initialisation requires a keypair");

		return create(keyPair, initialState);
	}

	public MergeContext getMergeContext() {
		return MergeContext.create(keyPair, timestamp, getConsensusState());
	}

	/**
	 * Updates the timestamp to the specified time, going forwards only
	 *
	 * @param newTimestamp
	 * @return This peer upated with the given timestamp
	 */
	public Peer updateTimestamp(long newTimestamp) {
		if (newTimestamp < timestamp) return this;
		return new Peer(keyPair, belief, states, blockResults, timestamp);
	}

	/**
	 * Compiles and executes a query on the current consensus state of this Peer.
	 *
	 * @param <T> Type of result
	 * @param form Form to compile and execute.
	 * @param address Address to use for query execution
	 * @return The Context containing the query results. Will be NOBODY error if address / account does not exist
	 */
	@SuppressWarnings("unchecked")
	public <T extends ACell> Context<T> executeQuery(ACell form, Address address) {
		State state=getConsensusState();

		if (address==null) {
			return  Context.createFake(state).withError(ErrorCodes.NOBODY,"Null Address provided for query");
		}

		Context<?> ctx= Context.createFake(state, address);

		if (state.getAccount(address)==null) {
			return ctx.withError(ErrorCodes.NOBODY,"Account does not exist for query: "+address);
		}

		Context<AOp<T>> ectx = ctx.expandCompile(form);
		if (ectx.isExceptional()) {
			return (Context<T>) ectx;
		}

		AOp<T> op = ectx.getResult();
		Context<T> rctx = ctx.run(op);
		return rctx;
	}

	/**
	 * Estimates the coin cost of a executing a given transaction by performing a "dry run".
	 *
	 * This will be exact if no intermediate transactions affect the state, and if no time-dependent functionality is used.
	 *
	 * @param trans Transaction to test
	 * @return Estimated cost
	 */
	public long estimateCost(ATransaction trans) {
		Address address=trans.getAddress();
		State state=getConsensusState();
		Context<?> ctx=executeDryRun(trans);
		return state.getBalance(address)-ctx.getState().getBalance(address);
	}

	/**
	 * Executes a transaction on the current consensus state of this Peer.
	 *
	 * @param <T>
	 * @param transaction Transaction to execute
	 * @return The Context containing the transaction results.
	 */
	public <T extends ACell> Context<T> executeDryRun(ATransaction transaction) {
		Context<T> ctx=getConsensusState().applyTransaction(transaction);
		return ctx;
	}

	/**
	 * Executes a query in this Peer's current Consensus State, using a default address
	 * @param <T>
	 * @param form
	 * @return Context after executing query
	 */
	public <T extends ACell> Context<T> executeQuery(ACell form) {
		return executeQuery(form,Init.BASE_FIRST_ADDRESS);
	}

	public long getTimeStamp() {
		return timestamp;
	}

	/**
	 * Gets the Peer Key of this Peer.
	 * @return Address of Peer.
	 */
 	public AccountKey getPeerKey() {
		return peerKey;
	}

	public Belief getBelief() {
		return belief.getValue();
	}

	public SignedData<Belief> getSignedBelief() {
		return belief;
	}

	public <T extends ACell> SignedData<T> sign(T value) {
		return SignedData.create(keyPair, value);
	}

	/**
	 * Gets the current consensus state for this chain
	 *
	 * @return Consensus state for this chain (initial state if no block consensus)
	 */
	public State getConsensusState() {
		return states.get(states.count() - 1);
	}

	/**
	 * Merges a set of new Beliefs into this Peer's belief. Beliefs may be null, in
	 * which case they are ignored.
	 *
	 * @param beliefs An array of Beliefs. May contain nulls, which will be ignored.
	 * @return Updated Peer after Belief Merge
	 * @throws InvalidDataException
	 * @throws BadSignatureException
	 *
	 */
	public Peer mergeBeliefs(Belief... beliefs) throws BadSignatureException, InvalidDataException {
		Belief belief = getBelief();
		MergeContext mc = MergeContext.create(keyPair, timestamp, getConsensusState());
		Belief newBelief = belief.merge(mc, beliefs);

		long ocp=getConsensusPoint();
		Order newOrder=newBelief.getOrder(peerKey);
		if (ocp>newBelief.getOrder(peerKey).getConsensusPoint()) {
			// This probably shouldn't happen, but just in case.....
			System.err.println("Receding consensus? Old CP="+ocp +", New CP="+newOrder.getConsensusPoint());
			@SuppressWarnings("unused")
			Belief newBelief2 = belief.merge(mc, beliefs);

		}

		return updateConsensus(newBelief);
	}

	/**
	 * Update this Peer with Consensus State for an updated Belief
	 *
	 * @param newBelief
	 * @return
	 * @throws BadSignatureException
	 */
	private Peer updateConsensus(Belief newBelief) throws BadSignatureException {
		if (belief.getValue() == newBelief) return this;
		Order myOrder = newBelief.getOrder(peerKey); // this peer's chain from new belief
		long consensusPoint = myOrder.getConsensusPoint();
		long stateIndex = states.count() - 1; // index of last state
		AVector<Block> blocks = myOrder.getBlocks();

		// need to advance states
		AVector<State> newStates = this.states;
		AVector<BlockResult> newResults = this.blockResults;
		while (stateIndex < consensusPoint) { // add states until last state is at consensus point
			State s = newStates.get(stateIndex);
			Block block = blocks.get(stateIndex);
			BlockResult br = s.applyBlock(block);
			newStates = newStates.append(br.getState());
			newResults = newResults.append(br);
			stateIndex++;
		}
		SignedData<Belief> sb = keyPair.signData(newBelief);
		return new Peer(keyPair, sb, newStates, newResults, timestamp);
	}

	/**
	 * Persist the state of the Peer to the current store
	 * @param noveltyHandler
	 * @return Updates Peer
	 */
	public Peer persistState(Consumer<Ref<ACell>> noveltyHandler) {
		// Peer Belief must be announced using novelty handler
		SignedData<Belief> sb=this.belief;
		sb=ACell.createAnnounced(sb, noveltyHandler).getValue();

		// Persist states
		AVector<State> newStates = this.states;
		newStates=ACell.createPersisted(newStates).getValue();

		// Persist results
		AVector<BlockResult> newResults = this.blockResults;
		newResults=ACell.createPersisted(newResults).getValue();

		return new Peer(this.keyPair, sb, newStates, newResults, this.timestamp);
	}

	public AVector<State> getStates() {
		return states;
	}

	/**
	 * Gets the result of a specific transaction
	 * @param blockIndex Index of Block in Order
	 * @param txIndex Index of transaction in block
	 * @return Result from transaction
	 */
	public Result getResult(long blockIndex, long txIndex) {
		return blockResults.get(blockIndex).getResult(txIndex);
	}

	/**
	 * Gets the BlockResult of a specific block index
	 * @param i Index of Block
	 * @return BlockResult
	 */
	public BlockResult getBlockResult(long i) {
		return blockResults.get(i);
	}

	/**
	 * Propose a new Block. Adds the block to the current proposed chain for this
	 * Peer.
	 *
	 * @param block Block to publish
	 * @return Peer after proposing new Block in Peer's own Order
	 * @throws BadSignatureException
	 */
	public Peer proposeBlock(Block block) throws BadSignatureException {
		Belief b = getBelief();
		BlobMap<AccountKey, SignedData<Order>> orders = b.getOrders();

		Order myOrder = b.getOrder(peerKey);
		if (myOrder==null) myOrder=Order.create();

		Order newChain = myOrder.propose(block);
		SignedData<Order> newSignedChain = sign(newChain);
		BlobMap<AccountKey, SignedData<Order>> newChains = orders.assoc(peerKey, newSignedChain);
		return updateConsensus(b.withOrders(newChains));
	}

	public long getConsensusPoint() {
		Order order=getPeerOrder();
		if (order==null) return 0;
		return order.getConsensusPoint();
	}

	/**
	 * Gets the current Order for this Peer
	 *
	 * @return The Order for this peer in its current Belief. Will return null if the Peer is not a peer in the current consensus state
	 *
	 */
	public Order getPeerOrder() {
		try {
			return getBelief().getOrder(peerKey);
		} catch (BadSignatureException e) {
			throw new Error("Bad signature on own chain?", e);
		}
	}

	/**
	 * Gets the current chain this Peer sees for a given peer address
	 *
	 * @param peerKey Peer Key
	 * @return The current Order for the specified peer
	 * @throws BadSignatureException
	 */
	public Order getOrder(AccountKey peerKey) throws BadSignatureException {
		return getBelief().getOrder(peerKey);
	}

	/**
	 * Returns State as-of timestamp.
	 *
	 * Timestamp doesn't need to be an exact match; a leftmost State will be returned - unless timestamp is too old.
	 *
	 * @param timestamp Timestamp in milliseconds.
	 * @return State or null.
	 */
	public State asOf(CVMLong timestamp) {
		return Utils.stateAsOf(states, timestamp);
	}

	/**
	 * Construct a vector of States starting at specified timestamp, and with a given interval in milliseconds.
	 *
	 * @param timestamp Timestamp in milliseconds.
	 * @param interval Interval in milliseconds.
	 * @param count Number of times to query.
	 * @return Vector of States.
	 */
	public AVector<State> asOfRange(CVMLong timestamp, long interval, int count) {
		return Utils.statesAsOfRange(states, timestamp, interval, count);
	}

	public Hash getNetworkID() {
		return getStates().get(0).getHash();
	}
}

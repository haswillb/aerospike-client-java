/*
 * Copyright 2012-2015 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements WHICH ARE COMPATIBLE WITH THE APACHE LICENSE, VERSION 2.0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.aerospike.client.async;

import java.util.List;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Host;
import com.aerospike.client.Key;
import com.aerospike.client.BatchRead;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.command.Command;
import com.aerospike.client.listener.BatchSequenceListener;
import com.aerospike.client.listener.DeleteListener;
import com.aerospike.client.listener.ExecuteListener;
import com.aerospike.client.listener.ExistsArrayListener;
import com.aerospike.client.listener.ExistsListener;
import com.aerospike.client.listener.ExistsSequenceListener;
import com.aerospike.client.listener.BatchListListener;
import com.aerospike.client.listener.RecordArrayListener;
import com.aerospike.client.listener.RecordListener;
import com.aerospike.client.listener.RecordSequenceListener;
import com.aerospike.client.listener.WriteListener;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Statement;

/**
 * Asynchronous Aerospike client.
 * <p>
 * Your application uses this class to perform asynchronous database operations 
 * such as writing and reading records, and selecting sets of records. Write 
 * operations include specialized functionality such as append/prepend and arithmetic
 * addition.
 * <p>
 * This client is thread-safe. One client instance should be used per cluster.
 * Multiple threads should share this cluster instance.
 * <p>
 * Each record may have multiple bins, unless the Aerospike server nodes are
 * configured as "single-bin". In "multi-bin" mode, partial records may be
 * written or read by specifying the relevant subset of bins.
 */
public class AsyncClient extends AerospikeClient implements IAsyncClient {	
	//-------------------------------------------------------
	// Member variables.
	//-------------------------------------------------------
	
	private final AsyncCluster cluster;
	
	/**
	 * Default read policy that is used when asynchronous read command policy is null.
	 */
	public final Policy asyncReadPolicyDefault;
	
	/**
	 * Default write policy that is used when asynchronous write command policy is null.
	 */
	public final WritePolicy asyncWritePolicyDefault;
	
	/**
	 * Default scan policy that is used when asynchronous scan command policy is null.
	 */
	public final ScanPolicy asyncScanPolicyDefault;

	/**
	 * Default query policy that is used when asynchronous query command policy is null.
	 */
	public final QueryPolicy asyncQueryPolicyDefault;
	
	/**
	 * Default batch policy that is used when asynchronous batch command policy is null.
	 */
	public final BatchPolicy asyncBatchPolicyDefault;

	//-------------------------------------------------------
	// Constructors
	//-------------------------------------------------------

	/**
	 * Initialize asynchronous client.
	 * If the host connection succeeds, the client will:
	 * <p>
	 * - Add host to the cluster map <br>
	 * - Request host's list of other nodes in cluster <br>
	 * - Add these nodes to cluster map <br>
	 * <p>
	 * If the connection succeeds, the client is ready to process database requests.
	 * If the connection fails, the cluster will remain in a disconnected state
	 * until the server is activated.
	 * 
	 * @param hostname				host name
	 * @param port					host port
	 * @throws AerospikeException	if host connection fails
	 */
	public AsyncClient(String hostname, int port) throws AerospikeException {
		this(new AsyncClientPolicy(), new Host(hostname, port));
	}

	/**
	 * Initialize asynchronous client.
	 * The client policy is used to set defaults and size internal data structures.
	 * If the host connection succeeds, the client will:
	 * <p>
	 * - Add host to the cluster map <br>
	 * - Request host's list of other nodes in cluster <br>
	 * - Add these nodes to cluster map <br>
	 * <p>
	 * If the connection succeeds, the client is ready to process database requests.
	 * If the connection fails and the policy's failOnInvalidHosts is true, a connection 
	 * exception will be thrown. Otherwise, the cluster will remain in a disconnected state
	 * until the server is activated.
	 * 
	 * @param policy				client configuration parameters, pass in null for defaults
	 * @param hostname				host name
	 * @param port					host port
	 * @throws AerospikeException	if host connection fails
	 */
	public AsyncClient(AsyncClientPolicy policy, String hostname, int port) throws AerospikeException {
		this(policy, new Host(hostname, port));	
	}

	/**
	 * Initialize asynchronous client with suitable hosts to seed the cluster map.
	 * The client policy is used to set defaults and size internal data structures.
	 * For each host connection that succeeds, the client will:
	 * <p>
	 * - Add host to the cluster map <br>
	 * - Request host's list of other nodes in cluster <br>
	 * - Add these nodes to cluster map <br>
	 * <p>
	 * In most cases, only one host is necessary to seed the cluster. The remaining hosts 
	 * are added as future seeds in case of a complete network failure.
	 * <p>
	 * If one connection succeeds, the client is ready to process database requests.
	 * If all connections fail and the policy's failIfNotConnected is true, a connection 
	 * exception will be thrown. Otherwise, the cluster will remain in a disconnected state
	 * until the server is activated.
	 * 
	 * @param policy				client configuration parameters, pass in null for defaults
	 * @param hosts					array of potential hosts to seed the cluster
	 * @throws AerospikeException	if all host connections fail
	 */
	public AsyncClient(AsyncClientPolicy policy, Host... hosts) throws AerospikeException {
		super(policy);
		
		if (policy == null) {
			policy = new AsyncClientPolicy();
		}
		
		this.asyncReadPolicyDefault = policy.asyncReadPolicyDefault;
		this.asyncWritePolicyDefault = policy.asyncWritePolicyDefault;
		this.asyncScanPolicyDefault = policy.asyncScanPolicyDefault;
		this.asyncQueryPolicyDefault = policy.asyncQueryPolicyDefault;
		this.asyncBatchPolicyDefault = policy.asyncBatchPolicyDefault;
		
		this.cluster = new AsyncCluster(policy, hosts);
		super.cluster = this.cluster;
	}

	//-------------------------------------------------------
	// Default Policies
	//-------------------------------------------------------

	public final Policy getAsyncReadPolicyDefault() {
		return asyncReadPolicyDefault;
	}

	public final WritePolicy getAsyncWritePolicyDefault() {
		return asyncWritePolicyDefault;
	}

	public final ScanPolicy getAsyncScanPolicyDefault() {
		return asyncScanPolicyDefault;
	}

	public final QueryPolicy getAsyncQueryPolicyDefault() {
		return asyncQueryPolicyDefault;
	}

	public final BatchPolicy getAsyncBatchPolicyDefault() {
		return asyncBatchPolicyDefault;
	}

	//-------------------------------------------------------
	// Write Record Operations
	//-------------------------------------------------------
	
	/**
	 * Asynchronously write record bin(s). 
	 * This method schedules the put command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy specifies the transaction timeout, record expiration and how the transaction is
	 * handled when the record already exists.
	 * 
	 * @param policy				write configuration parameters, pass in null for defaults
	 * @param listener				where to send results, pass in null for fire and forget
	 * @param key					unique record identifier
	 * @param bins					array of bin name/value pairs
	 * @throws AerospikeException	if queue is full
	 */
	public final void put(WritePolicy policy, WriteListener listener, Key key, Bin... bins) throws AerospikeException {		
		if (policy == null) {
			policy = asyncWritePolicyDefault;
		}
		AsyncWrite command = new AsyncWrite(cluster, policy, listener, key, bins, Operation.Type.WRITE);
		command.execute();
	}

	//-------------------------------------------------------
	// String Operations
	//-------------------------------------------------------
		
	/**
	 * Asynchronously append bin string values to existing record bin values.
	 * This method schedules the append command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy specifies the transaction timeout, record expiration and how the transaction is
	 * handled when the record already exists.
	 * This call only works for string values. 
	 * 
	 * @param policy				write configuration parameters, pass in null for defaults
	 * @param listener				where to send results, pass in null for fire and forget
	 * @param key					unique record identifier
	 * @param bins					array of bin name/value pairs
	 * @throws AerospikeException	if queue is full
	 */
	public final void append(WritePolicy policy, WriteListener listener, Key key, Bin... bins) throws AerospikeException {
		if (policy == null) {
			policy = asyncWritePolicyDefault;
		}
		AsyncWrite command = new AsyncWrite(cluster, policy, listener, key, bins, Operation.Type.APPEND);
		command.execute();
	}
	
	/**
	 * Asynchronously prepend bin string values to existing record bin values.
	 * This method schedules the prepend command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy specifies the transaction timeout, record expiration and how the transaction is
	 * handled when the record already exists.
	 * This call works only for string values. 
	 * 
	 * @param policy				write configuration parameters, pass in null for defaults
	 * @param listener				where to send results, pass in null for fire and forget
	 * @param key					unique record identifier
	 * @param bins					array of bin name/value pairs
	 * @throws AerospikeException	if queue is full
	 */
	public final void prepend(WritePolicy policy, WriteListener listener, Key key, Bin... bins) throws AerospikeException {
		if (policy == null) {
			policy = asyncWritePolicyDefault;
		}
		AsyncWrite command = new AsyncWrite(cluster, policy, listener, key, bins, Operation.Type.PREPEND);
		command.execute();
	}

	//-------------------------------------------------------
	// Arithmetic Operations
	//-------------------------------------------------------
	
	/**
	 * Asynchronously add integer bin values to existing record bin values.
	 * This method schedules the add command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy specifies the transaction timeout, record expiration and how the transaction is
	 * handled when the record already exists.
	 * This call only works for integer values. 
	 * 
	 * @param policy				write configuration parameters, pass in null for defaults
	 * @param listener				where to send results, pass in null for fire and forget
	 * @param key					unique record identifier
	 * @param bins					array of bin name/value pairs
	 * @throws AerospikeException	if queue is full
	 */
	public final void add(WritePolicy policy, WriteListener listener, Key key, Bin... bins) throws AerospikeException {
		if (policy == null) {
			policy = asyncWritePolicyDefault;
		}
		AsyncWrite command = new AsyncWrite(cluster, policy, listener, key, bins, Operation.Type.ADD);
		command.execute();
	}

	//-------------------------------------------------------
	// Delete Operations
	//-------------------------------------------------------
	
	/**
	 * Asynchronously delete record for specified key.
	 * This method schedules the delete command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy specifies the transaction timeout.
	 * 
	 * @param policy				delete configuration parameters, pass in null for defaults
	 * @param listener				where to send results, pass in null for fire and forget
	 * @param key					unique record identifier
	 * @throws AerospikeException	if queue is full
	 */
	public final void delete(WritePolicy policy, DeleteListener listener, Key key) throws AerospikeException {
		if (policy == null) {
			policy = asyncWritePolicyDefault;
		}
		AsyncDelete command = new AsyncDelete(cluster, policy, listener, key);
		command.execute();
	}

	//-------------------------------------------------------
	// Touch Operations
	//-------------------------------------------------------

	/**
	 * Asynchronously create record if it does not already exist.  If the record exists, the record's 
	 * time to expiration will be reset to the policy's expiration.
	 * <p>
	 * This method schedules the touch command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * 
	 * @param policy				write configuration parameters, pass in null for defaults
	 * @param listener				where to send results, pass in null for fire and forget
	 * @param key					unique record identifier
	 * @throws AerospikeException	if queue is full
	 */
	public final void touch(WritePolicy policy, WriteListener listener, Key key) throws AerospikeException {		
		if (policy == null) {
			policy = asyncWritePolicyDefault;
		}
		AsyncTouch command = new AsyncTouch(cluster, policy, listener, key);
		command.execute();
	}

	//-------------------------------------------------------
	// Existence-Check Operations
	//-------------------------------------------------------
	
	/**
	 * Asynchronously determine if a record key exists.
	 * This method schedules the exists command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy can be used to specify timeouts.
	 * 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param key					unique record identifier
	 * @throws AerospikeException	if queue is full
	 */
	public final void exists(Policy policy, ExistsListener listener, Key key) throws AerospikeException {
		if (policy == null) {
			policy = asyncReadPolicyDefault;
		}
		AsyncExists command = new AsyncExists(cluster, policy, listener, key);
		command.execute();
	}

	/**
	 * Asynchronously check if multiple record keys exist in one batch call.
	 * This method schedules the exists command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * <p>
	 * The policy can be used to specify timeouts.
	 *  
	 * @deprecated Use {@link #exists(BatchPolicy policy, ExistsArrayListener listener, Key[] keys)} instead. 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	@Deprecated
	public final void exists(Policy policy, ExistsArrayListener listener, Key[] keys) throws AerospikeException {
		BatchPolicy batchPolicy = getAsyncBatchPolicy(policy);
		exists(batchPolicy, listener, keys);
	}

	/**
	 * Asynchronously check if multiple record keys exist in one batch call.
	 * This method schedules the exists command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * <p>
	 * The policy can be used to specify timeouts and maximum parallel commands.
	 *  
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	public final void exists(BatchPolicy policy, ExistsArrayListener listener, Key[] keys) throws AerospikeException {
		if (keys.length == 0) {
			listener.onSuccess(keys, new boolean[0]);
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.ExistsArrayExecutor(cluster, policy, keys, listener);		
	}

	/**
	 * Asynchronously check if multiple record keys exist in one batch call.
	 * This method schedules the exists command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in multiple unordered calls.
	 * <p>
	 * The policy can be used to specify timeouts.
	 *  
	 * @deprecated Use {@link #exists(BatchPolicy policy, ExistsSequenceListener listener, Key[] keys)} instead. 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	@Deprecated
	public final void exists(Policy policy, ExistsSequenceListener listener, Key[] keys) throws AerospikeException {
		BatchPolicy batchPolicy = getAsyncBatchPolicy(policy);
		exists(batchPolicy, listener, keys);
	}

	/**
	 * Asynchronously check if multiple record keys exist in one batch call.
	 * This method schedules the exists command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in multiple unordered calls.
	 * <p>
	 * The policy can be used to specify timeouts and maximum parallel commands.
	 *  
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	public final void exists(BatchPolicy policy, ExistsSequenceListener listener, Key[] keys) throws AerospikeException {
		if (keys.length == 0) {
			listener.onSuccess();
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.ExistsSequenceExecutor(cluster, policy, keys, listener);		
	}

	//-------------------------------------------------------
	// Read Record Operations
	//-------------------------------------------------------
	
	/**
	 * Asynchronously read entire record for specified key.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy can be used to specify timeouts.
	 * 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param key					unique record identifier
	 * @throws AerospikeException	if queue is full
	 */	
	public final void get(Policy policy, RecordListener listener, Key key) throws AerospikeException {
		if (policy == null) {
			policy = asyncReadPolicyDefault;
		}
		AsyncRead command = new AsyncRead(cluster, policy, listener, key, null);
		command.execute();
	}
	
	/**
	 * Asynchronously read record header and bins for specified key.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy can be used to specify timeouts.
	 * 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param key					unique record identifier
	 * @param binNames				bins to retrieve
	 * @throws AerospikeException	if queue is full
	 */
	public final void get(Policy policy, RecordListener listener, Key key, String... binNames) throws AerospikeException {	
		if (policy == null) {
			policy = asyncReadPolicyDefault;
		}
		AsyncRead command = new AsyncRead(cluster, policy, listener, key, binNames);
		command.execute();
	}

	/**
	 * Asynchronously read record generation and expiration only for specified key.  Bins are not read.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy can be used to specify timeouts.
	 * 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param key					unique record identifier
	 * @throws AerospikeException	if queue is full
	 */
	public final void getHeader(Policy policy, RecordListener listener, Key key) throws AerospikeException {
		if (policy == null) {
			policy = asyncReadPolicyDefault;
		}
		AsyncReadHeader command = new AsyncReadHeader(cluster, policy, listener, key);
		command.execute();
	}

	//-------------------------------------------------------
	// Batch Read Operations
	//-------------------------------------------------------

	/**
	 * Asynchronously read multiple records for specified batch keys in one batch call.
	 * This method allows different namespaces/bins to be requested for each key in the batch.
	 * The returned records are located in the same list.
	 * If the BatchRead key field is not found, the corresponding record field will be null.
	 * <p>
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * This method requires Aerospike Server version >= 3.6.0.
	 * 
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param records				list of unique record identifiers and the bins to retrieve.
	 * @throws AerospikeException	if read fails
	 */
	public final void get(BatchPolicy policy, BatchListListener listener, List<BatchRead> records) throws AerospikeException {
		if (records.size() == 0) {
			listener.onSuccess(records);
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.ReadListExecutor(cluster, policy, listener, records);
	}

	/**
	 * Asynchronously read multiple records for specified batch keys in one batch call.
	 * This method allows different namespaces/bins to be requested for each key in the batch.
	 * The returned records are located in the same list.
	 * If the BatchRead key field is not found, the corresponding record field will be null.
	 * <p>
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * This method requires Aerospike Server version >= 3.6.0.
	 * 
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param records				list of unique record identifiers and the bins to retrieve.
	 * @throws AerospikeException	if read fails
	 */
	public final void get(BatchPolicy policy, BatchSequenceListener listener, List<BatchRead> records) throws AerospikeException {
		if (records.size() == 0) {
			listener.onSuccess();
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.ReadSequenceExecutor(cluster, policy, listener, records);
	}

	/**
	 * Asynchronously read multiple records for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts.
	 * 
	 * @deprecated Use {@link #get(BatchPolicy policy, RecordArrayListener listener, Key[] keys)} instead. 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	@Deprecated
	public final void get(Policy policy, RecordArrayListener listener, Key[] keys) throws AerospikeException {
		BatchPolicy batchPolicy = getAsyncBatchPolicy(policy);
		get(batchPolicy, listener, keys);
	}

	/**
	 * Asynchronously read multiple records for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts and maximum parallel commands.
	 * 
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	public final void get(BatchPolicy policy, RecordArrayListener listener, Key[] keys) throws AerospikeException {
		if (keys.length == 0) {
			listener.onSuccess(keys, new Record[0]);
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.GetArrayExecutor(cluster, policy, listener, keys, null, Command.INFO1_READ | Command.INFO1_GET_ALL);
	}

	/**
	 * Asynchronously read multiple records for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in multiple unordered calls.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts.
	 * 
	 * @deprecated Use {@link #get(BatchPolicy policy, RecordSequenceListener listener, Key[] keys)} instead. 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	@Deprecated
	public final void get(Policy policy, RecordSequenceListener listener, Key[] keys) throws AerospikeException {
		BatchPolicy batchPolicy = getAsyncBatchPolicy(policy);
		get(batchPolicy, listener, keys);
	}
	
	/**
	 * Asynchronously read multiple records for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in multiple unordered calls.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts and maximum parallel commands.
	 * 
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	public final void get(BatchPolicy policy, RecordSequenceListener listener, Key[] keys) throws AerospikeException {
		if (keys.length == 0) {
			listener.onSuccess();
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.GetSequenceExecutor(cluster, policy, listener, keys, null, Command.INFO1_READ | Command.INFO1_GET_ALL);		
	}

	/**
	 * Asynchronously read multiple record headers and bins for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts.
	 * 
	 * @deprecated Use {@link #get(BatchPolicy policy, RecordArrayListener listener, Key[] keys, String... binNames)} instead. 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @param binNames				array of bins to retrieve
	 * @throws AerospikeException	if queue is full
	 */
	@Deprecated
	public final void get(Policy policy, RecordArrayListener listener, Key[] keys, String... binNames) 
		throws AerospikeException {
		BatchPolicy batchPolicy = getAsyncBatchPolicy(policy);
		get(batchPolicy, listener, keys, binNames);
	}

	/**
	 * Asynchronously read multiple record headers and bins for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts and maximum parallel commands.
	 * 
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @param binNames				array of bins to retrieve
	 * @throws AerospikeException	if queue is full
	 */
	public final void get(BatchPolicy policy, RecordArrayListener listener, Key[] keys, String... binNames) 
		throws AerospikeException {
		if (keys.length == 0) {
			listener.onSuccess(keys, new Record[0]);
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.GetArrayExecutor(cluster, policy, listener, keys, binNames, Command.INFO1_READ);
	}

	/**
	 * Asynchronously read multiple record headers and bins for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in multiple unordered calls.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts.
	 * 
	 * @deprecated Use {@link #get(BatchPolicy policy, RecordSequenceListener listener, Key[] keys, String... binNames)} instead. 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @param binNames				array of bins to retrieve
	 * @throws AerospikeException	if queue is full
	 */
	@Deprecated
	public final void get(Policy policy, RecordSequenceListener listener, Key[] keys, String... binNames) 
		throws AerospikeException {
		BatchPolicy batchPolicy = getAsyncBatchPolicy(policy);
		get(batchPolicy, listener, keys, binNames);
	}
	
	/**
	 * Asynchronously read multiple record headers and bins for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in multiple unordered calls.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts and maximum parallel commands.
	 * 
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @param binNames				array of bins to retrieve
	 * @throws AerospikeException	if queue is full
	 */
	public final void get(BatchPolicy policy, RecordSequenceListener listener, Key[] keys, String... binNames) 
		throws AerospikeException {
		if (keys.length == 0) {
			listener.onSuccess();
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.GetSequenceExecutor(cluster, policy, listener, keys, binNames, Command.INFO1_READ);
	}

	/**
	 * Asynchronously read multiple record header data for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts.
	 * 
	 * @deprecated Use {@link #getHeader(BatchPolicy policy, RecordArrayListener listener, Key[] keys)} instead. 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	@Deprecated
	public final void getHeader(Policy policy, RecordArrayListener listener, Key[] keys) throws AerospikeException {
		BatchPolicy batchPolicy = getAsyncBatchPolicy(policy);
		getHeader(batchPolicy, listener, keys);
	}

	/**
	 * Asynchronously read multiple record header data for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in a single call.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts and maximum parallel commands.
	 * 
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	public final void getHeader(BatchPolicy policy, RecordArrayListener listener, Key[] keys) throws AerospikeException {
		if (keys.length == 0) {
			listener.onSuccess(keys, new Record[0]);
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.GetArrayExecutor(cluster, policy, listener, keys, null, Command.INFO1_READ | Command.INFO1_NOBINDATA);
	}

	/**
	 * Asynchronously read multiple record header data for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in multiple unordered calls.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts.
	 * 
	 * @deprecated Use {@link #getHeader(BatchPolicy policy, RecordSequenceListener listener, Key[] keys)} instead. 
	 * @param policy				generic configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	@Deprecated
	public final void getHeader(Policy policy, RecordSequenceListener listener, Key[] keys) throws AerospikeException {
		BatchPolicy batchPolicy = getAsyncBatchPolicy(policy);
		getHeader(batchPolicy, listener, keys);
	}

	/**
	 * Asynchronously read multiple record header data for specified keys in one batch call.
	 * This method schedules the get command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener in multiple unordered calls.
	 * <p>
	 * If a key is not found, the record will be null.
	 * The policy can be used to specify timeouts and maximum parallel commands.
	 * 
	 * @param policy				batch configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param keys					array of unique record identifiers
	 * @throws AerospikeException	if queue is full
	 */
	public final void getHeader(BatchPolicy policy, RecordSequenceListener listener, Key[] keys) throws AerospikeException {
		if (keys.length == 0) {
			listener.onSuccess();
			return;
		}
		if (policy == null) {
			policy = asyncBatchPolicyDefault;
		}
		new AsyncBatch.GetSequenceExecutor(cluster, policy, listener, keys, null, Command.INFO1_READ | Command.INFO1_NOBINDATA);
	}

	//-------------------------------------------------------
	// Generic Database Operations
	//-------------------------------------------------------
	
	/**
	 * Asynchronously perform multiple read/write operations on a single key in one batch call.
	 * An example would be to add an integer value to an existing record and then
	 * read the result, all in one database call.
	 * <p>
	 * This method schedules the operate command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * Both scalar bin operations (Operation) and list bin operations (ListOperation)
	 * can be performed in same call.
	 * 
	 * @param policy				write configuration parameters, pass in null for defaults
	 * @param listener				where to send results, pass in null for fire and forget
	 * @param key					unique record identifier
	 * @param operations			database operations to perform
	 * @throws AerospikeException	if queue is full
	 */
	public final void operate(WritePolicy policy, RecordListener listener, Key key, Operation... operations) 
		throws AerospikeException {		
		if (policy == null) {
			policy = asyncWritePolicyDefault;
		}
		AsyncOperate command = new AsyncOperate(cluster, policy, listener, key, operations);
		command.execute();
	}

	//-------------------------------------------------------
	// Scan Operations
	//-------------------------------------------------------

	/**
	 * Asynchronously read all records in specified namespace and set.  If the policy's 
	 * <code>concurrentNodes</code> is specified, each server node will be read in
	 * parallel.  Otherwise, server nodes are read in series.
	 * <p>
	 * This method schedules the scan command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * 
	 * @param policy				scan configuration parameters, pass in null for defaults
	 * @param listener				where to send results, pass in null for fire and forget
	 * @param namespace				namespace - equivalent to database name
	 * @param setName				optional set name - equivalent to database table
	 * @param binNames				optional bin to retrieve. All bins will be returned if not specified.
	 * 								Aerospike 2 servers ignore this parameter.
	 * @throws AerospikeException	if queue is full
	 */
	public final void scanAll(ScanPolicy policy, RecordSequenceListener listener, String namespace, String setName, String... binNames)
		throws AerospikeException {
		if (policy == null) {
			policy = asyncScanPolicyDefault;
		}	
		new AsyncScanExecutor(cluster, policy, listener, namespace, setName, binNames);
	}
	
	//---------------------------------------------------------------
	// User defined functions
	//---------------------------------------------------------------

	/**
	 * Asynchronously execute user defined function on server and return results.
	 * The function operates on a single record.
	 * The package name is used to locate the udf file location on the server:
	 * <p>
	 * udf file = <server udf dir>/<package name>.lua
	 * <p>
	 * This method schedules the execute command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * 
	 * @param policy				write configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param key					unique record identifier
	 * @param packageName			server package name where user defined function resides
	 * @param functionName			user defined function
	 * @param functionArgs			arguments passed in to user defined function
	 * @throws AerospikeException	if transaction fails
	 */
	public final void execute(
		WritePolicy policy,
		ExecuteListener listener,
		Key key,
		String packageName,
		String functionName,
		Value... functionArgs
	) throws AerospikeException {
		if (policy == null) {
			policy = asyncWritePolicyDefault;
		}	
		AsyncExecute command = new AsyncExecute(cluster, policy, listener, key, packageName, functionName, functionArgs);
		command.execute();
	}

	//-------------------------------------------------------
	// Query Operations
	//-------------------------------------------------------

	/**
	 * Asynchronously execute query on all server nodes.  The query policy's 
	 * <code>maxConcurrentNodes</code> dictate how many nodes can be queried in parallel.
	 * The default is to query all nodes in parallel.
	 * <p>
	 * This method schedules the node's query commands with channel selectors and returns.
	 * Selector threads will process the commands and send the results to the listener.
	 * 
	 * @param policy				query configuration parameters, pass in null for defaults
	 * @param listener				where to send results
	 * @param statement				database query command parameters
	 * @throws AerospikeException	if query fails
	 */
	public final void query(QueryPolicy policy, RecordSequenceListener listener, Statement statement) 
		throws AerospikeException {
		if (policy == null) {
			policy = asyncQueryPolicyDefault;
		}	
		new AsyncQueryExecutor(cluster, policy, listener, statement);
	}
	
	//-------------------------------------------------------
	// Internal Methods
	//-------------------------------------------------------
	
	private final BatchPolicy getAsyncBatchPolicy(Policy policy) {		
		if (policy == null) {
			return asyncBatchPolicyDefault;
		}
		else {
			BatchPolicy batchPolicy = new BatchPolicy(policy);
			batchPolicy.sleepBetweenRetries = 0;
			batchPolicy.maxConcurrentThreads = 0;
			return batchPolicy;
		}
	}
}

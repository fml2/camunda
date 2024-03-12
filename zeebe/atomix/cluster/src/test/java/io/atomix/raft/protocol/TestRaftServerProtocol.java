/*
 * Copyright 2017-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.raft.protocol;

import com.google.common.collect.Sets;
import io.atomix.cluster.MemberId;
import java.net.ConnectException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/** Test server protocol. */
public class TestRaftServerProtocol implements RaftServerProtocol {

  private static final long REQUEST_TIMEOUT_MS = 1000;

  private Function<ConfigureRequest, CompletableFuture<ConfigureResponse>> configureHandler;
  private Function<ReconfigureRequest, CompletableFuture<ReconfigureResponse>> reconfigureHandler;
  private Function<ForceConfigureRequest, CompletableFuture<ForceConfigureResponse>>
      forceConfigureHandler;
  private Function<JoinRequest, CompletableFuture<JoinResponse>> joinHandler;
  private Function<LeaveRequest, CompletableFuture<LeaveResponse>> leaveHandler;
  private Function<InstallRequest, CompletableFuture<InstallResponse>> installHandler;
  private Function<TransferRequest, CompletableFuture<TransferResponse>> transferHandler;
  private Function<PollRequest, CompletableFuture<PollResponse>> pollHandler;
  private Function<VoteRequest, CompletableFuture<VoteResponse>> voteHandler;
  private Function<VersionedAppendRequest, CompletableFuture<AppendResponse>> appendHandler;
  private final Set<MemberId> partitions = Sets.newCopyOnWriteArraySet();
  private final Map<Class<?>, Consumer<?>> interceptors = new ConcurrentHashMap<>();
  private final Map<MemberId, TestRaftServerProtocol> servers;

  public TestRaftServerProtocol(
      final MemberId memberId, final Map<MemberId, TestRaftServerProtocol> servers) {
    this.servers = servers;
    servers.put(memberId, this);
  }

  public void disconnect(final MemberId target) {
    partitions.add(target);
  }

  public void reconnect(final MemberId target) {
    partitions.remove(target);
  }

  TestRaftServerProtocol server(final MemberId memberId) {
    if (partitions.contains(memberId)) {
      return null;
    }
    return servers.get(memberId);
  }

  @Override
  public CompletableFuture<ConfigureResponse> configure(
      final MemberId memberId, final ConfigureRequest request) {
    intercept(request, ConfigureRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.configure(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<ReconfigureResponse> reconfigure(
      final MemberId memberId, final ReconfigureRequest request) {
    intercept(request, ReconfigureRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.reconfigure(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<ForceConfigureResponse> forceConfigure(
      final MemberId memberId, final ForceConfigureRequest request) {
    intercept(request, ForceConfigureRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.forceConfigure(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<JoinResponse> join(final MemberId memberId, final JoinRequest request) {
    intercept(request, JoinRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.join(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<LeaveResponse> leave(
      final MemberId memberId, final LeaveRequest request) {
    intercept(request, LeaveRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.leave(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<InstallResponse> install(
      final MemberId memberId, final InstallRequest request) {
    intercept(request, InstallRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.install(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<TransferResponse> transfer(
      final MemberId memberId, final TransferRequest request) {
    intercept(request, TransferRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.transfer(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<PollResponse> poll(final MemberId memberId, final PollRequest request) {
    intercept(request, PollRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.poll(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<VoteResponse> vote(final MemberId memberId, final VoteRequest request) {
    intercept(request, VoteRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.vote(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public CompletableFuture<AppendResponse> append(
      final MemberId memberId, final AppendRequest request) {
    throw new IllegalArgumentException("Using old version not supported in tests");
  }

  @Override
  public CompletableFuture<AppendResponse> append(
      final MemberId memberId, final VersionedAppendRequest request) {
    intercept(request, VersionedAppendRequest.class);
    return getServer(memberId)
        .thenCompose(listener -> listener.append(request))
        .orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public void registerTransferHandler(
      final Function<TransferRequest, CompletableFuture<TransferResponse>> handler) {
    transferHandler = handler;
  }

  @Override
  public void unregisterTransferHandler() {
    transferHandler = null;
  }

  @Override
  public void registerConfigureHandler(
      final Function<ConfigureRequest, CompletableFuture<ConfigureResponse>> handler) {
    configureHandler = handler;
  }

  @Override
  public void unregisterConfigureHandler() {
    configureHandler = null;
  }

  @Override
  public void registerReconfigureHandler(
      final Function<ReconfigureRequest, CompletableFuture<ReconfigureResponse>> handler) {
    reconfigureHandler = handler;
  }

  @Override
  public void unregisterReconfigureHandler() {
    reconfigureHandler = null;
  }

  @Override
  public void registerForceConfigureHandler(
      final Function<ForceConfigureRequest, CompletableFuture<ForceConfigureResponse>> handler) {
    forceConfigureHandler = handler;
  }

  @Override
  public void unregisterForceConfigureHandler() {
    forceConfigureHandler = null;
  }

  @Override
  public void registerJoinHandler(
      final Function<JoinRequest, CompletableFuture<JoinResponse>> handler) {
    joinHandler = handler;
  }

  @Override
  public void unregisterJoinHandler() {
    joinHandler = null;
  }

  @Override
  public void registerLeaveHandler(
      final Function<LeaveRequest, CompletableFuture<LeaveResponse>> handler) {
    leaveHandler = handler;
  }

  @Override
  public void unregisterLeaveHandler() {
    leaveHandler = null;
  }

  @Override
  public void registerInstallHandler(
      final Function<InstallRequest, CompletableFuture<InstallResponse>> handler) {
    installHandler = handler;
  }

  @Override
  public void unregisterInstallHandler() {
    installHandler = null;
  }

  @Override
  public void registerPollHandler(
      final Function<PollRequest, CompletableFuture<PollResponse>> handler) {
    pollHandler = handler;
  }

  @Override
  public void unregisterPollHandler() {
    pollHandler = null;
  }

  @Override
  public void registerVoteHandler(
      final Function<VoteRequest, CompletableFuture<VoteResponse>> handler) {
    voteHandler = handler;
  }

  @Override
  public void unregisterVoteHandler() {
    voteHandler = null;
  }

  @Override
  public void registerAppendV1Handler(
      final Function<AppendRequest, CompletableFuture<AppendResponse>> handler) {
    // Ignore as old version is not supported in tests
  }

  @Override
  public void registerAppendV2Handler(
      final Function<VersionedAppendRequest, CompletableFuture<AppendResponse>> handler) {
    appendHandler = handler;
  }

  @Override
  public void unregisterAppendHandler() {
    appendHandler = null;
  }

  private CompletableFuture<TestRaftServerProtocol> getServer(final MemberId memberId) {
    final TestRaftServerProtocol server = server(memberId);
    if (server != null) {
      return CompletableFuture.completedFuture(server);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<AppendResponse> append(final VersionedAppendRequest request) {
    if (appendHandler != null) {
      return appendHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<VoteResponse> vote(final VoteRequest request) {
    if (voteHandler != null) {
      return voteHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<PollResponse> poll(final PollRequest request) {
    if (pollHandler != null) {
      intercept(request, PollRequest.class);
      return pollHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<TransferResponse> transfer(final TransferRequest request) {
    if (transferHandler != null) {
      return transferHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<InstallResponse> install(final InstallRequest request) {
    if (installHandler != null) {
      intercept(request, InstallRequest.class);
      return installHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<ReconfigureResponse> reconfigure(final ReconfigureRequest request) {
    if (reconfigureHandler != null) {
      return reconfigureHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<ForceConfigureResponse> forceConfigure(final ForceConfigureRequest request) {
    if (forceConfigureHandler != null) {
      return forceConfigureHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<JoinResponse> join(final JoinRequest request) {
    if (joinHandler != null) {
      return joinHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<LeaveResponse> leave(final LeaveRequest request) {
    if (leaveHandler != null) {
      return leaveHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  CompletableFuture<ConfigureResponse> configure(final ConfigureRequest request) {
    if (configureHandler != null) {
      return configureHandler.apply(request);
    } else {
      return CompletableFuture.failedFuture(new ConnectException());
    }
  }

  public <T> void interceptRequest(final Class<T> requestType, final Consumer<T> interceptor) {
    interceptors.put(requestType, interceptor);
  }

  @SuppressWarnings("unchecked")
  private <T> void intercept(final T request, final Class<T> requestType) {
    final var interceptor = (Consumer<T>) interceptors.get(requestType);
    if (interceptor != null) {
      interceptor.accept(request);
    }
  }
}

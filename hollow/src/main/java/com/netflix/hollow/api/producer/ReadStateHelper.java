/*
 *
 *  Copyright 2017 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.hollow.api.producer;

import static com.netflix.hollow.api.consumer.HollowConsumer.newReadState;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;

/**
 * Beta API subject to change.
 *
 * Helper for {@link HollowProducer} to manage current and pending read states and the
 * transition that occurs within a cycle.
 *
 * @author Tim Taylor {@literal<tim@toolbear.io>}
 *
 */
final class ReadStateHelper {
    static ReadStateHelper newDeltaChain() {
        return new ReadStateHelper(null, null);
    }

    static ReadStateHelper restored(HollowConsumer.ReadState state) {
        return new ReadStateHelper(state, null);
    }

    private final HollowConsumer.ReadState current;
    private final HollowConsumer.ReadState pending;

    private ReadStateHelper(HollowConsumer.ReadState current, HollowConsumer.ReadState pending) {
        this.current = current;
        this.pending = pending;
    }

    ReadStateHelper roundtrip(HollowProducer.WriteState writeState) {
        if(pending != null) throw new IllegalStateException();
        return new ReadStateHelper(this.current, newReadState(writeState.getVersion(), new HollowReadStateEngine()));
    }

    /**
     * Swap underlying state engines between current and pending while keeping the versions consistent;
     * used after delta integrity checks have altered the underlying state engines.
     *
     * @return
     */
    ReadStateHelper swap() {
        return new ReadStateHelper(newReadState(current.getVersion(), pending.getStateEngine()),
                newReadState(pending.getVersion(), current.getStateEngine()));
    }

    ReadStateHelper commit() {
        if(pending == null) throw new IllegalStateException();
        return new ReadStateHelper(this.pending, null);
    }

    HollowConsumer.ReadState current() {
        return current;
    }

    boolean hasCurrent() {
        return current != null;
    }

    HollowConsumer.ReadState pending() {
        return pending;
    }

    long pendingVersion() {
        return pending != null ? pending.getVersion() : Long.MIN_VALUE;
    }
}
